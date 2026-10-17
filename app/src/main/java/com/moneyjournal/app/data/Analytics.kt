package com.moneyjournal.app.data

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object Analytics {

    data class Summary(
        val incomePaise: Long = 0,
        val expensePaise: Long = 0,
        val transferPaise: Long = 0,
        val txCount: Int = 0,
    ) {
        val netPaise: Long get() = incomePaise - expensePaise
        val savingsRate: Float
            get() = if (incomePaise <= 0L) 0f else (netPaise.toFloat() / incomePaise).coerceIn(-1f, 1f)
    }

    data class Slice(
        val categoryId: String?,
        val name: String,
        val emoji: String,
        val colorIndex: Int,
        val totalPaise: Long,
        val count: Int,
        val share: Float,
    )

    data class DayTotal(val day: Int, val date: LocalDate, val expensePaise: Long, val incomePaise: Long)

    fun inMonth(txns: List<Txn>, month: YearMonth): List<Txn> = txns.filter {
        val date = LocalDate.ofEpochDay(it.epochDay)
        date.year == month.year && date.monthValue == month.monthValue
    }

    fun summary(txns: List<Txn>): Summary {
        var income = 0L
        var expense = 0L
        var transfer = 0L
        txns.forEach { txn ->
            when (txn.type) {
                TxType.EXPENSE -> expense += txn.amountPaise
                TxType.INCOME -> income += txn.amountPaise
                TxType.TRANSFER -> transfer += txn.amountPaise
            }
        }
        return Summary(income, expense, transfer, txns.size)
    }

    fun categoryBreakdown(txns: List<Txn>, data: AppData): List<Slice> {
        val expenses = txns.filter { it.type == TxType.EXPENSE }
        val total = expenses.sumOf { it.amountPaise }
        if (total <= 0L) return emptyList()
        return expenses.groupBy { it.categoryId }
            .map { (categoryId, group) ->
                val category = data.category(categoryId)
                val sum = group.sumOf { it.amountPaise }
                Slice(
                    categoryId = categoryId,
                    name = category?.name ?: "Uncategorised",
                    emoji = category?.emoji ?: "\u2022",
                    colorIndex = category?.colorIndex ?: 7,
                    totalPaise = sum,
                    count = group.size,
                    share = sum.toFloat() / total,
                )
            }
            .sortedByDescending { it.totalPaise }
    }

    fun dailyTotals(txns: List<Txn>, month: YearMonth): List<DayTotal> {
        val days = month.lengthOfMonth()
        val expense = LongArray(days + 1)
        val income = LongArray(days + 1)
        txns.forEach { txn ->
            val date = LocalDate.ofEpochDay(txn.epochDay)
            if (date.year != month.year || date.monthValue != month.monthValue) return@forEach
            when (txn.type) {
                TxType.EXPENSE -> expense[date.dayOfMonth] += txn.amountPaise
                TxType.INCOME -> income[date.dayOfMonth] += txn.amountPaise
                TxType.TRANSFER -> Unit
            }
        }
        return (1..days).map {
            DayTotal(it, month.atDay(it), expense[it], income[it])
        }
    }

    /** Average spend per weekday across the supplied transactions (index 0 = Monday). */
    fun weekdayProfile(txns: List<Txn>): List<Long> {
        val totals = LongArray(7)
        val counts = IntArray(7)
        txns.filter { it.type == TxType.EXPENSE }.forEach { txn ->
            val idx = LocalDate.ofEpochDay(txn.epochDay).dayOfWeek.value - 1
            totals[idx] += txn.amountPaise
            counts[idx]++
        }
        return (0..6).map { if (counts[it] == 0) 0L else totals[it] / counts[it] }
    }

    fun busiestWeekday(txns: List<Txn>): Int? {
        val profile = weekdayProfile(txns)
        if (profile.all { it == 0L }) return null
        return profile.indices.maxByOrNull { profile[it] }
    }

    fun dailyAverage(txns: List<Txn>, month: YearMonth, untilToday: Boolean = true): Long {
        val expenses = txns.filter { it.type == TxType.EXPENSE }
        if (expenses.isEmpty()) return 0L
        val today = LocalDate.now()
        val isCurrentMonth = YearMonth.from(today) == month
        val days = when {
            untilToday && isCurrentMonth -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }.coerceAtLeast(1)
        return expenses.sumOf { it.amountPaise } / days
    }

    fun biggest(txns: List<Txn>): Txn? =
        txns.filter { it.type == TxType.EXPENSE }.maxByOrNull { it.amountPaise }

    fun balance(data: AppData, accountId: String): Long {
        val account = data.account(accountId) ?: return 0L
        var balance = account.openingPaise
        data.transactions.forEach { txn ->
            when (txn.type) {
                TxType.INCOME -> if (txn.accountId == accountId) balance += txn.amountPaise
                TxType.EXPENSE -> if (txn.accountId == accountId) balance -= txn.amountPaise
                TxType.TRANSFER -> {
                    if (txn.accountId == accountId) balance -= txn.amountPaise
                    if (txn.toAccountId == accountId) balance += txn.amountPaise
                }
            }
        }
        return balance
    }

    fun balances(data: AppData): Map<String, Long> =
        data.accounts.associate { it.id to balance(data, it.id) }

    fun netWorth(data: AppData): Long =
        data.accounts.filter { !it.archived }.sumOf { account ->
            if (account.kind == AccountKind.CARD) 0L else balance(data, account.id)
        }

    fun liability(data: AppData): Long =
        data.accounts.filter { it.kind == AccountKind.CARD }.sumOf { account ->
            abs(min(0L, balance(data, account.id)))
        }

    /**
     * Money health: savings rate (60), month-over-month trend (20) and spending
     * consistency (20). Deterministic so the UI ring animates to a stable value.
     */
    fun healthScore(txns: List<Txn>, month: YearMonth): Int {
        val current = summary(inMonth(txns, month))
        val previous = summary(inMonth(txns, month.minusMonths(1)))

        val rate = current.savingsRate.coerceAtLeast(0f)
        val savingsScore = (rate / 0.4f).coerceIn(0f, 1f) * 60f

        val trendScore = if (previous.expensePaise <= 0L) {
            if (current.expensePaise <= 0L) 12f else 8f
        } else {
            val change = (current.expensePaise - previous.expensePaise).toFloat() / previous.expensePaise
            when {
                change <= -0.15f -> 20f
                change <= 0f -> 14f + (change / -0.15f) * 6f
                change <= 0.2f -> 12f - (change / 0.2f) * 4f
                else -> 6f
            }
        }

        val daily = dailyTotalsIn(txns, month)
        val maxDay = daily.maxOfOrNull { it } ?: 0L
        val total = daily.sum()
        val dominance = if (total <= 0L) 0f else maxDay.toFloat() / total
        val spread = ((dominance - 0.15f) / 0.5f).coerceIn(0f, 1f)
        val activeDays = daily.count { it > 0L }
        val coverage = (activeDays / 12f).coerceIn(0f, 1f)
        val consistencyScore = 20f * (0.5f + 0.5f * coverage) * (1f - 0.5f * spread)

        return (savingsScore + trendScore + consistencyScore).toInt().coerceIn(0, 100)
    }

    fun healthLabel(score: Int): String = when {
        score >= 80 -> "Excellent"
        score >= 65 -> "Good"
        score >= 45 -> "Fair"
        score >= 25 -> "Needs care"
        else -> "At risk"
    }

    fun dailyTotalsIn(txns: List<Txn>, month: YearMonth): List<Long> {
        val days = month.lengthOfMonth()
        val totals = LongArray(days + 1)
        txns.forEach { txn ->
            if (txn.type != TxType.EXPENSE) return@forEach
            val date = LocalDate.ofEpochDay(txn.epochDay)
            if (date.year == month.year && date.monthValue == month.monthValue) {
                totals[date.dayOfMonth] += txn.amountPaise
            }
        }
        return (1..days).map { totals[it] }
    }

    /** Delta percentage vs the previous month (null when there is no baseline). */
    fun monthOverMonth(txns: List<Txn>, month: YearMonth): Float? {
        val current = summary(inMonth(txns, month)).expensePaise
        val previous = summary(inMonth(txns, month.minusMonths(1))).expensePaise
        if (previous <= 0L) return null
        return (current - previous).toFloat() / previous
    }

    fun highestDay(txns: List<Txn>, month: YearMonth): Pair<LocalDate, Long>? {
        val totals = dailyTotals(txns, month)
        val best = totals.maxByOrNull { it.expensePaise } ?: return null
        if (best.expensePaise <= 0L) return null
        return best.date to best.expensePaise
    }

    fun topMerchants(txns: List<Txn>, limit: Int = 3): List<Pair<String, Long>> =
        txns.filter { it.type == TxType.EXPENSE && it.merchant.isNotBlank() }
            .groupBy { it.merchant.lowercase() }
            .map { (key, group) -> (group.first().merchant) to group.sumOf { it.amountPaise } }
            .sortedByDescending { it.second }
            .take(limit)

    fun monthsWithData(txns: List<Txn>): List<YearMonth> =
        txns.map { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }.distinct().sortedDescending()

    fun streakOfRecordedDays(txns: List<Txn>): Int {
        if (txns.isEmpty()) return 0
        val days = txns.map { it.epochDay }.toHashSet()
        var streak = 0
        var cursor = LocalDate.now()
        if (!days.contains(cursor.toEpochDay())) cursor = cursor.minusDays(1)
        while (days.contains(cursor.toEpochDay())) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun clampScore(value: Int) = max(0, min(100, value))
}

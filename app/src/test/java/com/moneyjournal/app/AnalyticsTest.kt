package com.moneyjournal.app

import com.moneyjournal.app.data.Account
import com.moneyjournal.app.data.AccountKind
import com.moneyjournal.app.data.Analytics
import com.moneyjournal.app.data.AppData
import com.moneyjournal.app.data.Category
import com.moneyjournal.app.data.DemoData
import com.moneyjournal.app.data.PayMode
import com.moneyjournal.app.data.Txn
import com.moneyjournal.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AnalyticsTest {

    private val month = YearMonth.of(2026, 10)
    private val accounts = listOf(
        Account("a1", "Bank", AccountKind.BANK, 10_000_00L),
        Account("a2", "Cash", AccountKind.CASH, 1_000_00L),
        Account("a3", "Card", AccountKind.CARD, 0L),
    )
    private val categories = listOf(
        Category("food", "Food & Dining", "\uD83C\uDF5C", 1, TxType.EXPENSE),
        Category("rent", "Rent", "\uD83C\uDFE0", 4, TxType.EXPENSE),
        Category("salary", "Salary", "\uD83D\uDCBC", 0, TxType.INCOME),
    )

    private fun txn(
        id: String,
        type: TxType,
        rupees: Long,
        day: Int,
        category: String? = null,
        account: String = "a1",
        toAccount: String? = null,
        merchant: String = "Swiggy",
    ) = Txn(
        id = id,
        type = type,
        amountPaise = rupees * 100,
        merchant = merchant,
        categoryId = category,
        accountId = account,
        toAccountId = toAccount,
        mode = PayMode.UPI,
        epochDay = LocalDate.of(2026, 10, day).toEpochDay(),
        createdAt = 0L,
    )

    private val journal = AppData(
        accounts = accounts,
        categories = categories,
        transactions = listOf(
            txn("t1", TxType.EXPENSE, 1_000, 2, "food"),
            txn("t2", TxType.EXPENSE, 500, 2, "food"),
            txn("t3", TxType.EXPENSE, 20_000, 3, "rent"),
            txn("t4", TxType.INCOME, 80_000, 1, "salary"),
            txn("t5", TxType.TRANSFER, 5_000, 5, null, "a1", "a2", merchant = "Sweep"),
            txn("t6", TxType.EXPENSE, 300, 9, "food", account = "a3", merchant = "Amazon"),
        ),
    )

    @Test
    fun filtersTransactionsByMonth() {
        assertEquals(6, Analytics.inMonth(journal.transactions, month).size)
        assertEquals(0, Analytics.inMonth(journal.transactions, YearMonth.of(2026, 9)).size)
    }

    @Test
    fun summarisesIncomeExpenseAndTransfers() {
        val summary = Analytics.summary(Analytics.inMonth(journal.transactions, month))
        assertEquals(8_000_000L, summary.incomePaise)
        assertEquals(2_180_000L, summary.expensePaise)
        assertEquals(500_000L, summary.transferPaise)
        assertEquals(5_820_000L, summary.netPaise)
        assertEquals(0.7275f, summary.savingsRate, 0.001f)
        assertEquals(6, summary.txCount)
    }

    @Test
    fun breaksDownSpendingByCategoryWithShares() {
        val slices = Analytics.categoryBreakdown(Analytics.inMonth(journal.transactions, month), journal)
        assertEquals(2, slices.size)
        assertEquals("Rent", slices[0].name)
        assertEquals(2_000_000L, slices[0].totalPaise)
        assertEquals("Food & Dining", slices[1].name)
        assertEquals(180_000L, slices[1].totalPaise)
        assertEquals(3, slices[1].count)
        assertEquals(1f, slices.sumOf { it.share.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun buildsDailyTotalsForEveryDayOfTheMonth() {
        val days = Analytics.dailyTotals(Analytics.inMonth(journal.transactions, month), month)
        assertEquals(31, days.size)
        assertEquals(150_000L, days[1].expensePaise) // day 2
        assertEquals(2_000_000L, days[2].expensePaise) // day 3
        assertEquals(8_000_000L, days[0].incomePaise)
    }

    @Test
    fun averagesSpendPerWeekday() {
        val profile = Analytics.weekdayProfile(journal.transactions)
        assertEquals(7, profile.size)
        // 2 Oct 2026 is a Friday, 3 Oct a Saturday
        assertTrue(profile[4] > 0L)
        assertTrue(profile[5] > 0L)
        assertNotNull(Analytics.busiestWeekday(journal.transactions))
        // Fridays: ₹1,000 + ₹500 (2 Oct) and ₹300 (9 Oct) = ₹1,800 over 3 entries
        assertEquals(60_000L, profile[4])
    }

    @Test
    fun computesBalancesIncludingTransfers() {
        // opening 10,000 + salary 80,000 − food 1,500 − rent 20,000 − sweep 5,000 (all in paise)
        assertEquals(1_000_000L + 8_000_000L - 150_000L - 2_000_000L - 500_000L, Analytics.balance(journal, "a1"))
        assertEquals(100_000L + 500_000L, Analytics.balance(journal, "a2"))
        assertEquals(-30_000L, Analytics.balance(journal, "a3"))
    }

    @Test
    fun netWorthExcludesCardsAndReportsLiability() {
        val netWorth = Analytics.netWorth(journal)
        assertEquals(Analytics.balance(journal, "a1") + Analytics.balance(journal, "a2"), netWorth)
        assertEquals(30_000L, Analytics.liability(journal))
    }

    @Test
    fun healthScoreStaysInRangeAndImprovesWithSaving() {
        val good = Analytics.healthScore(journal.transactions, month)
        assertTrue(good in 0..100)
        assertTrue(good > 50)

        val overspent = journal.copy(
            transactions = journal.transactions.map {
                if (it.type == TxType.EXPENSE) it.copy(amountPaise = it.amountPaise * 20) else it
            }
        )
        val bad = Analytics.healthScore(overspent.transactions, month)
        assertTrue(bad < good)
    }

    @Test
    fun detectsMonthOverMonthChange() {
        val previous = AppData(
            accounts = accounts,
            categories = categories,
            transactions = listOf(txn("p1", TxType.EXPENSE, 1_000, 2, "food").copy(
                epochDay = LocalDate.of(2026, 9, 2).toEpochDay()
            )),
        )
        val delta = Analytics.monthOverMonth(previous.transactions + journal.transactions, month)
        assertNotNull(delta)
        assertTrue(delta!! > 0f)
    }

    @Test
    fun ranksTopMerchantsAndBiggestExpense() {
        val data = journal.copy(
            transactions = journal.transactions.map {
                when (it.id) {
                    "t3" -> it.copy(merchant = "Landlord")
                    "t4" -> it.copy(merchant = "Nexora Technologies")
                    else -> it
                }
            }
        )
        val merchants = Analytics.topMerchants(data.transactions)
        assertEquals("Landlord", merchants.first().first)
        assertEquals(2_000_000L, merchants.first().second)
        assertEquals(2_000_000L, Analytics.biggest(data.transactions)!!.amountPaise)
    }

    @Test
    fun seedDataProducesAHealthyCurrentMonth() {
        val seeded = DemoData.seed(LocalDate.of(2026, 10, 17))
        val current = Analytics.inMonth(seeded.transactions, month)
        assertTrue(current.size > 20)
        val summary = Analytics.summary(current)
        assertTrue(summary.incomePaise > 0)
        assertTrue(summary.expensePaise > 0)
        assertTrue(Analytics.healthScore(seeded.transactions, month) in 0..100)
        assertTrue(seeded.transactions.all { it.epochDay <= LocalDate.of(2026, 10, 17).toEpochDay() })
        assertEquals(DemoData.defaultCategories.size, seeded.categories.size)
        assertEquals(DemoData.defaultAccounts.size, seeded.accounts.size)
    }
}

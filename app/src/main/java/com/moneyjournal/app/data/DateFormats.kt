package com.moneyjournal.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateFormats {

    private val monthLong = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val monthShort = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

    fun monthLabel(month: YearMonth): String = month.format(monthLong)

    fun monthShortLabel(month: YearMonth): String = month.format(monthShort)

    fun fullDate(date: LocalDate): String = date.format(dayMonthYear)

    fun shortDate(date: LocalDate): String = date.format(dayMonth)

    fun weekdayShort(date: LocalDate): String = date.format(weekday)

    fun dayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String {
        val delta = date.toEpochDay() - today.toEpochDay()
        return when (delta) {
            0L -> "Today"
            -1L -> "Yesterday"
            in -6L..-2L -> "${weekdayShort(date)}, ${shortDate(date)}"
            else -> shortDate(date)
        }
    }

    fun weekdayLabels(): List<String> =
        listOf("M", "T", "W", "T", "F", "S", "S")

    fun weekdayNames(): List<String> =
        java.time.DayOfWeek.entries.map { it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }

    /** "Friday" so the UI can say "Fridays" without producing "Fris". */
    fun weekdayFullNames(): List<String> =
        java.time.DayOfWeek.entries.map { it.getDisplayName(TextStyle.FULL, Locale.ENGLISH) }

    fun monthOptions(around: YearMonth, before: Int = 18, after: Int = 0): List<YearMonth> =
        (-before..after).map { around.plusMonths(it.toLong()) }.reversed()
}

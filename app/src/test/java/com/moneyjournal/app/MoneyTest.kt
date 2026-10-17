package com.moneyjournal.app

import com.moneyjournal.app.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun groupsDigitsTheIndianWay() {
        assertEquals("5", Money.groupIndian(5))
        assertEquals("999", Money.groupIndian(999))
        assertEquals("1,000", Money.groupIndian(1000))
        assertEquals("12,345", Money.groupIndian(12345))
        assertEquals("1,23,456", Money.groupIndian(123456))
        assertEquals("12,34,567", Money.groupIndian(1234567))
        assertEquals("1,23,45,678", Money.groupIndian(12345678))
    }

    @Test
    fun formatsPaiseWithSymbolAndDecimalsOnlyWhenNeeded() {
        assertEquals("\u20B912,500", Money.formatPaise(1_250_000))
        assertEquals("\u20B912,500.50", Money.formatPaise(1_250_050))
        assertEquals("\u20B90.05", Money.formatPaise(5))
        assertEquals("\u20B90", Money.formatPaise(0))
        assertEquals("- \u20B91,000".replace(" ", ""), Money.formatPaise(-100_000))
        assertEquals("1,23,456.78", Money.formatPaise(12_345_678, withSymbol = false))
        assertEquals("\u20B91,000.00", Money.formatPaise(100_000, forceDecimals = true))
    }

    @Test
    fun compactsLargeAmounts() {
        assertEquals("\u20B9999", Money.compact(99_900))
        assertEquals("\u20B91.5K", Money.compact(150_000))
        assertEquals("\u20B92.5L", Money.compact(25_000_000))
        assertEquals("\u20B91.2Cr", Money.compact(1_200_000_000))
    }

    @Test
    fun parsesRealWorldAmounts() {
        assertEquals(123_456L, Money.parseToPaise("1,234.56"))
        assertEquals(123_456L, Money.parseToPaise("\u20B91,234.56"))
        assertEquals(500_000L, Money.parseToPaise("Rs. 5,000"))
        assertEquals(500_000L, Money.parseToPaise("INR 5000"))
        assertEquals(50L, Money.parseToPaise(".5"))
        assertEquals(5_000L, Money.parseToPaise("50"))
        assertEquals(999L, Money.parseToPaise("9.99"))
        assertEquals(-25_000L, Money.parseToPaise("-250"))
    }

    @Test
    fun rejectsNonMoneyInput() {
        assertNull(Money.parseToPaise(null))
        assertNull(Money.parseToPaise(""))
        assertNull(Money.parseToPaise("abc"))
        assertNull(Money.parseToPaise("1.234"))
        assertNull(Money.parseToPaise("1.2.3"))
        assertNull(Money.parseToPaise("."))
        assertNull(Money.parseToPaise("99999999999999999999"))
    }

    @Test
    fun roundTripsPaiseAndInput() {
        assertEquals("1234.56", Money.paiseToInput(123_456))
        assertEquals("1234", Money.paiseToInput(123_400))
        assertEquals(123_456L, Money.keypadToPaise(Money.paiseToInput(123_456)))
    }

    @Test
    fun splitsForLargeDisplay() {
        val (rupees, paise) = Money.split(12_345_678)
        assertEquals("1,23,456", rupees)
        assertEquals(".78", paise)
        assertEquals("", Money.split(100_000).second)
    }
}

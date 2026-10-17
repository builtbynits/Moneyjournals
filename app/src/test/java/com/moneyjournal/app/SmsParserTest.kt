package com.moneyjournal.app

import com.moneyjournal.app.data.PayMode
import com.moneyjournal.app.data.SmsParser
import com.moneyjournal.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SmsParserTest {

    private val today = LocalDate.of(2026, 10, 17)

    private fun day(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day).toEpochDay()

    @Test
    fun parsesUpiDebitWithVpaAndName() {
        val parsed = SmsParser.parse(
            "Rs.1,250.00 debited from A/c XX1234 on 12-10-26 to VPA swiggy@axl (Swiggy). UPI Ref 123456789012. -HDFC Bank",
            today,
        )!!
        assertEquals(TxType.EXPENSE, parsed.type)
        assertEquals(125_000L, parsed.amountPaise)
        assertEquals("Swiggy", parsed.merchant)
        assertEquals(PayMode.UPI, parsed.mode)
        assertEquals(day(2026, 10, 12), parsed.epochDay)
        assertEquals("1234", parsed.accountLast4)
        assertEquals("Food & Dining", parsed.categoryHint)
        assertEquals("Ref 123456789012", parsed.note)
    }

    @Test
    fun parsesSalaryCredit() {
        val parsed = SmsParser.parse(
            "Your A/c XX4821 is credited with Rs 85,000.00 on 01-10-26 from VPA nexora@okhdfcbank (Nexora Technologies). -HDFC Bank",
            today,
        )!!
        assertEquals(TxType.INCOME, parsed.type)
        assertEquals(8_500_000L, parsed.amountPaise)
        assertEquals("Nexora Technologies", parsed.merchant)
        assertEquals(PayMode.UPI, parsed.mode)
        assertEquals(day(2026, 10, 1), parsed.epochDay)
        assertEquals("4821", parsed.accountLast4)
    }

    @Test
    fun parsesCreditCardPurchase() {
        val parsed = SmsParser.parse(
            "INR 349.00 spent on your HDFC Bank Credit Card ending 1234 at AMAZON on 05-10-26.",
            today,
        )!!
        assertEquals(TxType.EXPENSE, parsed.type)
        assertEquals(34_900L, parsed.amountPaise)
        assertEquals("Amazon", parsed.merchant)
        assertEquals(PayMode.CREDIT_CARD, parsed.mode)
        assertEquals(day(2026, 10, 5), parsed.epochDay)
        assertEquals("1234", parsed.accountLast4)
        assertEquals("Shopping", parsed.categoryHint)
    }

    @Test
    fun parsesPersonToPersonTransferWithoutDate() {
        val parsed = SmsParser.parse(
            "Sent Rs.500 to rahul.sharma@okaxis (Rahul Sharma) from A/c XX4321. Ref 998877665544",
            today,
        )!!
        assertEquals(TxType.EXPENSE, parsed.type)
        assertEquals(50_000L, parsed.amountPaise)
        assertEquals("Rahul Sharma", parsed.merchant)
        assertEquals(PayMode.UPI, parsed.mode)
        assertNull(parsed.epochDay)
        assertEquals("4321", parsed.accountLast4)
    }

    @Test
    fun parsesAtmWithdrawal() {
        val parsed = SmsParser.parse(
            "You have withdrawn Rs.2,000 from ATM using HDFC Debit Card ending 5678",
            today,
        )!!
        assertEquals(TxType.EXPENSE, parsed.type)
        assertEquals(200_000L, parsed.amountPaise)
        assertEquals(PayMode.DEBIT_CARD, parsed.mode)
        assertEquals("5678", parsed.accountLast4)
        assertEquals("Cash Withdrawal", parsed.categoryHint)
    }

    @Test
    fun ignoresAvailableBalanceWhenFindingTheAmount() {
        val parsed = SmsParser.parse(
            "Rs 149.00 paid to zomato@ybl via UPI. Avl Bal INR 12,345.67",
            today,
        )!!
        assertEquals(14_900L, parsed.amountPaise)
        assertEquals("Zomato", parsed.merchant)
        assertEquals(PayMode.UPI, parsed.mode)
        assertEquals("Food & Dining", parsed.categoryHint)
    }

    @Test
    fun parsesBankAccountTransferWithNeft() {
        val parsed = SmsParser.parse(
            "HDFC Bank: Rs.3,499 debited from your Account No. XX9014 on 30-09-2026 towards NEFT transfer to Landlord. Ref 112233445566",
            today,
        )!!
        assertEquals(TxType.EXPENSE, parsed.type)
        assertEquals(349_900L, parsed.amountPaise)
        assertEquals("Landlord", parsed.merchant)
        assertEquals(PayMode.NET_BANKING, parsed.mode)
        assertEquals(day(2026, 9, 30), parsed.epochDay)
        assertEquals("9014", parsed.accountLast4)
    }

    @Test
    fun parsesSlashDatesAndGroceries() {
        val parsed = SmsParser.parse(
            "Rs.89 debited for UPI transaction to blinkit@ybl on 15/10/26. Avl Bal Rs.876",
            today,
        )!!
        assertEquals(8_900L, parsed.amountPaise)
        assertEquals("Blinkit", parsed.merchant)
        assertEquals(day(2026, 10, 15), parsed.epochDay)
        assertEquals("Groceries", parsed.categoryHint)
    }

    @Test
    fun parsesRefundAsIncome() {
        val parsed = SmsParser.parse(
            "Rs.1,899.00 credited to your HDFC Credit Card ending 9014 as refund from Amazon on 21-09-26.",
            today,
        )!!
        assertEquals(TxType.INCOME, parsed.type)
        assertEquals(189_900L, parsed.amountPaise)
        assertEquals("Refunds", parsed.categoryHint)
    }

    @Test
    fun ignoresUnrelatedClipboardText() {
        assertNull(SmsParser.parse("Hi, are we still meeting for lunch tomorrow?", today))
        assertNull(SmsParser.parse("", today))
        assertNull(SmsParser.parse("abc", today))
    }

    @Test
    fun ignoresDatesThatAreTooOldOrInTheFuture() {
        val parsed = SmsParser.parse("Rs.500 debited from A/c XX1234 on 12-10-19", today)!!
        assertNull(parsed.epochDay)
    }

    @Test
    fun neverPicksAnAmountFromASingleDigitNoise() {
        assertNull(SmsParser.parse("OTP 4 8 2 1 is your verification code", today))
    }

    @Test
    fun confidenceReflectsHowMuchWasFound() {
        val rich = SmsParser.parse(
            "Rs.1,250.00 debited from A/c XX1234 on 12-10-26 to VPA swiggy@axl (Swiggy). UPI Ref 123456789012",
            today,
        )!!
        val plain = SmsParser.parse("Rs.500 paid", today)!!
        assertTrue(rich.confidence > plain.confidence)
    }
}

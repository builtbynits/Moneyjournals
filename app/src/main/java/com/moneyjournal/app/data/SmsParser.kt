package com.moneyjournal.app.data

import java.time.LocalDate

data class ParsedEntry(
    val type: TxType,
    val amountPaise: Long,
    val merchant: String,
    val mode: PayMode,
    val epochDay: Long?,
    val accountLast4: String?,
    val categoryHint: String?,
    val note: String,
    val confidence: Float,
    val raw: String,
)

/**
 * Parses Indian bank / UPI / card SMS-style text into a draft entry.
 * Works fully on-device with the clipboard content only.
 */
object SmsParser {

    private val balanceClause = Regex(
        "(?i)\\b(?:avl|avail|available)\\s*(?:bal|balance)\\b[^.]*" +
            "|\\bclosing\\s*balance\\b[^.]*|\\bbalance\\s*(?:is|:)?\\s*(?:rs\\.?|inr|\u20B9)?\\s*[0-9][0-9,]*(?:\\.\\d{1,2})?"
    )

    private val amountAfterVerb = Regex(
        "(?i)\\b(debited|credited|spent|paid|received|withdrawn|withdrew|deposited|sent|refunded|" +
            "transferred|deducted)\\b\\s*(?:of|for|by|with|:)?\\s*(?:rs\\.?|inr|\u20B9)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
    )
    private val currencyAmount = Regex(
        "(?i)(?:rs\\.?|inr|\u20B9)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
    )
    private val bareAmount = Regex("\\b([0-9][0-9,]{1,12}(?:\\.[0-9]{1,2})?)\\b")
    private val accountMarkerBeforeAmount = Regex(
        "(?i)(?:a\\/c|acct|account|card|no\\.|x{2,}|\\*{2,})\\s*$"
    )

    private val debitWord = Regex(
        "(?i)\\b(debited|debit|spent|paid|payment|withdrawn|withdrew|sent|purchase[d]?|deducted|" +
            "trf\\s+to|transferred\\s+to|to\\s+vpa|dr)\\b"
    )
    private val creditWord = Regex(
        "(?i)\\b(credited|credit\\s+of|received|refund(?:ed)?|reversal|cashback|deposited|" +
            "salary|money\\s+received|cr\\s+by)\\b"
    )

    private val vpaWithName = Regex("(?i)(?:to|at|from)?\\s*vpa\\s+[\\w.\\-]+@[\\w.]+\\s*\\(([^)]{2,60})\\)")
    private val anyParenName = Regex("(?i)(?:to|at|from)?\\s*([\\w.\\-]+@[\\w.]+)\\s*\\(([^)]{2,60})\\)")
    private val verbToParty = Regex(
        "(?i)\\b(?:sent|paid|received|transferred|refunded|paid\\s+to|received\\s+from)\\s+(?:to|from)?\\s*" +
            "([A-Za-z0-9&'.\\- ]{3,40}?)(?=\\s+(?:on|via|using|through|ref|upi|from|to)\\b|[.,;]|$)"
    )
    private val toParty = Regex(
        "(?i)\\bto\\s+([A-Za-z0-9&'.\\- ]{3,40}?)(?=\\s+(?:on|via|using|through|ref|for)\\b|[.,;]|$)"
    )
    private val atParty = Regex(
        "(?i)\\bat\\s+([A-Za-z0-9&'.\\- ]{3,40}?)(?=\\s+(?:on|via|using|through|ref|for)\\b|[.,;]|$)"
    )
    private val fromParty = Regex(
        "(?i)\\bfrom\\s+([A-Za-z0-9&'.\\- ]{3,40}?)(?=\\s+(?:on|via|using|through|ref|for)\\b|[.,;]|$)"
    )
    private val infoField = Regex("(?i)\\binfo:?\\s*([A-Za-z0-9&'.\\- ]{3,40})")
    private val vpaHandle = Regex("(?i)(?:to|at|from)?\\s*([\\w.\\-]+)@([a-z][\\w.]*)")
    private val refField = Regex("(?i)\\b(?:ref(?:erence)?|utr|txn)\\s*(?:no\\.?|id|:)?\\s*([A-Za-z0-9]{6,22})")

    private val dateDmy = Regex("\\b(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})\\b")
    private val dateDMonY = Regex("(?i)\\b(\\d{1,2})[-/ ]([a-z]{3})[a-z]*[-/ ](\\d{2,4})\\b")
    private val last4 = Regex(
        "(?i)(?:a\\/c|ac|acct|account|card)\\s*(?:no\\.?|number|ending|end)?\\s*[:\\-]?\\s*(?:x{2,}|\\*{2,}|\\.{2,})?\\s*(\\d{4})\\b"
    )
    private val ending4 = Regex("(?i)ending\\s*(?:with)?\\s*(\\d{4})\\b")

    private val monthNames = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    private val categoryKeywords: List<Pair<String, List<String>>> = listOf(
        "Salary" to listOf("salary", "sal cr", "payroll", "stipend"),
        "Interest" to listOf("interest", "int.pd", "int pd"),
        "Refunds" to listOf("refund", "cashback", "reversal", "reversed"),
        "Food & Dining" to listOf("swiggy", "zomato", "restaurant", "cafe", "coffee", "dominos", "pizza", "bakery", "food", "dhaba", "tiffin", "chai", "eatery", "hotel ", "bar "),
        "Groceries" to listOf("bigbasket", "dmart", "d-mart", "grofers", "grocery", "kirana", "more retail", "reliance fresh", "supermarket", "instamart", "blinkit", "zepto"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "petrol", "fuel", "diesel", "irctc", "redbus", "fastag", "cab", "auto", "rapi do", "parking", "toll"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "decathlon", "lifestyle", "croma", "reliance digital", "tata cliq", "shopping"),
        "Bills & Utilities" to listOf("airtel", "jio", "vodafone", "vi ", "recharge", "electricity", "bescom", "mseb", "adani electricity", "broadband", "act fibernet", "gas", "water bill", "bill payment", "dth", "tata sky", "postpaid"),
        "Entertainment" to listOf("netflix", "prime video", "hotstar", "disney", "spotify", "bookmyshow", "pvr", "inox", "youtube", "gaming", "steam"),
        "Health" to listOf("apollo", "pharmacy", "medplus", "hospital", "clinic", "1mg", "pharmeasy", "practo", "diagnostic", "lab", "dentist", "doctor"),
        "Rent" to listOf("rent", "landlord", "maintenance", "society", "housing"),
        "Education" to listOf("school", "college", "tuition", "course", "udemy", "coursera", "byju", "unacademy", "exam fee"),
        "Cash Withdrawal" to listOf("atm", "withdraw", "cash wdl", "self wdl"),
        "Transfers" to listOf("neft", "imps", "rtgs", "self transfer", "own account"),
        "Investments" to listOf("sip", "mutual fund", "groww", "zerodha", "upstox", "smallcase", "ppf", "nps", "fd ", "rd ", "insurance", "lic", "gold bond"),
    )

    fun parse(input: String?, today: LocalDate = LocalDate.now()): ParsedEntry? {
        val raw = input?.trim().orEmpty()
        if (raw.length < 4) return null

        val text = raw
            .replace('\n', ' ')
            .replace('\u00A0', ' ')
            .replace('\u2013', '-')
            .replace('\u2014', '-')
            .replace(Regex("\\s+"), " ")
            .trim()

        val searchable = balanceClause.replace(text, " ")

        // 1. Amount — prefer a value glued to a transaction verb, else a currency amount
        //    that is not part of an account/card number.
        var amountPaise: Long? = null
        amountAfterVerb.find(searchable)?.let { match ->
            amountPaise = Money.parseToPaise(match.groupValues[2])
        }
        if (amountPaise == null) {
            currencyAmount.findAll(searchable).forEach { match ->
                if (amountPaise == null && !isAccountNumber(searchable, match.range.first)) {
                    amountPaise = Money.parseToPaise(match.groupValues[1])
                }
            }
        }
        if (amountPaise == null) {
            bareAmount.findAll(searchable).forEach { match ->
                if (amountPaise == null && !isAccountNumber(searchable, match.range.first)) {
                    amountPaise = Money.parseToPaise(match.groupValues[1])
                }
            }
        }
        val amount = amountPaise?.takeIf { it > 0 } ?: return null

        // 2. Direction
        val debitMatch = debitWord.find(searchable)
        val creditMatch = creditWord.find(searchable)
        val debitAt = debitMatch?.range?.first ?: -1
        val creditAt = creditMatch?.range?.first ?: -1
        val debitLimit = if (debitAt >= 0) debitAt else Int.MAX_VALUE
        val type = when {
            debitAt < 0 && creditAt < 0 -> TxType.EXPENSE
            creditAt in 0 until debitLimit -> TxType.INCOME
            debitAt < 0 -> TxType.INCOME
            else -> TxType.EXPENSE
        }

        // 3. Merchant
        val merchant = extractMerchant(searchable)

        // 4. Payment mode
        val mode = detectMode(searchable)

        // 5. Date
        val epochDay = extractDate(text, today)

        // 6. Account tail
        val last4Value = (last4.find(searchable) ?: ending4.find(searchable))?.groupValues?.get(1)

        // 7. Reference / note
        val ref = refField.find(searchable)?.groupValues?.get(1)
        val note = if (ref != null) "Ref $ref" else ""

        // 8. Category hint
        val hint = guessCategory(searchable, type)

        val found = listOf(merchant.isNotBlank(), epochDay != null, last4Value != null, ref != null)
            .count { it }
        val confidence = (0.45f + found * 0.14f).coerceAtMost(0.98f)

        return ParsedEntry(
            type = type,
            amountPaise = amount,
            merchant = merchant,
            mode = mode,
            epochDay = epochDay,
            accountLast4 = last4Value,
            categoryHint = hint,
            note = note,
            confidence = confidence,
            raw = raw,
        )
    }

    private fun isAccountNumber(text: String, matchStart: Int): Boolean {
        val before = text.substring(0, matchStart).takeLast(18)
        return accountMarkerBeforeAmount.containsMatchIn(before)
    }

    private fun extractMerchant(text: String): String {
        val candidates = mutableListOf<String>()
        vpaWithName.find(text)?.let { candidates += it.groupValues[1] }
        anyParenName.find(text)?.let { candidates += it.groupValues[2] }
        verbToParty.find(text)?.let { candidates += it.groupValues[1] }
        infoField.find(text)?.let { candidates += it.groupValues[1] }
        toParty.find(text)?.let { candidates += it.groupValues[1] }
        atParty.find(text)?.let { candidates += it.groupValues[1] }
        fromParty.find(text)?.let { candidates += it.groupValues[1] }
        if (candidates.isEmpty()) {
            vpaHandle.find(text)?.let { candidates += it.groupValues[1] }
        }
        for (candidate in candidates) {
            val clean = cleanMerchant(candidate)
            if (clean.isNotBlank() && !isNoise(clean)) return clean
        }
        return ""
    }

    private val merchantStopWords = setOf(
        "ref", "upi", "txn", "neft", "imps", "rtgs", "vpa", "info", "from", "to", "at",
        "your", "the", "no", "number", "account", "acct", "bank", "credit", "debit", "card",
        "ending", "end", "using", "via", "through", "towards", "transaction", "trf", "a/c",
    )

    private fun cleanMerchant(value: String): String {
        val out = value
            .replace(Regex("[^A-Za-z0-9&'.\\- ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.', ',', '-', '\'', ' ')
            .split(" ")
            .filter { token ->
                val clean = token.trim('.', ',', '-', '\'', ' ')
                clean.isNotBlank() &&
                    clean.lowercase() !in merchantStopWords &&
                    !clean.all { it.isDigit() } &&
                    !clean.matches(Regex("(?i)[x*]{2,}\\d*"))
            }
            .joinToString(" ")
        return prettify(out.take(34).trim())
    }

    private fun isNoise(value: String): Boolean {
        val v = value.lowercase()
        return v.length < 3 || v in setOf("the", "your", "account", "ac", "bank", "credit", "debit", "card", "sms")
    }

    private fun prettify(value: String): String {
        if (value.isBlank()) return ""
        val letters = value.filter { it.isLetter() }
        val upper = letters.count { it.isUpperCase() }
        val isShouty = letters.isNotEmpty() && upper.toFloat() / letters.length > 0.7
        val isFlat = letters.isNotEmpty() && upper == 0
        if (!isShouty && !isFlat) return value
        return value.split(" ").joinToString(" ") { word ->
            if (word.length <= 3 && word.all { !it.isLetter() || it.isUpperCase() }) word
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    private fun detectMode(text: String): PayMode {
        val t = text.lowercase()
        return when {
            t.contains("credit card") -> PayMode.CREDIT_CARD
            t.contains("debit card") -> PayMode.DEBIT_CARD
            t.contains("atm") || t.contains("withdrawn") || t.contains("cash withdrawal") -> PayMode.CASH
            t.contains("upi") || t.contains("vpa") || Regex("\\b[\\w.\\-]+@[a-z]+\\b").containsMatchIn(t) ||
                t.contains("gpay") || t.contains("google pay") || t.contains("phonepe") || t.contains("paytm") -> PayMode.UPI
            t.contains("neft") || t.contains("imps") || t.contains("rtgs") ||
                t.contains("net banking") || t.contains("netbanking") -> PayMode.NET_BANKING
            else -> PayMode.UPI
        }
    }

    private fun extractDate(text: String, today: LocalDate): Long? {
        dateDmy.find(text)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            val month = m.groupValues[2].toIntOrNull()
            var year = m.groupValues[3].toIntOrNull() ?: return@let
            if (year < 100) year += 2000
            validDate(year, month, day, today)?.let { return it.toEpochDay() }
        }
        dateDMonY.find(text)?.let { m ->
            val day = m.groupValues[1].toIntOrNull()
            val month = monthNames[m.groupValues[2].lowercase()]
            var year = m.groupValues[3].toIntOrNull() ?: return@let
            if (year < 100) year += 2000
            if (month != null) validDate(year, month, day, today)?.let { return it.toEpochDay() }
        }
        return null
    }

    private fun validDate(year: Int, month: Int?, day: Int?, today: LocalDate): LocalDate? {
        if (month == null || day == null) return null
        if (month !in 1..12 || day !in 1..31) return null
        return try {
            val date = LocalDate.of(year, month, day)
            val delta = date.toEpochDay() - today.toEpochDay()
            if (delta > 1 || delta < -400) null else date
        } catch (_: Exception) {
            null
        }
    }

    private fun guessCategory(text: String, type: TxType): String? {
        val t = text.lowercase()
        if (type == TxType.INCOME) {
            return categoryKeywords
                .firstOrNull { (name, keys) -> name in listOf("Salary", "Interest", "Refunds") && keys.any { t.contains(it) } }
                ?.first
        }
        return categoryKeywords.firstOrNull { (_, keys) -> keys.any { t.contains(it) } }?.first
    }
}

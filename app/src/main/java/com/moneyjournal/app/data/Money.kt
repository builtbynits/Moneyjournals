package com.moneyjournal.app.data

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * All money in Money Journal is stored as paise (1 rupee = 100 paise) in a Long,
 * so arithmetic never drifts. This object owns every display/parse conversion.
 */
object Money {

    private const val SYMBOL = "\u20B9"

    /** Groups digits the Indian way: 12,34,56,789. */
    fun groupIndian(absRupees: Long): String {
        val digits = absRupees.toString()
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        var rest = digits.dropLast(3)
        val parts = mutableListOf<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    /** ₹1,23,456.78 — decimals only when paise are non-zero. */
    fun formatPaise(paise: Long, withSymbol: Boolean = true, forceDecimals: Boolean = false): String {
        val negative = paise < 0
        val abs = kotlin.math.abs(paise)
        val rupees = abs / 100
        val fraction = (abs % 100).toInt()
        val sign = if (negative) "-" else ""
        val prefix = if (withSymbol) SYMBOL else ""
        val body = if (fraction != 0 || forceDecimals) {
            "${groupIndian(rupees)}.${fraction.toString().padStart(2, '0')}"
        } else {
            groupIndian(rupees)
        }
        return "$sign$prefix$body"
    }

    /** Compact form for tight spots: ₹9,999 · ₹1.2L · ₹2.4Cr */
    fun compact(paise: Long): String {
        val negative = paise < 0
        val rupees = kotlin.math.abs(paise) / 100
        val sign = if (negative) "-" else ""
        return when {
            rupees >= 10_000_000 -> "$sign$SYMBOL${oneDecimal(rupees / 10_000_000.0)}Cr"
            rupees >= 100_000 -> "$sign$SYMBOL${oneDecimal(rupees / 100_000.0)}L"
            rupees >= 1_000 -> "$sign$SYMBOL${oneDecimal(rupees / 1_000.0)}K"
            else -> "$sign$SYMBOL$rupees"
        }
    }

    private fun oneDecimal(value: Double): String {
        val rounded = BigDecimal(value).setScale(1, RoundingMode.HALF_UP)
        return if (rounded.stripTrailingZeros().scale() <= 0) rounded.toBigInteger().toString()
        else rounded.toPlainString()
    }

    /** "1,234.5" / "₹ 1 234" / "1234" -> paise. Null when it is not a valid money amount. */
    fun parseToPaise(raw: String?): Long? {
        if (raw == null) return null
        var text = raw.trim()
            .replace("\u20B9", "")
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("INR", "", ignoreCase = true)
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", "")
        if (text.isEmpty()) return null
        val negative = text.startsWith("-")
        text = text.removePrefix("-").removePrefix("+")
        if (text.isEmpty() || !text.matches(Regex("^\\d*(\\.\\d*)?$"))) return null
        if (text == "." ) return null
        val parts = text.split(".")
        if (parts.size > 2) return null
        val rupeesPart = parts[0].ifEmpty { "0" }
        val paisePart = if (parts.size == 2) parts[1] else ""
        if (paisePart.length > 2) return null
        if (rupeesPart.length > 12) return null
        val rupees = rupeesPart.toLongOrNull() ?: return null
        val paise = when (paisePart.length) {
            0 -> 0L
            1 -> paisePart.toLong() * 10
            else -> paisePart.toLong()
        }
        val total = rupees * 100 + paise
        return if (negative) -total else total
    }

    /** Digits-only keypad entry to paise: "12.5" -> 1250. */
    fun keypadToPaise(entry: String): Long = parseToPaise(entry) ?: 0L

    fun paiseToInput(paise: Long): String {
        val rupees = paise / 100
        val fraction = (paise % 100).toInt()
        return if (fraction == 0) rupees.toString()
        else "$rupees.${fraction.toString().padStart(2, '0')}"
    }

    /** Splits for large display: "1,23,456" + ".50" */
    fun split(paise: Long): Pair<String, String> {
        val abs = kotlin.math.abs(paise)
        val rupees = groupIndian(abs / 100)
        val fraction = abs % 100
        val sign = if (paise < 0) "-" else ""
        val frac = if (fraction == 0L) "" else ".${fraction.toString().padStart(2, '0')}"
        return (sign + rupees) to frac
    }
}

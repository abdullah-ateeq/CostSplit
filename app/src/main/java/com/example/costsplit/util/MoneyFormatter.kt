package com.example.costsplit.util

import com.example.costsplit.model.CurrencyOption
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats minor units into user-facing currency strings and formats percentage basis points.
 */
object MoneyFormatter {

    /**
     * Formats minor units into a formatted string with currency symbol and thousands separators.
     * Examples: "$1,250.00", "¥1,250", "₨1,250.00"
     */
    fun format(
        minorUnits: Long,
        currency: CurrencyOption,
        includeSymbol: Boolean = true
    ): String {
        val divisor = BigDecimal.TEN.pow(currency.decimalPlaces)
        val amount = BigDecimal.valueOf(minorUnits).divide(divisor)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val pattern = buildString {
            append("#,##0")
            if (currency.decimalPlaces > 0) {
                append(".")
                repeat(currency.decimalPlaces) { append("0") }
            }
        }

        val decimalFormat = DecimalFormat(pattern, symbols)
        val formattedNumber = decimalFormat.format(amount)

        return if (includeSymbol) {
            val symbolTrimmed = currency.symbol.trim()
            val needsSpace = symbolTrimmed.length > 1 &&
                !symbolTrimmed.startsWith("CA$") &&
                !symbolTrimmed.startsWith("A$")
            val space = if (needsSpace) " " else ""
            "$symbolTrimmed$space$formattedNumber"
        } else {
            formattedNumber
        }
    }

    /**
     * Formats basis points (10000 = 100%) into percentage string with up to 2 decimal places.
     * Examples: 5000 -> "50%", 3333 -> "33.33%", 1250 -> "12.5%", 10000 -> "100%", 0 -> "0%"
     */
    fun formatBasisPoints(basisPoints: Int): String {
        val whole = basisPoints / 100
        val frac = basisPoints % 100

        return when {
            frac == 0 -> "$whole%"
            frac % 10 == 0 -> "$whole.${frac / 10}%"
            else -> {
                val fracStr = if (frac < 10) "0$frac" else "$frac"
                "$whole.$fracStr%"
            }
        }
    }

    /**
     * Formats basis points into clean decimal string without "%" for textfields.
     * Examples: 5000 -> "50", 3333 -> "33.33", 1250 -> "12.5"
     */
    fun basisPointsToInputString(basisPoints: Int): String {
        val whole = basisPoints / 100
        val frac = basisPoints % 100

        return when {
            frac == 0 -> "$whole"
            frac % 10 == 0 -> "$whole.${frac / 10}"
            else -> {
                val fracStr = if (frac < 10) "0$frac" else "$frac"
                "$whole.$fracStr"
            }
        }
    }
}

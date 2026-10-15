package com.example.costsplit.util

import com.example.costsplit.calculator.CostSplitCalculator
import com.example.costsplit.model.CurrencyOption
import java.math.BigDecimal

/**
 * Parses user numeric strings into Long minor units according to currency precision.
 */
object MoneyParser {

    sealed class ParseResult {
        data class Success(val minorUnits: Long) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }

    /**
     * Parses an expense input string into minor units.
     *
     * @param rawInput user input, e.g. "240", "1000.50", "1,250.00"
     * @param currency selected currency option with decimalPlaces
     */
    fun parseExpense(rawInput: String, currency: CurrencyOption): ParseResult {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return ParseResult.Error("Enter a valid expense amount.")
        }

        // Clean thousand separators (spaces or commas when dot is decimal, or periods when comma is decimal)
        // Normalize: replace comma with dot if there is only comma as decimal point
        val normalized = normalizeNumberString(trimmed)
            ?: return ParseResult.Error("Enter a valid expense amount.")

        val bigDecimal = try {
            BigDecimal(normalized)
        } catch (_: Exception) {
            return ParseResult.Error("Enter a valid expense amount.")
        }

        if (bigDecimal < BigDecimal.ZERO) {
            return ParseResult.Error("Enter a valid expense amount.")
        }

        // Check decimal places precision
        val scale = bigDecimal.scale().coerceAtLeast(0)
        if (scale > currency.decimalPlaces) {
            return ParseResult.Error(
                if (currency.decimalPlaces == 0) {
                    "${currency.code} does not use decimal places."
                } else {
                    "Maximum ${currency.decimalPlaces} decimal places allowed."
                }
            )
        }

        val multiplier = BigDecimal.TEN.pow(currency.decimalPlaces)
        val minorUnitsBigDecimal = bigDecimal.multiply(multiplier)

        return try {
            val minorUnits = minorUnitsBigDecimal.longValueExact()
            if (minorUnits > CostSplitCalculator.MAX_EXPENSE_MINOR_UNITS) {
                ParseResult.Error("Expense amount is too large.")
            } else {
                ParseResult.Success(minorUnits)
            }
        } catch (_: ArithmeticException) {
            ParseResult.Error("Expense amount is too large.")
        }
    }

    /**
     * Parses a percentage input string (e.g. "25", "33.33", "50%") into basis points (0..10,000).
     */
    fun parsePercentage(rawInput: String): Int? {
        val cleaned = rawInput.trim().removeSuffix("%").trim()
        if (cleaned.isEmpty()) return null

        val normalized = normalizeNumberString(cleaned) ?: return null
        val bigDecimal = try {
            BigDecimal(normalized)
        } catch (_: Exception) {
            return null
        }

        if (bigDecimal < BigDecimal.ZERO || bigDecimal > BigDecimal(100)) {
            return null
        }

        // Allow up to 2 decimal places
        if (bigDecimal.scale() > 2) {
            return null
        }

        val basisPointsBigDecimal = bigDecimal.multiply(BigDecimal(100))
        return try {
            val bp = basisPointsBigDecimal.intValueExact()
            if (bp in 0..10000) bp else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses a share integer string.
     */
    fun parseShare(rawInput: String): Long? {
        val trimmed = rawInput.trim().replace(",", "")
        val value = trimmed.toLongOrNull() ?: return null
        return if (value in 1L..CostSplitCalculator.MAX_SHARE_VALUE) value else null
    }

    private fun normalizeNumberString(input: String): String? {
        var str = input.replace(" ", "")

        // Check for multiple dots or commas
        val dotCount = str.count { it == '.' }
        val commaCount = str.count { it == ',' }

        if (dotCount > 1 && commaCount > 0) return null
        if (commaCount > 1 && dotCount > 0) return null

        return if (dotCount == 1 && commaCount >= 1) {
            // Comma as thousands separator: e.g. 1,000.50
            str.replace(",", "")
        } else if (commaCount == 1 && dotCount >= 1) {
            // Dot as thousands separator: e.g. 1.000,50
            str.replace(".", "").replace(',', '.')
        } else if (commaCount == 1 && dotCount == 0) {
            // Single comma: treat as decimal separator e.g. 12,50
            str.replace(',', '.')
        } else if (dotCount <= 1 && commaCount == 0) {
            // Standard dot decimal e.g. 12.50
            str
        } else {
            // Multiple commas with no dots: e.g. 1,000,000
            if (commaCount > 0 && dotCount == 0) {
                str.replace(",", "")
            } else {
                null
            }
        }
    }
}

package com.example.costsplit.model

/**
 * Currency configuration with code, symbol, display name, and minor unit precision.
 */
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String,
    val decimalPlaces: Int
) {
    companion object {
        val USD = CurrencyOption("USD", "$", "US Dollar", 2)
        val EUR = CurrencyOption("EUR", "€", "Euro", 2)
        val GBP = CurrencyOption("GBP", "£", "British Pound", 2)
        val PKR = CurrencyOption("PKR", "Rs.", "Pakistani Rupee", 2)
        val AED = CurrencyOption("AED", "د.إ", "UAE Dirham", 2)
        val SAR = CurrencyOption("SAR", "﷼", "Saudi Riyal", 2)
        val CAD = CurrencyOption("CAD", "CA$", "Canadian Dollar", 2)
        val AUD = CurrencyOption("AUD", "A$", "Australian Dollar", 2)
        val INR = CurrencyOption("INR", "₹", "Indian Rupee", 2)
        val JPY = CurrencyOption("JPY", "¥", "Japanese Yen", 0)
        val CNY = CurrencyOption("CNY", "¥", "Chinese Yuan", 2)

        val DEFAULT_CURRENCIES = listOf(
            USD, EUR, GBP, PKR, AED, SAR, CAD, AUD, INR, JPY, CNY
        )

        val OTHER_DEFAULT = CurrencyOption("CUSTOM", "¤", "Other", 2)
    }
}

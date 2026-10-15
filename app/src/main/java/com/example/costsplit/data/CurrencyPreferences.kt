package com.example.costsplit.data

import android.content.Context
import android.content.SharedPreferences
import com.example.costsplit.model.CurrencyOption

/**
 * Manages persistence for the selected currency using Android SharedPreferences.
 */
class CurrencyPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedCurrency(): CurrencyOption {
        val code = prefs.getString(KEY_CODE, CurrencyOption.USD.code) ?: CurrencyOption.USD.code
        val symbol = prefs.getString(KEY_SYMBOL, CurrencyOption.USD.symbol) ?: CurrencyOption.USD.symbol
        val name = prefs.getString(KEY_NAME, CurrencyOption.USD.name) ?: CurrencyOption.USD.name
        val decimals = prefs.getInt(KEY_DECIMALS, CurrencyOption.USD.decimalPlaces)

        return CurrencyOption(
            code = code,
            symbol = symbol,
            name = name,
            decimalPlaces = decimals
        )
    }

    fun saveSelectedCurrency(currency: CurrencyOption) {
        prefs.edit()
            .putString(KEY_CODE, currency.code)
            .putString(KEY_SYMBOL, currency.symbol)
            .putString(KEY_NAME, currency.name)
            .putInt(KEY_DECIMALS, currency.decimalPlaces)
            .apply()
    }

    fun getSavedCustomCurrency(): CurrencyOption {
        val code = prefs.getString(KEY_CUSTOM_CODE, "CUSTOM") ?: "CUSTOM"
        val symbol = prefs.getString(KEY_CUSTOM_SYMBOL, "¤") ?: "¤"
        val name = prefs.getString(KEY_CUSTOM_NAME, "Other") ?: "Other"
        val decimals = prefs.getInt(KEY_CUSTOM_DECIMALS, 2)
        return CurrencyOption(code, symbol, name, decimals)
    }

    fun saveCustomCurrency(currency: CurrencyOption) {
        prefs.edit()
            .putString(KEY_CUSTOM_CODE, currency.code)
            .putString(KEY_CUSTOM_SYMBOL, currency.symbol)
            .putString(KEY_CUSTOM_NAME, currency.name)
            .putInt(KEY_CUSTOM_DECIMALS, currency.decimalPlaces)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "cost_split_preferences"
        private const val KEY_CODE = "currency_code"
        private const val KEY_SYMBOL = "currency_symbol"
        private const val KEY_NAME = "currency_name"
        private const val KEY_DECIMALS = "currency_decimals"

        private const val KEY_CUSTOM_CODE = "custom_currency_code"
        private const val KEY_CUSTOM_SYMBOL = "custom_currency_symbol"
        private const val KEY_CUSTOM_NAME = "custom_currency_name"
        private const val KEY_CUSTOM_DECIMALS = "custom_currency_decimals"
    }
}

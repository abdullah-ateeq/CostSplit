package com.example.costsplit

import com.example.costsplit.model.CurrencyOption
import com.example.costsplit.util.MoneyFormatter
import com.example.costsplit.util.MoneyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyParserAndFormatterTest {

    private val usd = CurrencyOption.USD
    private val jpy = CurrencyOption.JPY
    private val pkr = CurrencyOption.PKR

    @Test
    fun testMoneyParser_validInputs() {
        val r1 = MoneyParser.parseExpense("240", usd)
        assertTrue(r1 is MoneyParser.ParseResult.Success)
        assertEquals(24000L, (r1 as MoneyParser.ParseResult.Success).minorUnits)

        val r2 = MoneyParser.parseExpense("240.00", usd)
        assertTrue(r2 is MoneyParser.ParseResult.Success)
        assertEquals(24000L, (r2 as MoneyParser.ParseResult.Success).minorUnits)

        val r3 = MoneyParser.parseExpense("1,250.50", usd)
        assertTrue(r3 is MoneyParser.ParseResult.Success)
        assertEquals(125050L, (r3 as MoneyParser.ParseResult.Success).minorUnits)

        val r4 = MoneyParser.parseExpense("0", usd)
        assertTrue(r4 is MoneyParser.ParseResult.Success)
        assertEquals(0L, (r4 as MoneyParser.ParseResult.Success).minorUnits)
    }

    @Test
    fun testMoneyParser_jpyZeroDecimals() {
        val r1 = MoneyParser.parseExpense("1250", jpy)
        assertTrue(r1 is MoneyParser.ParseResult.Success)
        assertEquals(1250L, (r1 as MoneyParser.ParseResult.Success).minorUnits)

        val r2 = MoneyParser.parseExpense("1250.5", jpy)
        assertTrue(r2 is MoneyParser.ParseResult.Error)
    }

    @Test
    fun testMoneyParser_invalidAndNegative() {
        val r1 = MoneyParser.parseExpense("-50", usd)
        assertTrue(r1 is MoneyParser.ParseResult.Error)

        val r2 = MoneyParser.parseExpense("abc", usd)
        assertTrue(r2 is MoneyParser.ParseResult.Error)

        val r3 = MoneyParser.parseExpense("", usd)
        assertTrue(r3 is MoneyParser.ParseResult.Error)
    }

    @Test
    fun testMoneyParser_percentageParsing() {
        assertEquals(5000, MoneyParser.parsePercentage("50"))
        assertEquals(5000, MoneyParser.parsePercentage("50%"))
        assertEquals(3333, MoneyParser.parsePercentage("33.33"))
        assertEquals(1250, MoneyParser.parsePercentage("12.5%"))
        assertEquals(0, MoneyParser.parsePercentage("0"))
        assertEquals(10000, MoneyParser.parsePercentage("100"))

        assertNull(MoneyParser.parsePercentage("100.01"))
        assertNull(MoneyParser.parsePercentage("-5"))
        assertNull(MoneyParser.parsePercentage("abc"))
    }

    @Test
    fun testMoneyFormatter_formatting() {
        assertEquals("$240.00", MoneyFormatter.format(24000L, usd))
        assertEquals("$1,250.00", MoneyFormatter.format(125000L, usd))
        assertEquals("¥1,250", MoneyFormatter.format(1250L, jpy))
        assertEquals("Rs. 1,250.00", MoneyFormatter.format(125000L, pkr))
    }

    @Test
    fun testMoneyFormatter_percentages() {
        assertEquals("50%", MoneyFormatter.formatBasisPoints(5000))
        assertEquals("33.33%", MoneyFormatter.formatBasisPoints(3333))
        assertEquals("12.5%", MoneyFormatter.formatBasisPoints(1250))
        assertEquals("100%", MoneyFormatter.formatBasisPoints(10000))
        assertEquals("0%", MoneyFormatter.formatBasisPoints(0))
    }
}

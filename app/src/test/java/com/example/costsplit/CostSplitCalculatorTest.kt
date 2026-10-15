package com.example.costsplit

import com.example.costsplit.calculator.CostSplitCalculator
import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.model.CostPerson
import com.example.costsplit.model.CurrencyOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CostSplitCalculatorTest {

    private val usd = CurrencyOption.USD
    private val jpy = CurrencyOption.JPY

    @Test
    fun testEqualSplit_exactDivision() {
        // $240 among 4 people = $60 each (24000 cents)
        val result = CostSplitCalculator.splitEqually(
            totalMinorUnits = 24000L,
            peopleCount = 4,
            currency = usd
        )

        assertEquals(24000L, result.totalAllocatedMinorUnits)
        assertEquals(0L, result.remainingMinorUnits)
        assertEquals(4, result.people.size)
        assertFalse(result.hasRoundingAdjustment)

        for (person in result.people) {
            assertEquals(6000L, person.allocatedMinorUnits)
            assertEquals(2500, person.effectivePercentageBasisPoints)
        }
    }

    @Test
    fun testEqualSplit_remainderOneCent() {
        // $100.00 among 3 people = 10000 cents / 3 -> 3334, 3333, 3333
        val result = CostSplitCalculator.splitEqually(
            totalMinorUnits = 10000L,
            peopleCount = 3,
            currency = usd
        )

        assertEquals(10000L, result.totalAllocatedMinorUnits)
        assertTrue(result.hasRoundingAdjustment)
        assertEquals(3334L, result.people[0].allocatedMinorUnits)
        assertEquals(3333L, result.people[1].allocatedMinorUnits)
        assertEquals(3333L, result.people[2].allocatedMinorUnits)
    }

    @Test
    fun testCustomShares_exampleFromPrompt() {
        // Expense: $240 (24000 cents)
        // Alex: 2 shares, Sam: 1 share, John: 1 share
        // Total: 4 shares -> Alex: $120 (12000), Sam: $60 (6000), John: $60 (6000)
        val people = listOf(
            CostPerson("1", "Alex", 2L),
            CostPerson("2", "Sam", 1L),
            CostPerson("3", "John", 1L)
        )

        val result = CostSplitCalculator.splitByShares(
            totalMinorUnits = 24000L,
            people = people,
            currency = usd
        )

        assertEquals(24000L, result.totalAllocatedMinorUnits)
        assertEquals(12000L, result.people[0].allocatedMinorUnits)
        assertEquals(6000L, result.people[1].allocatedMinorUnits)
        assertEquals(6000L, result.people[2].allocatedMinorUnits)

        assertEquals(5000, result.people[0].effectivePercentageBasisPoints)
        assertEquals(2500, result.people[1].effectivePercentageBasisPoints)
        assertEquals(2500, result.people[2].effectivePercentageBasisPoints)
    }

    @Test
    fun testCustomPercentages_exampleFromPrompt() {
        // Expense: $240 (24000 cents)
        // Alex: 50% (5000), Sam: 30% (3000), John: 20% (2000)
        // Alex: $120 (12000), Sam: $72 (7200), John: $48 (4800)
        val people = listOf(
            CostPerson("1", "Alex", 1L),
            CostPerson("2", "Sam", 1L),
            CostPerson("3", "John", 1L)
        )
        val percentages = listOf(5000, 3000, 2000)

        val result = CostSplitCalculator.splitByPercentages(
            totalMinorUnits = 24000L,
            people = people,
            percentageUnits = percentages,
            currency = usd
        )

        assertEquals(24000L, result.totalAllocatedMinorUnits)
        assertEquals(12000L, result.people[0].allocatedMinorUnits)
        assertEquals(7200L, result.people[1].allocatedMinorUnits)
        assertEquals(4800L, result.people[2].allocatedMinorUnits)
    }

    @Test
    fun testZeroExpense() {
        // Zero expense is allowed: everyone gets $0.00
        val result = CostSplitCalculator.splitEqually(
            totalMinorUnits = 0L,
            peopleCount = 4,
            currency = usd
        )

        assertEquals(0L, result.totalAllocatedMinorUnits)
        for (person in result.people) {
            assertEquals(0L, person.allocatedMinorUnits)
        }
    }

    @Test
    fun testZeroPercentage_isAllowed() {
        // Alex: 0%, Sam: 100% -> Alex receives $0.00, Sam receives total
        val people = listOf(
            CostPerson("1", "Alex", 1L),
            CostPerson("2", "Sam", 1L)
        )
        val percentages = listOf(0, 10000)

        val result = CostSplitCalculator.splitByPercentages(
            totalMinorUnits = 10000L,
            people = people,
            percentageUnits = percentages,
            currency = usd
        )

        assertEquals(0L, result.people[0].allocatedMinorUnits)
        assertEquals(10000L, result.people[1].allocatedMinorUnits)
        assertEquals(10000L, result.totalAllocatedMinorUnits)
    }

    @Test
    fun testDeterministicLargestRemainder() {
        // $10.00 (1000 cents), 3 people with 1 share each
        // 1000 / 3 = 333 remainder 1. Winner should be person 0 (first in order)
        val people = listOf(
            CostPerson("1", "A", 1L),
            CostPerson("2", "B", 1L),
            CostPerson("3", "C", 1L)
        )
        val result1 = CostSplitCalculator.splitByShares(1000L, people, usd)
        val result2 = CostSplitCalculator.splitByShares(1000L, people, usd)

        assertEquals(334L, result1.people[0].allocatedMinorUnits)
        assertEquals(333L, result1.people[1].allocatedMinorUnits)
        assertEquals(333L, result1.people[2].allocatedMinorUnits)

        // Must be deterministic
        for (i in people.indices) {
            assertEquals(result1.people[i].allocatedMinorUnits, result2.people[i].allocatedMinorUnits)
        }
    }

    @Test
    fun testLargeExpenseWithLargeShares_noOverflow() {
        // 500,000,000,000 minor units ($5 Billion)
        // Person 1: 500,000,000 shares, Person 2: 500,000,000 shares
        val people = listOf(
            CostPerson("1", "A", 500_000_000L),
            CostPerson("2", "B", 500_000_000L)
        )
        val total = 500_000_000_000L
        val result = CostSplitCalculator.splitByShares(total, people, usd)

        assertEquals(total, result.totalAllocatedMinorUnits)
        assertEquals(250_000_000_000L, result.people[0].allocatedMinorUnits)
        assertEquals(250_000_000_000L, result.people[1].allocatedMinorUnits)
    }

    @Test
    fun testZeroDecimalCurrency_JPY() {
        val people = listOf(
            CostPerson("1", "A", 1L),
            CostPerson("2", "B", 2L)
        )
        // 1000 yen among 1 and 2 shares -> 333 and 667
        val result = CostSplitCalculator.splitByShares(1000L, people, jpy)
        assertEquals(1000L, result.totalAllocatedMinorUnits)
        assertEquals(333L, result.people[0].allocatedMinorUnits)
        assertEquals(667L, result.people[1].allocatedMinorUnits)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeExpense_throwsException() {
        CostSplitCalculator.splitEqually(-100L, 4, usd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testExcessiveExpense_throwsException() {
        CostSplitCalculator.splitEqually(1_000_000_000_001L, 4, usd)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroShare_throwsException() {
        val people = listOf(
            CostPerson("1", "A", 0L),
            CostPerson("2", "B", 1L)
        )
        CostSplitCalculator.splitByShares(1000L, people, usd)
    }
}

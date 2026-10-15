package com.example.costsplit.calculator

import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.model.CostPerson
import com.example.costsplit.model.CostPersonResult
import com.example.costsplit.model.CostSplitResult
import com.example.costsplit.model.CurrencyOption
import java.math.BigInteger

/**
 * Pure calculation engine for CostSplit.
 *
 * Invariant: For every valid calculation, sum(person.allocatedMinorUnits) == totalMinorUnits.
 * Employs deterministic largest-remainder allocation for all custom distributions.
 */
object CostSplitCalculator {

    const val MAX_EXPENSE_MINOR_UNITS: Long = 1_000_000_000_000L // 1 Trillion minor units
    const val MAX_SHARE_VALUE: Long = 1_000_000_000L // 1 Billion
    const val MIN_PEOPLE: Int = 2
    const val MAX_PEOPLE: Int = 50
    const val BASIS_POINTS_100_PERCENT: Int = 10000 // 100.00%

    /**
     * Splits an expense equally among peopleCount people.
     */
    fun splitEqually(
        totalMinorUnits: Long,
        peopleCount: Int,
        currency: CurrencyOption
    ): CostSplitResult {
        require(peopleCount in MIN_PEOPLE..MAX_PEOPLE) {
            "People count must be between $MIN_PEOPLE and $MAX_PEOPLE."
        }
        val defaultPeople = (1..peopleCount).map { i ->
            CostPerson(id = "person_$i", name = "Person $i", share = 1L)
        }
        return splitEqually(totalMinorUnits, defaultPeople, currency, "Untitled Expense")
    }

    /**
     * Splits an expense equally with custom person names.
     */
    fun splitEqually(
        totalMinorUnits: Long,
        people: List<CostPerson>,
        currency: CurrencyOption,
        expenseDescription: String = "Untitled Expense"
    ): CostSplitResult {
        validateCommon(totalMinorUnits, people)

        val peopleCount = people.size
        val base = totalMinorUnits / peopleCount
        val remainder = (totalMinorUnits % peopleCount).toInt()

        val results = people.mapIndexed { index, person ->
            val isRemainderRecipient = index < remainder
            val allocated = if (isRemainderRecipient) base + 1L else base
            val displayName = person.name.trim().ifEmpty { "Person ${index + 1}" }

            val basisPoints = if (totalMinorUnits > 0L) {
                ((allocated.toBigDecimal() * BASIS_POINTS_100_PERCENT.toBigDecimal())
                    .divide(totalMinorUnits.toBigDecimal(), java.math.MathContext.DECIMAL64))
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .toInt()
            } else {
                BASIS_POINTS_100_PERCENT / peopleCount
            }

            CostPersonResult(
                id = person.id,
                name = displayName,
                allocationUnits = 1L,
                allocatedMinorUnits = allocated,
                effectivePercentageBasisPoints = basisPoints
            )
        }

        val totalAllocated = results.sumOf { it.allocatedMinorUnits }
        check(totalAllocated == totalMinorUnits) {
            "Invariant violation: allocated $totalAllocated != total $totalMinorUnits"
        }

        val description = expenseDescription.trim().ifEmpty { "Untitled Expense" }

        return CostSplitResult(
            expenseDescription = description,
            totalMinorUnits = totalMinorUnits,
            people = results,
            totalAllocatedMinorUnits = totalAllocated,
            remainingMinorUnits = 0L,
            method = AllocationMethod.EQUAL,
            currency = currency,
            hasRoundingAdjustment = remainder > 0
        )
    }

    /**
     * Splits an expense according to custom share weights using deterministic largest-remainder.
     */
    fun splitByShares(
        totalMinorUnits: Long,
        people: List<CostPerson>,
        currency: CurrencyOption,
        expenseDescription: String = "Untitled Expense"
    ): CostSplitResult {
        validateCommon(totalMinorUnits, people)

        for (p in people) {
            require(p.share in 1L..MAX_SHARE_VALUE) {
                "Share must be a positive whole number between 1 and $MAX_SHARE_VALUE."
            }
        }

        val totalShares = people.sumOf { it.share }
        require(totalShares > 0L) { "Total shares must be greater than zero." }

        val weights = people.map { it.share }
        val allocatedAmounts = allocateLargestRemainder(totalMinorUnits, weights, totalShares)

        val results = people.mapIndexed { index, person ->
            val displayName = person.name.trim().ifEmpty { "Person ${index + 1}" }
            val effectiveBasisPoints = ((person.share.toBigDecimal() * BASIS_POINTS_100_PERCENT.toBigDecimal())
                .divide(totalShares.toBigDecimal(), java.math.MathContext.DECIMAL64))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toInt()

            CostPersonResult(
                id = person.id,
                name = displayName,
                allocationUnits = person.share,
                allocatedMinorUnits = allocatedAmounts[index],
                effectivePercentageBasisPoints = effectiveBasisPoints
            )
        }

        val totalAllocated = results.sumOf { it.allocatedMinorUnits }
        check(totalAllocated == totalMinorUnits) {
            "Invariant violation: allocated $totalAllocated != total $totalMinorUnits"
        }

        val description = expenseDescription.trim().ifEmpty { "Untitled Expense" }
        val hasRounding = weights.any { weight ->
            (BigInteger.valueOf(totalMinorUnits) * BigInteger.valueOf(weight))
                .remainder(BigInteger.valueOf(totalShares)) != BigInteger.ZERO
        }

        return CostSplitResult(
            expenseDescription = description,
            totalMinorUnits = totalMinorUnits,
            people = results,
            totalAllocatedMinorUnits = totalAllocated,
            remainingMinorUnits = 0L,
            method = AllocationMethod.SHARES,
            currency = currency,
            hasRoundingAdjustment = hasRounding
        )
    }

    /**
     * Splits an expense according to custom percentage units (10,000 units = 100.00%).
     */
    fun splitByPercentages(
        totalMinorUnits: Long,
        people: List<CostPerson>,
        percentageUnits: List<Int>,
        currency: CurrencyOption,
        expenseDescription: String = "Untitled Expense"
    ): CostSplitResult {
        validateCommon(totalMinorUnits, people)
        require(percentageUnits.size == people.size) {
            "Percentage units count (${percentageUnits.size}) must match people count (${people.size})."
        }

        for (unit in percentageUnits) {
            require(unit in 0..BASIS_POINTS_100_PERCENT) {
                "Each percentage must be between 0% and 100%."
            }
        }

        val totalPercentages = percentageUnits.sum()
        require(totalPercentages == BASIS_POINTS_100_PERCENT) {
            "Total percentage must equal exactly 100% (was ${(totalPercentages / 100.0)}%)."
        }

        val weights = percentageUnits.map { it.toLong() }
        val allocatedAmounts = allocateLargestRemainder(
            totalMinorUnits = totalMinorUnits,
            weights = weights,
            totalWeight = BASIS_POINTS_100_PERCENT.toLong()
        )

        val results = people.mapIndexed { index, person ->
            val displayName = person.name.trim().ifEmpty { "Person ${index + 1}" }
            CostPersonResult(
                id = person.id,
                name = displayName,
                allocationUnits = percentageUnits[index].toLong(),
                allocatedMinorUnits = allocatedAmounts[index],
                effectivePercentageBasisPoints = percentageUnits[index]
            )
        }

        val totalAllocated = results.sumOf { it.allocatedMinorUnits }
        check(totalAllocated == totalMinorUnits) {
            "Invariant violation: allocated $totalAllocated != total $totalMinorUnits"
        }

        val description = expenseDescription.trim().ifEmpty { "Untitled Expense" }
        val hasRounding = weights.any { weight ->
            (BigInteger.valueOf(totalMinorUnits) * BigInteger.valueOf(weight))
                .remainder(BigInteger.valueOf(BASIS_POINTS_100_PERCENT.toLong())) != BigInteger.ZERO
        }

        return CostSplitResult(
            expenseDescription = description,
            totalMinorUnits = totalMinorUnits,
            people = results,
            totalAllocatedMinorUnits = totalAllocated,
            remainingMinorUnits = 0L,
            method = AllocationMethod.PERCENTAGES,
            currency = currency,
            hasRoundingAdjustment = hasRounding
        )
    }

    /**
     * Deterministic largest-remainder allocation algorithm.
     *
     * 1. Calculate exact floor allocation for each weight.
     * 2. Calculate fractional remainders.
     * 3. Assign remaining minor units to largest fractional remainders.
     * 4. Use original person order (ascending index) as tie-breaker.
     * 5. Verify final total.
     */
    private fun allocateLargestRemainder(
        totalMinorUnits: Long,
        weights: List<Long>,
        totalWeight: Long
    ): LongArray {
        val count = weights.size
        val floorAmounts = LongArray(count)
        val fractionRemainders = LongArray(count)

        val totalMinorBig = BigInteger.valueOf(totalMinorUnits)
        val totalWeightBig = BigInteger.valueOf(totalWeight)

        var sumFloors = 0L

        for (i in 0 until count) {
            val weightBig = BigInteger.valueOf(weights[i])
            val product = totalMinorBig.multiply(weightBig)
            val floor = product.divide(totalWeightBig).toLong()
            val remainder = product.remainder(totalWeightBig).toLong()

            floorAmounts[i] = floor
            fractionRemainders[i] = remainder
            sumFloors += floor
        }

        val remainingUnits = (totalMinorUnits - sumFloors).toInt()
        check(remainingUnits in 0 until count) {
            "Unexpected remainder: $remainingUnits for count $count"
        }

        val allocated = floorAmounts.clone()

        if (remainingUnits > 0) {
            // Sort by fractional remainder descending, then index ascending for deterministic tie-breaking
            val sortedIndices = (0 until count).sortedWith { a, b ->
                val cmp = fractionRemainders[b].compareTo(fractionRemainders[a])
                if (cmp != 0) cmp else a.compareTo(b)
            }

            for (r in 0 until remainingUnits) {
                val winnerIndex = sortedIndices[r]
                allocated[winnerIndex] += 1L
            }
        }

        return allocated
    }

    private fun validateCommon(totalMinorUnits: Long, people: List<CostPerson>) {
        require(totalMinorUnits >= 0L) { "Enter a valid expense amount." }
        require(totalMinorUnits <= MAX_EXPENSE_MINOR_UNITS) { "Expense amount is too large." }
        require(people.size in MIN_PEOPLE..MAX_PEOPLE) {
            "People count must be between $MIN_PEOPLE and $MAX_PEOPLE."
        }
    }
}

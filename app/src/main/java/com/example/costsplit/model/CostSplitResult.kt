package com.example.costsplit.model

/**
 * Result of the full expense split calculation.
 *
 * Invariant: sum of people.allocatedMinorUnits must equal totalMinorUnits.
 */
data class CostSplitResult(
    val expenseDescription: String,
    val totalMinorUnits: Long,
    val people: List<CostPersonResult>,
    val totalAllocatedMinorUnits: Long,
    val remainingMinorUnits: Long,
    val method: AllocationMethod,
    val currency: CurrencyOption,
    val hasRoundingAdjustment: Boolean = false
)

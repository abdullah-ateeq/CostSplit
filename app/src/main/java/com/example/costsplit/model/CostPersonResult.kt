package com.example.costsplit.model

/**
 * Result of expense allocation for an individual person.
 *
 * @param id Unique identifier
 * @param name Effective name displayed to user
 * @param allocationUnits Units assigned (e.g. 1 in Equal, shares in Shares mode, basis points in Percentages mode)
 * @param allocatedMinorUnits Exact allocated amount in currency minor units (e.g. cents)
 * @param effectivePercentageBasisPoints Proportion of total expense (10000 = 100.00%)
 */
data class CostPersonResult(
    val id: String,
    val name: String,
    val allocationUnits: Long,
    val allocatedMinorUnits: Long,
    val effectivePercentageBasisPoints: Int
)

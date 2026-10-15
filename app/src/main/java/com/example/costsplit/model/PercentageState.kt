package com.example.costsplit.model

/**
 * State representing percentage allocation total relative to 100% (10,000 basis points).
 */
sealed interface PercentageState {
    data object Balanced : PercentageState
    data class Remaining(val basisPointsRemaining: Int) : PercentageState
    data class Over(val basisPointsOver: Int) : PercentageState
}

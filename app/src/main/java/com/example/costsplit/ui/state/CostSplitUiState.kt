package com.example.costsplit.ui.state

import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.model.CostSplitResult
import com.example.costsplit.model.CurrencyOption
import com.example.costsplit.model.PercentageState
import com.example.costsplit.util.MoneyFormatter

/**
 * Unified UI state for the CostSplit application.
 */
data class CostSplitUiState(
    val expenseInput: String = "240.00",
    val expenseDescription: String = "",
    val allocationMethod: AllocationMethod = AllocationMethod.EQUAL,
    val customMode: CustomAllocationMode = CustomAllocationMode.SHARES,
    val selectedCurrency: CurrencyOption = CurrencyOption.USD,
    val people: List<PersonInputState> = defaultPeople(4),
    val expenseError: String? = null,
    val parsedExpenseMinorUnits: Long? = 24000L,
    val percentageState: PercentageState = PercentageState.Balanced,
    val totalPercentageBasisPoints: Int = 10000,
    val totalShares: Long = 4L,
    val isSharesValid: Boolean = true,
    val isCalculateEnabled: Boolean = true,
    val calculationErrorHint: String? = null,
    val autoPreviewEstimate: String? = "$60.00 each",
    val lastCalculationResult: CostSplitResult? = null,
    val showCustomCurrencyDialog: Boolean = false,
    val showCurrencyPickerSheet: Boolean = false,
    val pendingPresetCount: Int? = null,
    val showPresetConfirmationDialog: Boolean = false
) {
    companion object {
        fun defaultPeople(count: Int): List<PersonInputState> {
            val bpPerPerson = 10000 / count
            val remainder = 10000 % count
            return (1..count).map { i ->
                val bp = bpPerPerson + if (i <= remainder) 1 else 0
                val pctStr = MoneyFormatter.basisPointsToInputString(bp)
                PersonInputState(
                    id = "person_$i",
                    name = "",
                    shareInput = "1",
                    percentageInput = pctStr
                )
            }
        }
    }
}

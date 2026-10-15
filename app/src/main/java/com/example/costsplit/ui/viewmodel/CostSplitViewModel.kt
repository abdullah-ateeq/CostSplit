package com.example.costsplit.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.costsplit.calculator.CostSplitCalculator
import com.example.costsplit.data.CurrencyPreferences
import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.model.CostPerson
import com.example.costsplit.model.CostSplitResult
import com.example.costsplit.model.CurrencyOption
import com.example.costsplit.model.PercentageState
import com.example.costsplit.ui.state.CostSplitUiState
import com.example.costsplit.ui.state.CustomAllocationMode
import com.example.costsplit.ui.state.PersonInputState
import com.example.costsplit.util.MoneyFormatter
import com.example.costsplit.util.MoneyParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class CostSplitViewModel(application: Application) : AndroidViewModel(application) {

    private val currencyPreferences = CurrencyPreferences(application)

    private val _uiState = MutableStateFlow(
        initInitialState(currencyPreferences.getSelectedCurrency())
    )
    val uiState: StateFlow<CostSplitUiState> = _uiState.asStateFlow()

    init {
        // Run initial validation and auto preview
        _uiState.update { recomputeState(it) }
    }

    private fun initInitialState(currency: CurrencyOption): CostSplitUiState {
        val initialExpense = if (currency.decimalPlaces == 0) "240" else "240.00"
        val parsedUnits = if (currency.decimalPlaces == 0) 240L else 24000L
        return CostSplitUiState(
            expenseInput = initialExpense,
            expenseDescription = "",
            allocationMethod = AllocationMethod.EQUAL,
            customMode = CustomAllocationMode.SHARES,
            selectedCurrency = currency,
            people = CostSplitUiState.defaultPeople(4),
            parsedExpenseMinorUnits = parsedUnits
        )
    }

    fun onExpenseInputChanged(input: String) {
        if (input.length > 20) return
        _uiState.update { state ->
            recomputeState(state.copy(expenseInput = input))
        }
    }

    fun onDescriptionChanged(description: String) {
        if (description.length <= 50) {
            _uiState.update { it.copy(expenseDescription = description) }
        }
    }

    fun onAllocationMethodSelected(method: AllocationMethod) {
        _uiState.update { state ->
            val newMethod = if (method == AllocationMethod.EQUAL) {
                AllocationMethod.EQUAL
            } else {
                if (state.customMode == CustomAllocationMode.SHARES) {
                    AllocationMethod.SHARES
                } else {
                    AllocationMethod.PERCENTAGES
                }
            }
            recomputeState(state.copy(allocationMethod = newMethod))
        }
    }

    fun onCustomSubModeSelected(mode: CustomAllocationMode) {
        _uiState.update { state ->
            val method = if (mode == CustomAllocationMode.SHARES) {
                AllocationMethod.SHARES
            } else {
                AllocationMethod.PERCENTAGES
            }
            recomputeState(state.copy(customMode = mode, allocationMethod = method))
        }
    }

    fun onAddPerson() {
        val currentCount = _uiState.value.people.size
        if (currentCount >= CostSplitCalculator.MAX_PEOPLE) return

        val newId = UUID.randomUUID().toString()
        val newPerson = PersonInputState(
            id = newId,
            name = "",
            shareInput = "1",
            percentageInput = "0"
        )

        _uiState.update { state ->
            val updatedPeople = state.people + newPerson
            recomputeState(state.copy(people = updatedPeople))
        }
    }

    fun onRemovePerson(personId: String) {
        val currentCount = _uiState.value.people.size
        if (currentCount <= CostSplitCalculator.MIN_PEOPLE) return

        _uiState.update { state ->
            val updatedPeople = state.people.filterNot { it.id == personId }
            recomputeState(state.copy(people = updatedPeople))
        }
    }

    fun onPersonNameChanged(personId: String, name: String) {
        if (name.length > 50) return
        _uiState.update { state ->
            val updated = state.people.map { p ->
                if (p.id == personId) p.copy(name = name) else p
            }
            state.copy(people = updated)
        }
    }

    fun onPersonShareChanged(personId: String, shareStr: String) {
        val filtered = shareStr.filter { it.isDigit() }
        if (filtered.length > 10) return
        _uiState.update { state ->
            val updated = state.people.map { p ->
                if (p.id == personId) p.copy(shareInput = filtered) else p
            }
            recomputeState(state.copy(people = updated))
        }
    }

    fun onPersonPercentageChanged(personId: String, pctStr: String) {
        // Filter characters: digits, dot, comma
        val filtered = pctStr.filter { it.isDigit() || it == '.' || it == ',' }
        if (filtered.length > 7) return
        _uiState.update { state ->
            val updated = state.people.map { p ->
                if (p.id == personId) p.copy(percentageInput = filtered) else p
            }
            recomputeState(state.copy(people = updated))
        }
    }

    fun onBalanceTo100Percent() {
        _uiState.update { state ->
            val people = state.people
            if (people.size < 2) return@update state

            val otherSum = people.dropLast(1).sumOf { p ->
                MoneyParser.parsePercentage(p.percentageInput) ?: 0
            }

            val targetForLast = (10000 - otherSum).coerceIn(0, 10000)
            val formatted = MoneyFormatter.basisPointsToInputString(targetForLast)

            val updated = people.mapIndexed { index, person ->
                if (index == people.lastIndex) {
                    person.copy(percentageInput = formatted)
                } else {
                    person
                }
            }
            recomputeState(state.copy(people = updated))
        }
    }

    fun onQuickPresetTapped(count: Int) {
        val currentState = _uiState.value
        val hasCustomData = currentState.people.any { it.name.isNotBlank() } ||
            (currentState.customMode == CustomAllocationMode.SHARES && currentState.people.any { it.shareInput != "1" }) ||
            (currentState.customMode == CustomAllocationMode.PERCENTAGES && currentState.totalPercentageBasisPoints != 10000)

        if (hasCustomData && count != currentState.people.size) {
            _uiState.update {
                it.copy(
                    pendingPresetCount = count,
                    showPresetConfirmationDialog = true
                )
            }
        } else {
            applyPeopleCount(count)
        }
    }

    fun onConfirmPresetChange() {
        val pending = _uiState.value.pendingPresetCount ?: return
        applyPeopleCount(pending)
        _uiState.update {
            it.copy(
                pendingPresetCount = null,
                showPresetConfirmationDialog = false
            )
        }
    }

    fun onDismissPresetDialog() {
        _uiState.update {
            it.copy(
                pendingPresetCount = null,
                showPresetConfirmationDialog = false
            )
        }
    }

    private fun applyPeopleCount(count: Int) {
        if (count !in CostSplitCalculator.MIN_PEOPLE..CostSplitCalculator.MAX_PEOPLE) return
        _uiState.update { state ->
            val newPeople = CostSplitUiState.defaultPeople(count)
            recomputeState(state.copy(people = newPeople))
        }
    }

    fun onSelectCurrency(currency: CurrencyOption) {
        currencyPreferences.saveSelectedCurrency(currency)
        _uiState.update { state ->
            // Re-parse current expense input with new currency
            recomputeState(state.copy(selectedCurrency = currency, showCurrencyPickerSheet = false))
        }
    }

    fun onSaveCustomCurrency(customCurrency: CurrencyOption) {
        currencyPreferences.saveCustomCurrency(customCurrency)
        onSelectCurrency(customCurrency)
        _uiState.update { it.copy(showCustomCurrencyDialog = false) }
    }

    fun getSavedCustomCurrency(): CurrencyOption {
        return currencyPreferences.getSavedCustomCurrency()
    }

    fun setShowCurrencyPicker(show: Boolean) {
        _uiState.update { it.copy(showCurrencyPickerSheet = show) }
    }

    fun setShowCustomCurrencyDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomCurrencyDialog = show) }
    }

    fun onReset() {
        val currency = _uiState.value.selectedCurrency
        val initialExpense = if (currency.decimalPlaces == 0) "240" else "240.00"
        val parsedUnits = if (currency.decimalPlaces == 0) 240L else 24000L

        _uiState.update {
            recomputeState(
                it.copy(
                    expenseInput = initialExpense,
                    expenseDescription = "",
                    allocationMethod = AllocationMethod.EQUAL,
                    customMode = CustomAllocationMode.SHARES,
                    people = CostSplitUiState.defaultPeople(4),
                    parsedExpenseMinorUnits = parsedUnits,
                    lastCalculationResult = null
                )
            )
        }
    }

    fun calculateSplit(): CostSplitResult? {
        val state = _uiState.value
        val expenseUnits = state.parsedExpenseMinorUnits ?: return null
        val peopleCount = state.people.size
        if (peopleCount !in CostSplitCalculator.MIN_PEOPLE..CostSplitCalculator.MAX_PEOPLE) return null

        val result = try {
            when (state.allocationMethod) {
                AllocationMethod.EQUAL -> {
                    val people = state.people.mapIndexed { index, p ->
                        CostPerson(
                            id = p.id,
                            name = p.name.trim().ifEmpty { "Person ${index + 1}" },
                            share = 1L
                        )
                    }
                    CostSplitCalculator.splitEqually(
                        totalMinorUnits = expenseUnits,
                        people = people,
                        currency = state.selectedCurrency,
                        expenseDescription = state.expenseDescription
                    )
                }

                AllocationMethod.SHARES -> {
                    val people = state.people.mapIndexed { index, p ->
                        val share = MoneyParser.parseShare(p.shareInput) ?: 1L
                        CostPerson(
                            id = p.id,
                            name = p.name.trim().ifEmpty { "Person ${index + 1}" },
                            share = share
                        )
                    }
                    CostSplitCalculator.splitByShares(
                        totalMinorUnits = expenseUnits,
                        people = people,
                        currency = state.selectedCurrency,
                        expenseDescription = state.expenseDescription
                    )
                }

                AllocationMethod.PERCENTAGES -> {
                    val people = state.people.mapIndexed { index, p ->
                        CostPerson(
                            id = p.id,
                            name = p.name.trim().ifEmpty { "Person ${index + 1}" },
                            share = 1L
                        )
                    }
                    val percentageUnits = state.people.map { p ->
                        MoneyParser.parsePercentage(p.percentageInput) ?: 0
                    }
                    CostSplitCalculator.splitByPercentages(
                        totalMinorUnits = expenseUnits,
                        people = people,
                        percentageUnits = percentageUnits,
                        currency = state.selectedCurrency,
                        expenseDescription = state.expenseDescription
                    )
                }
            }
        } catch (_: Exception) {
            null
        }

        if (result != null) {
            _uiState.update { it.copy(lastCalculationResult = result) }
        }
        return result
    }

    private fun recomputeState(state: CostSplitUiState): CostSplitUiState {
        // 1. Parse expense
        val parseResult = MoneyParser.parseExpense(state.expenseInput, state.selectedCurrency)
        val (parsedUnits, expenseError) = when (parseResult) {
            is MoneyParser.ParseResult.Success -> Pair(parseResult.minorUnits, null)
            is MoneyParser.ParseResult.Error -> Pair(null, parseResult.message)
        }

        // 2. Shares validation
        var totalShares = 0L
        var sharesValid = true
        for (person in state.people) {
            val share = MoneyParser.parseShare(person.shareInput)
            if (share == null || share < 1L) {
                sharesValid = false
            } else {
                totalShares += share
            }
        }
        if (totalShares <= 0L) sharesValid = false

        // 3. Percentages validation
        var totalBp = 0
        var allPercentagesValid = true
        for (person in state.people) {
            val bp = MoneyParser.parsePercentage(person.percentageInput)
            if (bp == null || bp < 0 || bp > 10000) {
                allPercentagesValid = false
            } else {
                totalBp += bp
            }
        }

        val percentageState = when {
            totalBp == 10000 && allPercentagesValid -> PercentageState.Balanced
            totalBp < 10000 -> PercentageState.Remaining(10000 - totalBp)
            else -> PercentageState.Over(totalBp - 10000)
        }

        // 4. Calculate Split button eligibility & hint
        var isCalculateEnabled = false
        var calculationErrorHint: String? = null

        if (expenseError != null) {
            calculationErrorHint = expenseError
        } else if (state.people.size !in CostSplitCalculator.MIN_PEOPLE..CostSplitCalculator.MAX_PEOPLE) {
            calculationErrorHint = "At least 2 people required."
        } else {
            when (state.allocationMethod) {
                AllocationMethod.EQUAL -> {
                    isCalculateEnabled = true
                }
                AllocationMethod.SHARES -> {
                    if (sharesValid) {
                        isCalculateEnabled = true
                    } else {
                        calculationErrorHint = "All shares must be whole numbers greater than zero."
                    }
                }
                AllocationMethod.PERCENTAGES -> {
                    if (percentageState is PercentageState.Balanced && allPercentagesValid) {
                        isCalculateEnabled = true
                    } else if (percentageState is PercentageState.Remaining) {
                        calculationErrorHint = "Percentages total ${MoneyFormatter.formatBasisPoints(totalBp)} (${MoneyFormatter.formatBasisPoints(10000 - totalBp)} remaining)"
                    } else {
                        calculationErrorHint = "Percentages total ${MoneyFormatter.formatBasisPoints(totalBp)} (${MoneyFormatter.formatBasisPoints(totalBp - 10000)} over 100%)"
                    }
                }
            }
        }

        // 5. Auto preview estimation (only when straightforward)
        val autoPreviewEstimate = if (parsedUnits != null && state.people.size in CostSplitCalculator.MIN_PEOPLE..CostSplitCalculator.MAX_PEOPLE) {
            when (state.allocationMethod) {
                AllocationMethod.EQUAL -> {
                    val count = state.people.size
                    val base = parsedUnits / count
                    "${MoneyFormatter.format(base, state.selectedCurrency)} each"
                }
                AllocationMethod.SHARES -> {
                    if (sharesValid && state.people.all { it.shareInput == "1" }) {
                        val count = state.people.size
                        val base = parsedUnits / count
                        "${MoneyFormatter.format(base, state.selectedCurrency)} each"
                    } else null
                }
                AllocationMethod.PERCENTAGES -> null
            }
        } else null

        return state.copy(
            parsedExpenseMinorUnits = parsedUnits,
            expenseError = expenseError,
            totalShares = totalShares,
            isSharesValid = sharesValid,
            totalPercentageBasisPoints = totalBp,
            percentageState = percentageState,
            isCalculateEnabled = isCalculateEnabled,
            calculationErrorHint = calculationErrorHint,
            autoPreviewEstimate = autoPreviewEstimate
        )
    }
}

package com.example.costsplit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.costsplit.calculator.CostSplitCalculator
import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.model.PercentageState
import com.example.costsplit.ui.components.CurrencySelectionDialog
import com.example.costsplit.ui.components.CustomCurrencyConfigDialog
import com.example.costsplit.ui.components.ExpenseFlowCard
import com.example.costsplit.ui.components.PeoplePresetChips
import com.example.costsplit.ui.components.PersonRowItem
import com.example.costsplit.ui.state.CustomAllocationMode
import com.example.costsplit.ui.theme.StatusError
import com.example.costsplit.ui.theme.StatusSuccess
import com.example.costsplit.ui.theme.StatusWarning
import com.example.costsplit.ui.viewmodel.CostSplitViewModel
import com.example.costsplit.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    viewModel: CostSplitViewModel,
    onNavigateToResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CostSplit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Divide a total expense across multiple people.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Currency selector chip
                    SuggestionChip(
                        onClick = { viewModel.setShowCurrencyPicker(true) },
                        label = {
                            Text(
                                text = "${state.selectedCurrency.code} (${state.selectedCurrency.symbol})",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Currency",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Reset button
                    IconButton(onClick = { viewModel.onReset() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Primary Calculate Bar
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (state.calculationErrorHint != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.calculationErrorHint ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val result = viewModel.calculateSplit()
                            if (result != null) {
                                onNavigateToResult()
                            }
                        },
                        enabled = state.isCalculateEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "CALCULATE SPLIT",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. EXPENSE INPUT CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "TOTAL EXPENSE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.expenseInput,
                            onValueChange = { viewModel.onExpenseInputChanged(it) },
                            prefix = {
                                Text(
                                    text = state.selectedCurrency.symbol + " ",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            isError = state.expenseError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state.expenseError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.expenseError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Description input (Optional, max 50 chars)
                        OutlinedTextField(
                            value = state.expenseDescription,
                            onValueChange = { viewModel.onDescriptionChanged(it) },
                            label = { Text("Description (Optional)") },
                            placeholder = { Text("e.g. Travel, Hotel, Office supplies") },
                            singleLine = true,
                            supportingText = {
                                Text(
                                    text = "${state.expenseDescription.length}/50",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. ALLOCATION MODE CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ALLOCATION MODE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Primary Mode Selection: EQUAL vs CUSTOM
                        val isCustomSelected = state.allocationMethod != AllocationMethod.EQUAL
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !isCustomSelected,
                                onClick = { viewModel.onAllocationMethodSelected(AllocationMethod.EQUAL) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("EQUAL", fontWeight = FontWeight.Bold)
                            }
                            SegmentedButton(
                                selected = isCustomSelected,
                                onClick = { viewModel.onAllocationMethodSelected(AllocationMethod.SHARES) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("CUSTOM", fontWeight = FontWeight.Bold)
                            }
                        }

                        // If Custom: SHARES vs PERCENTAGES
                        if (isCustomSelected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "CUSTOM METHOD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = state.customMode == CustomAllocationMode.SHARES,
                                    onClick = { viewModel.onCustomSubModeSelected(CustomAllocationMode.SHARES) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("SHARES", fontWeight = FontWeight.SemiBold)
                                }
                                SegmentedButton(
                                    selected = state.customMode == CustomAllocationMode.PERCENTAGES,
                                    onClick = { viewModel.onCustomSubModeSelected(CustomAllocationMode.PERCENTAGES) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("PERCENTAGES", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. PEOPLE PRESETS & HEADER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PEOPLE (${state.people.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Min ${CostSplitCalculator.MIN_PEOPLE} • Max ${CostSplitCalculator.MAX_PEOPLE}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick presets chips
                        PeoplePresetChips(
                            currentCount = state.people.size,
                            onPresetSelected = { count ->
                                viewModel.onQuickPresetTapped(count)
                            }
                        )

                        // Mode status row
                        when (state.allocationMethod) {
                            AllocationMethod.EQUAL -> {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Each person contributes an equal share.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AllocationMethod.SHARES -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TOTAL SHARES: ${state.totalShares}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Whole numbers ≥ 1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            AllocationMethod.PERCENTAGES -> {
                                Spacer(modifier = Modifier.height(10.dp))
                                val pctTotalStr = MoneyFormatter.formatBasisPoints(state.totalPercentageBasisPoints)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (val pState = state.percentageState) {
                                        is PercentageState.Balanced -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(StatusSuccess)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "TOTAL: $pctTotalStr (BALANCED)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusSuccess
                                                )
                                            }
                                        }
                                        is PercentageState.Remaining -> {
                                            val remainingStr = MoneyFormatter.formatBasisPoints(pState.basisPointsRemaining)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(StatusWarning)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "TOTAL: $pctTotalStr ($remainingStr REMAINING)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusWarning
                                                )
                                            }
                                        }
                                        is PercentageState.Over -> {
                                            val overStr = MoneyFormatter.formatBasisPoints(pState.basisPointsOver)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(StatusError)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "TOTAL: $pctTotalStr ($overStr OVER)",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusError
                                                )
                                            }
                                        }
                                    }

                                    // "BALANCE TO 100%" Button
                                    if (state.percentageState is PercentageState.Remaining) {
                                        FilledTonalButton(
                                            onClick = { viewModel.onBalanceTo100Percent() },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text(
                                                text = "BALANCE TO 100%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. PEOPLE LIST ROWS
            itemsIndexed(state.people, key = { _, person -> person.id }) { index, person ->
                PersonRowItem(
                    index = index,
                    person = person,
                    allocationMethod = state.allocationMethod,
                    customMode = state.customMode,
                    isDeleteDisabled = state.people.size <= CostSplitCalculator.MIN_PEOPLE,
                    onNameChange = { viewModel.onPersonNameChanged(person.id, it) },
                    onShareChange = { viewModel.onPersonShareChanged(person.id, it) },
                    onPercentageChange = { viewModel.onPersonPercentageChanged(person.id, it) },
                    onDelete = { viewModel.onRemovePerson(person.id) }
                )
            }

            // ADD PERSON BUTTON
            item {
                OutlinedButton(
                    onClick = { viewModel.onAddPerson() },
                    enabled = state.people.size < CostSplitCalculator.MAX_PEOPLE,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+ ADD PERSON",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5. AUTO PREVIEW CARD (When straightforward)
            if (state.autoPreviewEstimate != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "PREVIEW ESTIMATE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.autoPreviewEstimate ?: "",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${state.people.size} people",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // 6. EXPENSE FLOW PIPELINE CARD
            item {
                val totalFormatted = state.parsedExpenseMinorUnits?.let {
                    MoneyFormatter.format(it, state.selectedCurrency)
                } ?: "${state.selectedCurrency.symbol}${state.expenseInput}"

                val methodLabel = when (state.allocationMethod) {
                    AllocationMethod.EQUAL -> "Equal Split"
                    AllocationMethod.SHARES -> "${state.totalShares} Shares"
                    AllocationMethod.PERCENTAGES -> "100% Split"
                }

                val outcomeSummary = state.autoPreviewEstimate ?: "Custom Allocation"

                ExpenseFlowCard(
                    totalFormatted = totalFormatted,
                    peopleCount = state.people.size,
                    methodLabel = methodLabel,
                    outcomeSummary = outcomeSummary
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Confirmation Dialog for Preset change
    if (state.showPresetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissPresetDialog() },
            title = {
                Text(
                    text = "Change Number of People?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Selecting a preset will reset custom names and allocation weights for the group.")
            },
            confirmButton = {
                Button(onClick = { viewModel.onConfirmPresetChange() }) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissPresetDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Currency Selection Dialog
    if (state.showCurrencyPickerSheet) {
        CurrencySelectionDialog(
            selectedCurrency = state.selectedCurrency,
            onSelectCurrency = { viewModel.onSelectCurrency(it) },
            onDismiss = { viewModel.setShowCurrencyPicker(false) },
            onOpenCustomConfig = {
                viewModel.setShowCurrencyPicker(false)
                viewModel.setShowCustomCurrencyDialog(true)
            }
        )
    }

    // Custom Currency Config Dialog
    if (state.showCustomCurrencyDialog) {
        CustomCurrencyConfigDialog(
            initialCurrency = viewModel.getSavedCustomCurrency(),
            onSave = { viewModel.onSaveCustomCurrency(it) },
            onDismiss = { viewModel.setShowCustomCurrencyDialog(false) }
        )
    }
}

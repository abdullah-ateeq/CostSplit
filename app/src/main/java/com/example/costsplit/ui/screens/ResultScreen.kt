package com.example.costsplit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.model.CostSplitResult
import com.example.costsplit.ui.components.AllocationBar
import com.example.costsplit.ui.components.ExpenseFlowCard
import com.example.costsplit.ui.theme.SegmentPalette
import com.example.costsplit.ui.viewmodel.CostSplitViewModel
import com.example.costsplit.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: CostSplitViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val result: CostSplitResult? = state.lastCalculationResult
    val context = LocalContext.current

    if (result == null) {
        // Fallback if accessed without result
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No calculation result available.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    val currency = result.currency
    val totalExpenseFormatted = MoneyFormatter.format(result.totalMinorUnits, currency)
    val totalAllocatedFormatted = MoneyFormatter.format(result.totalAllocatedMinorUnits, currency)
    val remainingFormatted = MoneyFormatter.format(result.remainingMinorUnits, currency)

    val methodLabel = when (result.method) {
        AllocationMethod.EQUAL -> "Equal Split"
        AllocationMethod.SHARES -> "Custom Shares"
        AllocationMethod.PERCENTAGES -> "Custom Percentages"
    }

    val summaryText = buildString {
        appendLine("CostSplit: ${result.expenseDescription}")
        appendLine("Total Expense: $totalExpenseFormatted")
        appendLine("Method: $methodLabel (${result.people.size} people)")
        appendLine()
        result.people.forEach { person ->
            val pct = MoneyFormatter.formatBasisPoints(person.effectivePercentageBasisPoints)
            val amt = MoneyFormatter.format(person.allocatedMinorUnits, currency)
            val unitDetail = when (result.method) {
                AllocationMethod.SHARES -> " (${person.allocationUnits} shares, $pct)"
                AllocationMethod.PERCENTAGES -> " ($pct)"
                AllocationMethod.EQUAL -> " ($pct)"
            }
            appendLine("• ${person.name}: $amt$unitDetail")
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Allocation Result",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Copy to clipboard
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("CostSplit Summary", summaryText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Summary"
                        )
                    }

                    // Share intent
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share CostSplit breakdown")
                            context.startActivity(shareIntent)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Split", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share CostSplit breakdown"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
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

            // 1. PRIMARY RESULT HERO CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = result.expenseDescription.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (result.method == AllocationMethod.EQUAL) {
                            // "$60.00 EACH" highlight
                            val amountEach = MoneyFormatter.format(result.people.first().allocatedMinorUnits, currency)
                            Text(
                                text = "$amountEach EACH",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            // Custom split highlight
                            Text(
                                text = totalExpenseFormatted,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Total Expense: $totalExpenseFormatted • ${result.people.size} People • $methodLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // 2. ROUNDING NOTICE (Secondary alert if rounding occurred)
            if (result.hasRoundingAdjustment) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Rounded so the total remains exactly $totalExpenseFormatted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. PROPORTIONAL ALLOCATION BAR
            item {
                AllocationBar(
                    people = result.people,
                    currency = currency
                )
            }

            // 4. RESULT SUMMARY CARD
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RESULT SUMMARY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        SummaryRow("TOTAL EXPENSE", totalExpenseFormatted)
                        SummaryRow("PEOPLE", "${result.people.size}")
                        SummaryRow("METHOD", methodLabel)
                        SummaryRow("ALLOCATED", totalAllocatedFormatted)
                        SummaryRow("REMAINING", remainingFormatted)

                        when (result.method) {
                            AllocationMethod.SHARES -> {
                                val totalShares = result.people.sumOf { it.allocationUnits }
                                SummaryRow("TOTAL SHARES", "$totalShares")
                            }
                            AllocationMethod.PERCENTAGES -> {
                                SummaryRow("TOTAL", "100%")
                            }
                            AllocationMethod.EQUAL -> {}
                        }
                    }
                }
            }

            // 5. INDIVIDUAL BREAKDOWN LIST
            item {
                Text(
                    text = "INDIVIDUAL AMOUNTS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(result.people, key = { _, person -> person.id }) { index, person ->
                val avatarColor = SegmentPalette[index % SegmentPalette.size]
                val formattedAmount = MoneyFormatter.format(person.allocatedMinorUnits, currency)
                val effectivePct = MoneyFormatter.formatBasisPoints(person.effectivePercentageBasisPoints)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Name & allocation units
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val unitDescription = when (result.method) {
                                AllocationMethod.EQUAL -> "Equal share"
                                AllocationMethod.SHARES -> "${person.allocationUnits} ${if (person.allocationUnits == 1L) "share" else "shares"}"
                                AllocationMethod.PERCENTAGES -> MoneyFormatter.formatBasisPoints(person.allocationUnits.toInt())
                            }
                            Text(
                                text = unitDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Amount and Effective % badge
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formattedAmount,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = effectivePct,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. EXPENSE FLOW PIPELINE IN RESULT
            item {
                val flowOutcome = if (result.method == AllocationMethod.EQUAL) {
                    "${MoneyFormatter.format(result.people.first().allocatedMinorUnits, currency)} each"
                } else {
                    result.people.take(3).joinToString(" / ") {
                        MoneyFormatter.format(it.allocatedMinorUnits, currency)
                    } + if (result.people.size > 3) "..." else ""
                }

                val methodSummary = when (result.method) {
                    AllocationMethod.EQUAL -> "Equal Split"
                    AllocationMethod.SHARES -> result.people.take(3).joinToString(" / ") { "${it.allocationUnits}" } + " Shares"
                    AllocationMethod.PERCENTAGES -> "100% Custom"
                }

                ExpenseFlowCard(
                    totalFormatted = totalExpenseFormatted,
                    peopleCount = result.people.size,
                    methodLabel = methodSummary,
                    outcomeSummary = flowOutcome
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

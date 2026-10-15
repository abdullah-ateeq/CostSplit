package com.example.costsplit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.costsplit.model.AllocationMethod
import com.example.costsplit.ui.state.CustomAllocationMode
import com.example.costsplit.ui.state.PersonInputState
import com.example.costsplit.ui.theme.SegmentPalette

@Composable
fun PersonRowItem(
    index: Int,
    person: PersonInputState,
    allocationMethod: AllocationMethod,
    customMode: CustomAllocationMode,
    isDeleteDisabled: Boolean,
    onNameChange: (String) -> Unit,
    onShareChange: (String) -> Unit,
    onPercentageChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarColor = SegmentPalette[index % SegmentPalette.size]

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Person index avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Person Name input
            OutlinedTextField(
                value = person.name,
                onValueChange = onNameChange,
                placeholder = { Text("Person ${index + 1}") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mode-dependent allocation inputs
            when (allocationMethod) {
                AllocationMethod.EQUAL -> {
                    // In Equal mode, allocation is automatic 1 share
                    Text(
                        text = "Equal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                AllocationMethod.SHARES -> {
                    // Stepper / numeric input for Shares
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val currentShare = person.shareInput.toLongOrNull() ?: 1L
                        IconButton(
                            onClick = {
                                if (currentShare > 1) {
                                    onShareChange((currentShare - 1).toString())
                                }
                            },
                            enabled = currentShare > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease share",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = person.shareInput,
                            onValueChange = onShareChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.width(52.dp)
                        )

                        IconButton(
                            onClick = {
                                onShareChange((currentShare + 1).toString())
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase share",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                AllocationMethod.PERCENTAGES -> {
                    // Percentage input
                    OutlinedTextField(
                        value = person.percentageInput,
                        onValueChange = onPercentageChange,
                        trailingIcon = { Text("%", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
                        modifier = Modifier.width(88.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete button (disabled if <= 2 people)
            IconButton(
                onClick = onDelete,
                enabled = !isDeleteDisabled,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove person"
                )
            }
        }
    }
}

package com.example.edi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edi.model.EdiDelimiters
import com.example.edi.model.EdiParseResult
import com.example.edi.model.EdiSamples

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdiEditorTab(
    rawText: String,
    onRawTextChange: (String) -> Unit,
    onValidate: () -> Unit,
    onFormat: () -> Unit,
    onClear: () -> Unit,
    onLoadSample: (String) -> Unit,
    selectedSampleId: String?,
    parseResult: EdiParseResult?,
    modifier: Modifier = Modifier
) {
    var showSampleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Sample Selector Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sample_selector_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Templates",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "004010 Sample Templates",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { showSampleDialog = true },
                        modifier = Modifier.testTag("view_all_samples_button")
                    ) {
                        Text("All Templates (${EdiSamples.SAMPLES.size})")
                    }
                }

                // Horizontal scroll of quick template chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EdiSamples.SAMPLES.forEach { sample ->
                        val isSelected = sample.id == selectedSampleId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLoadSample(sample.id) },
                            label = {
                                Text(
                                    text = if (sample.isExpectedValid) sample.transactionCode else "! ${sample.name.split(" - ").getOrNull(1) ?: sample.transactionCode}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                if (!sample.isExpectedValid) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Invalid Test",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Valid",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Delimiters Bar
        val delimiters = parseResult?.delimiters ?: EdiDelimiters()
        EdiDelimitersBar(
            delimiters = delimiters,
            segmentCount = parseResult?.summary?.get("total_segments") as? Int ?: 0
        )

        // Raw EDI Editor with Monospace text
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("edi_editor_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Raw ANSI ASC X12 EDI Text",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onFormat,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("format_edi_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.WrapText,
                                contentDescription = "Format & Break Segments",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = onClear,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("clear_edi_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Editor",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                OutlinedTextField(
                    value = rawText,
                    onValueChange = onRawTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("raw_edi_text_field"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    placeholder = {
                        Text(
                            text = "Paste or type raw EDI text here (e.g., ISA*00*...~GS*PO*...~ST*850*...)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }
        }

        // Bottom Primary Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onFormat,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("format_action_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Format Segments")
            }

            Button(
                onClick = onValidate,
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("validate_edi_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Validate & Parse", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Sample Selection Dialog
    if (showSampleDialog) {
        AlertDialog(
            onDismissRequest = { showSampleDialog = false },
            title = {
                Text(
                    text = "Select 004010 EDI Template",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Supported 004010 Transaction Sets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    EdiSamples.SAMPLES.forEach { sample ->
                        Surface(
                            onClick = {
                                onLoadSample(sample.id)
                                showSampleDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (sample.id == selectedSampleId) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (sample.isExpectedValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (sample.isExpectedValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sample.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = sample.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSampleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

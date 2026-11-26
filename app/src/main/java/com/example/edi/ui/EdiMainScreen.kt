package com.example.edi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.edi.ui.components.*
import com.example.ui.theme.EdiError
import com.example.ui.theme.EdiSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdiMainScreen(
    viewModel: EdiViewModel = viewModel()
) {
    val rawText by viewModel.rawEdiText.collectAsStateWithLifecycle()
    val parseResult by viewModel.parseResult.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedSampleId by viewModel.selectedSampleId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val tabItems = listOf(
        TabItem("Editor", Icons.Outlined.Edit, Icons.Default.Edit),
        TabItem("Validation", Icons.AutoMirrored.Outlined.FactCheck, Icons.AutoMirrored.Filled.FactCheck),
        TabItem("Semantics", Icons.Outlined.AutoGraph, Icons.Default.AutoGraph),
        TabItem("JSON Output", Icons.Outlined.Code, Icons.Default.Code),
        TabItem("Segments", Icons.Outlined.AccountTree, Icons.Default.AccountTree)
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edi_main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = "EDI X12 Validator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "004010",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "ANSI ASC X12 Syntax, Envelope & Extraction Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.formatEdiText() },
                        modifier = Modifier.testTag("top_bar_format_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.WrapText,
                            contentDescription = "Format",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearEditor() },
                        modifier = Modifier.testTag("top_bar_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
                modifier = Modifier.testTag("edi_tab_row")
            ) {
                tabItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setSelectedTab(index) },
                        modifier = Modifier.testTag("tab_${item.title.lowercase().replace(" ", "_")}"),
                        text = {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )

                                // Validation error/pass badge
                                if (item.title == "Validation" && parseResult != null) {
                                    val errCount = parseResult?.validationErrors?.count { it.severity == "ERROR" } ?: 0
                                    Surface(
                                        color = if (errCount > 0) EdiError else EdiSuccess,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (errCount > 0) "$errCount" else "✓",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> EdiEditorTab(
                        rawText = rawText,
                        onRawTextChange = { viewModel.updateRawEdiText(it) },
                        onValidate = {
                            viewModel.validateAndParse()
                            viewModel.setSelectedTab(1) // Navigate to Validation tab
                        },
                        onFormat = { viewModel.formatEdiText() },
                        onClear = { viewModel.clearEditor() },
                        onLoadSample = { viewModel.loadSample(it) },
                        selectedSampleId = selectedSampleId,
                        parseResult = parseResult
                    )

                    1 -> EdiValidationTab(
                        parseResult = parseResult,
                        onSwitchToEditor = { viewModel.setSelectedTab(0) }
                    )

                    2 -> EdiSemanticsTab(
                        parseResult = parseResult,
                        onSwitchToEditor = { viewModel.setSelectedTab(0) }
                    )

                    3 -> EdiJsonOutputTab(
                        parseResult = parseResult,
                        onShowSnackbar = { viewModel.setSnackbarMessage(it) }
                    )

                    4 -> EdiSegmentTreeTab(
                        parseResult = parseResult,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) }
                    )
                }
            }
        }
    }
}

private data class TabItem(
    val title: String,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

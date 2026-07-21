package com.sherif.ledger.feature.debug.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SmartToy
import com.sherif.ledger.core.common.diagnostics.PipelineEvent
import com.sherif.ledger.core.common.diagnostics.StageStatus
import com.sherif.ledger.core.designsystem.component.LedgerTopBar
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.debug.presentation.viewmodel.DatabaseSummary
import com.sherif.ledger.feature.debug.presentation.viewmodel.DebugAction
import com.sherif.ledger.feature.debug.presentation.viewmodel.DebugUiState

@Composable
fun DebugConsoleScreen(
    state: DebugUiState,
    dbSummary: DatabaseSummary,
    onAction: (DebugAction) -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToLedgerDiagnostics: () -> Unit,
    onNavigateToBalanceInspector: () -> Unit,
    onNavigateToAiMetrics: () -> Unit,
    onNavigateToAiDebug: () -> Unit,
    onNavigateToIntelligenceInspector: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Injection", "Pipeline", "Database", "Logs", "Fixtures")

    Scaffold(
        topBar = {
            Column(modifier = Modifier.statusBarsPadding()) {
                LedgerTopBar(
                    title = "Developer Console",
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LedgerTheme.colors.label,
                            modifier = Modifier
                                .size(LedgerTheme.iconSize.Medium)
                                .ledgerClickable { onBackClick() },
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToDiagnostics) {
                            Icon(
                                imageVector = Icons.Filled.BugReport,
                                contentDescription = "Pipeline Diagnostics",
                                tint = LedgerTheme.colors.tint
                            )
                        }
                        IconButton(onClick = onNavigateToLedgerDiagnostics) {
                            Icon(
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = "Ledger Diagnostics",
                                tint = LedgerTheme.colors.tint
                            )
                        }
                        IconButton(onClick = onNavigateToBalanceInspector) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = "Balance Inspector",
                                tint = LedgerTheme.colors.tint
                            )
                        }
                        IconButton(onClick = onNavigateToAiMetrics) {
                            Icon(
                                imageVector = Icons.Filled.Insights,
                                contentDescription = "AI Metrics",
                                tint = LedgerTheme.colors.tint
                            )
                        }
                        IconButton(onClick = onNavigateToAiDebug) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = "AI Debug",
                                tint = LedgerTheme.colors.tint
                            )
                        }
                        IconButton(onClick = onNavigateToIntelligenceInspector) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = "Intelligence Inspector",
                                tint = LedgerTheme.colors.tint
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = LedgerSpacing.Screen)
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = LedgerTheme.colors.tint,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, style = LedgerTextStyles.Caption) }
                        )
                    }
                }
            }
        },
        containerColor = LedgerTheme.colors.surfaceLevel0
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> InjectionTab(state, onAction)
                1 -> PipelineTab(state.pipelineEvents)
                2 -> DatabaseTab(state, dbSummary, onAction)
                3 -> LogsTab(state.logs)
                4 -> FixturesTab(state, onAction)
            }
        }
    }
}

@Composable
private fun InjectionTab(state: DebugUiState, onAction: (DebugAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = LedgerSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large)
    ) {
        item {
            Spacer(Modifier.height(LedgerSpacing.Medium))
            SectionTitle("Notification Details")
            
            OutlinedTextField(
                value = state.packageName,
                onValueChange = { onAction(DebugAction.UpdatePackage(it)) },
                label = { Text("Package Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            
            Spacer(Modifier.height(LedgerSpacing.Small))
            
            OutlinedTextField(
                value = state.title,
                onValueChange = { onAction(DebugAction.UpdateTitle(it)) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )
            
            Spacer(Modifier.height(LedgerSpacing.Small))
            
            OutlinedTextField(
                value = state.text,
                onValueChange = { onAction(DebugAction.UpdateText(it)) },
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = textFieldColors()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
            ) {
                Button(
                    onClick = { onAction(DebugAction.InjectOnce) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.success)
                ) {
                    Text("Inject Once")
                }
                
                Button(
                    onClick = { onAction(DebugAction.InjectRandom) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.tint)
                ) {
                    Text("Random")
                }
            }
        }

        item {
            SectionTitle("Batch Injection")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
            ) {
                OutlinedButton(
                    onClick = { onAction(DebugAction.InjectMultiple(10)) },
                    modifier = Modifier.weight(1f)
                ) { Text("x10") }
                OutlinedButton(
                    onClick = { onAction(DebugAction.InjectMultiple(100)) },
                    modifier = Modifier.weight(1f)
                ) { Text("x100") }
            }
            Spacer(Modifier.height(LedgerSpacing.Massive))
        }
    }
}

@Composable
private fun PipelineTab(events: List<PipelineEvent>) {
    if (events.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pipeline activity yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = LedgerSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)
        ) {
            item { Spacer(Modifier.height(LedgerSpacing.Medium)) }
            items(events.reversed()) { event ->
                PipelineEventItem(event)
            }
        }
    }
}

@Composable
private fun PipelineEventItem(event: PipelineEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.stage.name,
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.label
            )
            val details = when (val status = event.status) {
                is StageStatus.SuccessWithDetails -> status.details
                is StageStatus.Failed -> "FAILED: ${status.reason}"
                is StageStatus.Ignored -> "Ignored"
                StageStatus.Success -> "Success"
            }
            Text(details, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
        }
        
        val color = when (event.status) {
            is StageStatus.Success, is StageStatus.SuccessWithDetails -> LedgerTheme.colors.success
            is StageStatus.Failed -> LedgerTheme.colors.expense
            is StageStatus.Ignored -> Color.Gray
        }
        
        Box(Modifier.size(8.dp).background(color, shape = CircleShape))
    }
}

@Composable
private fun DatabaseTab(state: DebugUiState, summary: DatabaseSummary, onAction: (DebugAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(LedgerSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large)
    ) {
        item { SectionTitle("Database Statistics") }
        
        item {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                DiagnosticRow("Total Transactions", summary.totalTransactions.toString())
                DiagnosticRow("Total Accounts", summary.totalAccounts.toString())
                DiagnosticRow("Last Activity", summary.lastInsertTime?.let { java.util.Date(it).toString() } ?: "Never")
            }
        }

        state.lastTransaction?.let { txn ->
            item { SectionTitle("Last Transaction") }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ledgerSurface(level = LedgerSurfaceLevel.Level1)
                        .padding(LedgerSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
                ) {
                    DiagnosticRow("ID", txn.id.toString())
                    DiagnosticRow("Merchant", txn.rawText ?: "Unknown")
                    DiagnosticRow("Amount", "${txn.amount.currencyCode} ${txn.amount.minorUnits}")
                    DiagnosticRow("Type", txn.type.name)
                    DiagnosticRow("Source", txn.source.name)
                    DiagnosticRow("Fingerprint", txn.fingerprint)
                }
            }
        }
        
        item {
            Spacer(Modifier.height(LedgerSpacing.Large))
            Button(
                onClick = { onAction(DebugAction.ClearDatabase) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.expense)
            ) {
                Text("Clear All Tables")
            }
        }
    }
}

@Composable
private fun LogsTab(logs: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = LedgerSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { Spacer(Modifier.height(LedgerSpacing.Medium)) }
        items(logs) { log ->
            Text(
                text = log,
                style = LedgerTextStyles.Caption.copy(
                    fontSize = 10.sp, 
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp
                ),
                color = if ("ERROR" in log) LedgerTheme.colors.expense else Color.Green
            )
        }
    }
}

@Composable
private fun FixturesTab(state: DebugUiState, onAction: (DebugAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = LedgerSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
    ) {
        item { 
            Spacer(Modifier.height(LedgerSpacing.Medium))
            SectionTitle("Available Fixtures") 
        }
        items(state.fixtures) { fixture ->
            FixtureItem(
                fixture = fixture,
                isSelected = state.selectedFixtureId == fixture.id,
                onClick = { onAction(DebugAction.SelectFixture(fixture.id)) }
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = LedgerTextStyles.Caption.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = LedgerTheme.colors.tertiaryLabel,
        modifier = Modifier.padding(vertical = LedgerSpacing.Small)
    )
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color = Color.White.copy(alpha = 0.7f)) {
    Column {
        Text(label, style = LedgerTextStyles.Caption.copy(fontSize = 10.sp), color = Color.Gray)
        Text(value, style = LedgerTextStyles.Caption, color = valueColor)
    }
}

@Composable
private fun FixtureItem(
    fixture: com.sherif.ledger.feature.capture.parsing.validation.ParserFixture,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) LedgerTheme.colors.surfaceLevel2 else LedgerTheme.colors.surfaceLevel1,
        shape = LedgerTheme.radius.Medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(LedgerSpacing.Medium)) {
            Text(fixture.id, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
            Text(fixture.raw, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel, maxLines = 2)
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LedgerTheme.colors.tint,
    unfocusedBorderColor = LedgerTheme.colors.surfaceLevel2,
    focusedLabelColor = LedgerTheme.colors.tint,
    unfocusedLabelColor = LedgerTheme.colors.tertiaryLabel,
    cursorColor = LedgerTheme.colors.tint,
    focusedTextColor = LedgerTheme.colors.label,
    unfocusedTextColor = LedgerTheme.colors.label
)




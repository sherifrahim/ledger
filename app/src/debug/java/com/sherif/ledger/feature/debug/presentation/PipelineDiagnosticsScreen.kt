package com.sherif.ledger.feature.debug.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.common.diagnostics.PipelineEvent
import com.sherif.ledger.core.common.diagnostics.PipelineStage
import com.sherif.ledger.core.common.diagnostics.StageStatus
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.debug.presentation.viewmodel.DebugConsoleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineDiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: DebugConsoleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dbSummary by viewModel.dbSummary.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pipeline Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LedgerSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)
        ) {
            item {
                SectionHeader("Live Database Stats")
                Card(
                    colors = CardDefaults.cardColors(containerColor = LedgerTheme.colors.surfaceLevel1)
                ) {
                    Column(Modifier.padding(LedgerSpacing.Medium)) {
                        StatRow("Total Accounts", dbSummary.totalAccounts.toString())
                        StatRow("Total Transactions", dbSummary.totalTransactions.toString())
                        StatRow("Last Insert", dbSummary.lastInsertTime?.let { formatTime(it) } ?: "Never")
                    }
                }
            }

            item {
                SectionHeader("Recent Trace Activity")
            }

            items(uiState.pipelineEvents.reversed()) { event ->
                PipelineEventItem(event)
            }

            if (uiState.pipelineEvents.isEmpty()) {
                item {
                    Text(
                        "No events tracked yet. Inject a notification to see activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LedgerTheme.colors.tertiaryLabel,
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = LedgerTheme.colors.tertiaryLabel,
        modifier = Modifier.padding(top = LedgerSpacing.Large, bottom = LedgerSpacing.Small)
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = LedgerTheme.colors.secondaryLabel)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = LedgerTheme.colors.label)
    }
}

@Composable
private fun PipelineEventItem(event: PipelineEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.status) {
                is StageStatus.Failed -> Color(0xFF450A0A)
                StageStatus.Ignored -> Color(0xFF1F2937)
                else -> LedgerTheme.colors.surfaceLevel1
            }
        )
    ) {
        Column(Modifier.padding(LedgerSpacing.Medium)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.stage.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (event.status) {
                        is StageStatus.Failed -> Color.Red
                        StageStatus.Ignored -> Color.Gray
                        else -> LedgerTheme.colors.tint
                    }
                )
                Text(
                    text = formatTime(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = LedgerTheme.colors.tertiaryLabel
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            Text(
                text = "Trace: ${event.traceId ?: "None"}",
                style = MaterialTheme.typography.bodySmall,
                color = LedgerTheme.colors.tertiaryLabel,
                fontSize = 10.sp
            )

            val details = when (val s = event.status) {
                is StageStatus.SuccessWithDetails -> s.details
                is StageStatus.Failed -> s.reason
                StageStatus.Ignored -> "Ignored by logic"
                StageStatus.Success -> "Success"
            }

            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = LedgerTheme.colors.label
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}

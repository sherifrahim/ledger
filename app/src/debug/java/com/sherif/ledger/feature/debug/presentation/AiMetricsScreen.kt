package com.sherif.ledger.feature.debug.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.debug.presentation.viewmodel.AiMetricsViewModel

/** RC6 — "Display inside Developer Console." All-time aggregates, not the AI Settings screen's "today" cost summary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMetricsScreen(
    onBackClick: () -> Unit,
    viewModel: AiMetricsViewModel = hiltViewModel(),
) {
    val metrics by viewModel.metrics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Metrics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0,
    ) { padding ->
        val current = metrics
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                CircularProgressIndicator(color = LedgerTheme.colors.tint)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = LedgerSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
        ) {
            item { Spacer(Modifier.height(LedgerSpacing.Medium)) }

            item {
                Column(modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium)) {
                    MetricRow("Total Requests", current.totalRequests.toString())
                    MetricRow("Success Rate", current.successRatePercent?.let { "$it%" } ?: "No data yet")
                    MetricRow("Failure Rate", current.failureRatePercent?.let { "$it%" } ?: "No data yet")
                    MetricRow("Average Latency", "${current.averageLatencyMs}ms")
                    MetricRow("Average Tokens", current.averageTokens.toString())
                    MetricRow("Cache Hit Rate", current.cacheHitRatePercent?.let { "$it%" } ?: "No data yet")
                }
            }

            item { SectionTitle("Capability Usage") }
            if (current.capabilityUsage.isEmpty()) {
                item { Text("No requests yet.", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel) }
            } else {
                items(current.capabilityUsage) { (label, count) -> UsageRow(label, count) }
            }

            item { SectionTitle("Provider Usage") }
            if (current.providerUsage.isEmpty()) {
                item { Text("No requests yet.", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel) }
            } else {
                items(current.providerUsage) { (label, count) -> UsageRow(label, count) }
            }

            item { Spacer(Modifier.height(LedgerSpacing.XxLarge)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
        color = LedgerTheme.colors.tertiaryLabel,
        modifier = Modifier.padding(vertical = LedgerSpacing.Tiny),
    )
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
        Text(value, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
    }
    Spacer(Modifier.height(LedgerSpacing.Tiny))
}

@Composable
private fun UsageRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
        Text("$count", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
    }
}

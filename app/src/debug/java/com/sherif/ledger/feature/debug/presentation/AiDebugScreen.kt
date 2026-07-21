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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.ai.audit.AiDebugTrace
import com.sherif.ledger.feature.ai.audit.CacheStatus
import com.sherif.ledger.feature.debug.presentation.viewmodel.AiDebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RC6's Developer Console AI debug view: Context Preview, Prompt Preview,
 * Response Preview, Validation Result, Execution Time, Errors, Cache Status
 * — one card per request, most recent first, last 20 kept in memory only
 * (see AiDebugTraceStore; never persisted, never exported).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDebugScreen(
    onBackClick: () -> Unit,
    viewModel: AiDebugViewModel = hiltViewModel(),
) {
    val traces by viewModel.traces.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Debug") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear")
                    }
                },
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0,
    ) { padding ->
        if (traces.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Spacer(Modifier.height(LedgerSpacing.XxLarge))
                LedgerEmptyState(title = "No AI requests yet", subtitle = "Every orchestrator call, success or failure, will appear here.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = LedgerSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
        ) {
            item { Spacer(Modifier.height(LedgerSpacing.Medium)) }
            items(traces) { trace -> TraceCard(trace) }
            item { Spacer(Modifier.height(LedgerSpacing.XxLarge)) }
        }
    }
}

@Composable
private fun TraceCard(trace: AiDebugTrace) {
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeFormatter.format(Date(trace.timestampMillis)), style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text("${trace.executionTimeMs}ms", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
        Text(trace.capability.displayName, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        Text("${trace.providerId} · ${trace.model}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)

        Spacer(Modifier.height(LedgerSpacing.Small))
        LabeledBlock("Context", trace.renderedContext)
        LabeledBlock("Prompt", trace.prompt)
        trace.rawResponse?.let { LabeledBlock("Response", it) }

        Spacer(Modifier.height(LedgerSpacing.Tiny))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Validation: ${trace.validationResult}",
                style = LedgerTextStyles.Caption,
                color = if (trace.validationResult.startsWith("Valid")) LedgerTheme.colors.positive else LedgerTheme.colors.negative,
            )
            Text(
                when (trace.cacheStatus) {
                    CacheStatus.HIT -> "Cache: HIT"
                    CacheStatus.MISS -> "Cache: MISS"
                    CacheStatus.NOT_APPLICABLE -> ""
                },
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )
        }
        trace.error?.let {
            Text(it, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.negative)
        }
    }
}

@Composable
private fun LabeledBlock(label: String, content: String) {
    Text(label.uppercase(), style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
    Text(content, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel, maxLines = 4)
    Spacer(Modifier.height(LedgerSpacing.Tiny))
}

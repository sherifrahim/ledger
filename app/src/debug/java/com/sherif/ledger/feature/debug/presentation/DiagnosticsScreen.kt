package com.sherif.ledger.feature.debug.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.debug.presentation.viewmodel.DiagnosticsViewModel

/**
 * RC4's Diagnostics section. Deliberately generic — a list of collector IDs
 * with a "Run" button each, rather than one bespoke layout per data shape.
 * New collectors appear here automatically the moment they're registered in
 * DiagnosticCollectorModule; this screen never needs a new case added for
 * them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBackClick: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refreshLiveLogCount() }

    LaunchedEffect(uiState.bundleShareIntent) {
        uiState.bundleShareIntent?.let { intent ->
            context.startActivity(intent)
            viewModel.bundleShareIntentConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ledger Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LedgerSpacing.Medium)
        ) {
            // ---- Export bundle: the primary action, at the top ----
            Button(
                onClick = { viewModel.exportBundle() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isGeneratingBundle,
            ) {
                if (uiState.isGeneratingBundle) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Export Diagnostic Bundle")
                }
            }

            Text(
                "Runs every diagnostic and packages the results into a shareable zip " +
                    "(ledger_diagnostic_YYYYMMDD_HHMM.zip) — ledger.log plus one JSON file per section.",
                style = LedgerTheme.typography.bodySmall,
                color = LedgerTheme.colors.textSecondary,
                modifier = Modifier.padding(top = LedgerSpacing.Tiny, bottom = LedgerSpacing.Medium),
            )

            // ---- Live log status ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Live log buffer: ${uiState.liveLogCount} / 10,000 entries",
                    style = LedgerTheme.typography.bodyMedium,
                    color = LedgerTheme.colors.textPrimary,
                )
                OutlinedButton(onClick = { viewModel.clearLiveLogs() }) {
                    Text("Clear")
                }
            }

            Text(
                "Sections",
                style = LedgerTheme.typography.titleLarge,
                color = LedgerTheme.colors.textPrimary,
                modifier = Modifier.padding(top = LedgerSpacing.Medium, bottom = LedgerSpacing.Tiny),
            )

            LazyColumn {
                items(uiState.availableCollectorIds) { collectorId ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = LedgerSpacing.Tiny),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            collectorId,
                            style = LedgerTheme.typography.bodyMedium,
                            color = LedgerTheme.colors.textPrimary,
                        )
                        OutlinedButton(
                            onClick = { viewModel.runCollector(collectorId) },
                            enabled = !uiState.isRunningCollector,
                        ) {
                            Text("Run")
                        }
                    }
                }
            }

            // ---- Selected collector's raw output ----
            uiState.selectedOutput?.let { output ->
                Text(
                    "Output: ${uiState.selectedCollectorId}",
                    style = LedgerTheme.typography.titleLarge,
                    color = LedgerTheme.colors.textPrimary,
                    modifier = Modifier.padding(top = LedgerSpacing.Medium, bottom = LedgerSpacing.Tiny),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .background(LedgerTheme.colors.surfaceLevel1, RoundedCornerShape(8.dp))
                        .padding(LedgerSpacing.Small)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        output,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = LedgerTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}




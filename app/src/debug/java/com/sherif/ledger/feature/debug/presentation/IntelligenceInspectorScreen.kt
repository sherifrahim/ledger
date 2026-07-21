package com.sherif.ledger.feature.debug.presentation

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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.util.formatSignedPlainDecimal
import com.sherif.ledger.feature.debug.presentation.viewmodel.CategoryIntelligenceRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.DuplicateReasoningRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.ForecastRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.IntelligenceInspectorViewModel
import com.sherif.ledger.feature.debug.presentation.viewmodel.LearnedDecisionRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.RecurringRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.RelationshipRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.TransactionIntelligenceRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RC8 Phase H — Intelligence Inspector. Merchant/Category/Relationship/
 * Recurring/Forecast/Learning, all explainable in one place. No black boxes:
 * every row shows confidence + reason + source. "Ask AI" is the one AI call
 * site this RC adds, always user-triggered (see ViewModel doc comment).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelligenceInspectorScreen(
    onBackClick: () -> Unit,
    viewModel: IntelligenceInspectorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intelligence Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0,
    ) { padding ->
        if (state.loading) {
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

            state.forecast?.let { forecast ->
                item { SectionTitle("Forecast") }
                item { ForecastCard(forecast) }
            }

            item { SectionTitle("Upcoming (Subscriptions, Bills, EMIs, Rent, Salary)") }
            if (state.recurringSchedules.isEmpty()) {
                item { LedgerEmptyState(title = "Nothing recurring detected yet", subtitle = "Needs at least 2 occurrences of the same charge/income.") }
            } else {
                items(state.recurringSchedules) { schedule -> RecurringRowView(schedule) }
            }

            item { SectionTitle("Recent Transactions — Merchant & Category Intelligence") }
            items(state.transactions) { row ->
                TransactionRowView(row, onAskAi = { viewModel.askAiForCategory(row.transactionId) })
            }

            item { SectionTitle("Relationship Intelligence (top ${state.relationships.size} by confidence)") }
            if (state.relationships.isEmpty()) {
                item { LedgerEmptyState(title = "No relationships found", subtitle = "Needs persisted transactions to analyze.") }
            } else {
                items(state.relationships) { rel -> RelationshipRowView(rel) }
            }

            item { SectionTitle("Learned Decisions (deterministic memory)") }
            if (state.learnedDecisions.isEmpty()) {
                item { LedgerEmptyState(title = "Nothing learned yet", subtitle = "e.g. promoting a Candidate Account teaches its institution.") }
            } else {
                items(state.learnedDecisions) { row -> LearnedDecisionRowView(row) }
            }

            item { SectionTitle("Duplicate Evidence") }
            item {
                SectionCard {
                    Text(
                        if (state.duplicateFingerprintCount == 0) "No duplicate fingerprints found — healthy state." else "${state.duplicateFingerprintCount} fingerprint(s) shared by more than one transaction.",
                        style = LedgerTextStyles.BodyMedium,
                        color = if (state.duplicateFingerprintCount == 0) LedgerTheme.colors.positive else LedgerTheme.colors.negative,
                    )
                }
            }

            if (state.duplicateReasoning.isNotEmpty()) {
                item { SectionTitle("Duplicate Reasoning (real ReconciliationEngine scoring)") }
                items(state.duplicateReasoning) { row -> DuplicateReasoningRowView(row) }
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
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
    ) { content() }
}

@Composable
private fun ForecastCard(forecast: ForecastRow) {
    val dateFormatter = SimpleDateFormat("d MMM", Locale.getDefault())
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Current", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text(formatMoney(forecast.currentBalanceMinor, forecast.currencyCode), style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Expected in ${forecast.horizonDays}d", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text(formatMoney(forecast.expectedBalanceMinor, forecast.currencyCode), style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
        }
        forecast.projectedSalaryEpochMillis?.let {
            Text("Projected salary: ${dateFormatter.format(Date(it))}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
    }
}

@Composable
private fun RecurringRowView(schedule: RecurringRow) {
    val dateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    Column(modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(schedule.label, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
            Text(formatMoney(schedule.averageAmountMinor, schedule.currencyCode), style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        }
        Text(
            "${schedule.kind} · ${schedule.frequency} · confidence ${schedule.confidence}%",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Text(
            "Last: ${dateFormatter.format(Date(schedule.lastOccurrenceEpochMillis))} · Next expected: ${dateFormatter.format(Date(schedule.nextExpectedEpochMillis))}",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
    }
}

@Composable
private fun TransactionRowView(row: TransactionIntelligenceRow, onAskAi: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.rawText.take(50), style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, maxLines = 1)
            Text(formatMoney(row.amountMinor, row.currencyCode), style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
        }
        Text(
            "Merchant: ${row.merchant.displayName} (${row.merchant.confidence}%) — ${row.merchant.reason}",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
            maxLines = 2,
        )
        CategoryLine("Category", row.category)
        row.aiResult?.let { CategoryLine("AI Category", it) }
        if (row.canAskAi && row.aiResult == null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAskAi) { Text(if (row.askingAi) "Asking…" else "Ask AI") }
            }
        }
    }
}

@Composable
private fun CategoryLine(label: String, category: CategoryIntelligenceRow) {
    Text(
        "$label: ${category.category}${category.subcategory?.let { " ($it)" } ?: ""} — ${category.confidence}% via ${category.source}: ${category.reason}",
        style = LedgerTextStyles.Caption,
        color = LedgerTheme.colors.tertiaryLabel,
        maxLines = 2,
    )
}

@Composable
private fun RelationshipRowView(row: RelationshipRow) {
    Column(modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.type, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
            Text("${row.band} (${row.confidencePercent}%)", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
        Text(row.reasoning.joinToString(" · "), style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel, maxLines = 2)
    }
}

@Composable
private fun LearnedDecisionRowView(row: LearnedDecisionRow) {
    Row(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(row.decisionType.uppercase(), style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text(row.subjectKey, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
        }
        Text("-> ${row.learnedValue} (${row.confidence}%)", style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
    }
}

@Composable
private fun DuplicateReasoningRowView(row: DuplicateReasoningRow) {
    Column(modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small)) {
        Text(row.transactionRawText.take(50), style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, maxLines = 1)
        Text(
            "Best match: ${row.bestMatchRawText?.take(40) ?: "none"} — score ${row.score} (${row.details})",
            style = LedgerTextStyles.Caption,
            color = if (row.score >= 90) LedgerTheme.colors.negative else LedgerTheme.colors.tertiaryLabel,
            maxLines = 2,
        )
    }
}

private fun formatMoney(minorUnits: Long, currency: CurrencyCode): String =
    "${currency.name} ${formatSignedPlainDecimal(minorUnits, currency)}"

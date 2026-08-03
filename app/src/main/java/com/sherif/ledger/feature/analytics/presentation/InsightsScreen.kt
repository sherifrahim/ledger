package com.sherif.ledger.feature.analytics.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerCardDefaults
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerInteractiveLineChart
import com.sherif.ledger.core.designsystem.component.LedgerInteractivePieChart
import com.sherif.ledger.core.designsystem.component.LedgerLinePoint
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import kotlin.math.roundToInt
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding

/**
 * Insights — the analytics home, wired to real data (P-Analytics).
 *
 * Two first-class analytics components live here, integrated into the calm
 * editorial language rather than presented as reporting widgets: an **interactive
 * Spending Trend** (this month's real daily spending — a single quiet line with
 * labeled axes you can scrub to read any day) and an **interactive Spending
 * Breakdown** (real category composition as a restrained donut whose slices can be
 * tapped to reveal each category's exact value and share). Every figure comes from
 * the existing analytics (`trendPoints` / `categoryTotals`) — no new engine, no
 * fabricated progress. Honest empty state until there's activity.
 */
@Composable
fun InsightsScreen(state: InsightsUiState, onBackClick: () -> Unit = {}) {
    val hasTrend = state.trend.size >= 2 && state.trend.any { it.value != 0f }
    val hasBreakdown = state.pieSlices.isNotEmpty()

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item {
                // Title and subtitle share one text column to the right of the back
                // button. Previously the subtitle was a sibling of the whole Row, so
                // it started at the screen edge while the title started past the
                // button — two left edges where the eye expects one.
                Row(
                    modifier = Modifier.statusBarsPadding().padding(top = LedgerSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBackClick,
                        contentDescription = "Back",
                        tint = LedgerTheme.colors.textPrimary,
                    )
                    Spacer(Modifier.width(LedgerSpacing.Small))
                    Column {
                        Text("Insights", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
                        if (state.dateRange.isNotBlank()) {
                            Text(state.dateRange, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
                        }
                    }
                }
            }

            item { CashflowCard(state.incomeTotal, state.spentTotal) }

            if (hasTrend) item { TrendCard(state.trend, state.currencySymbol) }

            if (hasBreakdown) item { BreakdownCard(state) }

            if (!hasTrend && !hasBreakdown) {
                item {
                    LedgerEmptyState(
                        title = "Insights are on their way",
                        subtitle = "As your transactions are captured, your spending trend and " +
                            "category breakdown for the month build here automatically.",
                        icon = Icons.Outlined.Insights,
                        modifier = Modifier.padding(top = LedgerSpacing.XLarge),
                    )
                }
            }
        }
    }
}

@Composable
private fun CashflowCard(income: String, spent: String) {
    LedgerCard(elevation = LedgerCardDefaults.Elevation) {
        Text("This month", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Income", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                Spacer(Modifier.height(2.dp))
                Text(income, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.positive)
            }
            Column(Modifier.weight(1f)) {
                Text("Spent", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                Spacer(Modifier.height(2.dp))
                Text(spent, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
            }
        }
    }
}

@Composable
private fun TrendCard(points: List<LedgerLinePoint>, currencySymbol: String) {
    Column {
        SectionLabel("SPENDING TREND")
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
            Text("Daily spending this month", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
            Spacer(Modifier.height(LedgerSpacing.Medium))
            LedgerInteractiveLineChart(
                points = points,
                yAxisFormatter = { compactMoney(currencySymbol, it) },
                modifier = Modifier.fillMaxWidth(),
                lineColor = LedgerTheme.colors.textSecondary,
                height = 184.dp,
            )
            Spacer(Modifier.height(LedgerSpacing.Tiny))
            Text(
                "Touch and drag along the line to read any day",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun BreakdownCard(state: InsightsUiState) {
    Column {
        SectionLabel("SPENDING BREAKDOWN")
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
            LedgerInteractivePieChart(
                slices = state.pieSlices,
                restingCenterLabel = "Spent",
                restingCenterValue = state.spentTotal,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(LedgerSpacing.Medium))
            Text(
                "Tap a category to see its exact amount and share",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textTertiary,
            )
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = LedgerTheme.colors.textTertiary,
    )
}

/**
 * Compact currency for a chart axis tick — approximate by design (the exact figure is
 * one scrub away in the callout), so thousands collapse to "k" and millions to "M".
 * Operates on major units (the value already divided out of minor units by the VM).
 */
private fun compactMoney(symbol: String, majorValue: Float): String {
    val magnitude = when {
        majorValue >= 1_000_000f -> trimZero(majorValue / 1_000_000f) + "M"
        majorValue >= 1_000f -> trimZero(majorValue / 1_000f) + "k"
        else -> majorValue.roundToInt().toString()
    }
    return "$symbol $magnitude"
}

private fun trimZero(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}

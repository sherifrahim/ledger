package com.sherif.ledger.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerCardDefaults
import com.sherif.ledger.core.designsystem.component.LedgerDonutChart
import com.sherif.ledger.core.designsystem.component.LedgerDonutSlice
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerLineChart
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Insights — the analytics home, wired to real data (P-Analytics).
 *
 * Two first-class analytics components live here, integrated into the calm
 * editorial language rather than presented as reporting widgets: a **Financial
 * Trend** (this month's real daily spending, a single quiet line) and a **Spending
 * Breakdown** (real category composition as a restrained donut with a plain
 * legend). Every figure comes from the existing analytics
 * (`FinancialAnalytics.trendPoints` / `categoryTotals`) — no new engine, no
 * fabricated progress. Honest empty state until there's activity.
 */
@Composable
fun InsightsScreen(state: InsightsUiState, onBackClick: () -> Unit = {}) {
    val hasTrend = state.chartPoints.size >= 2 && state.chartPoints.any { it != 0f }
    val hasBreakdown = state.categories.isNotEmpty()

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
                bottom = LedgerSpacing.ScreenBottom + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = LedgerSpacing.Small)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LedgerIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onBackClick,
                            contentDescription = "Back",
                            tint = LedgerTheme.colors.textPrimary,
                        )
                        Spacer(Modifier.width(LedgerSpacing.Small))
                        Text("Insights", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
                    }
                    if (state.dateRange.isNotBlank()) {
                        Text(state.dateRange, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
                    }
                }
            }

            item { CashflowCard(state.incomeTotal, state.spentTotal) }

            if (hasTrend) item { TrendCard(state.chartPoints) }

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
private fun TrendCard(points: List<Float>) {
    Column {
        SectionLabel("SPENDING TREND")
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
            Text("Daily spending this month", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
            Spacer(Modifier.height(LedgerSpacing.Medium))
            LedgerLineChart(
                data = points,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                lineColor = LedgerTheme.colors.textSecondary,
                fill = true,
            )
        }
    }
}

@Composable
private fun BreakdownCard(state: InsightsUiState) {
    // Show the five largest categories individually; fold everything else into a
    // single "Other" entry so both the donut ring and the legend account for the
    // full 100% of spending — never a legend that silently sums to less.
    val shown = state.categories.take(5)
    val rest = state.categories.drop(5)
    val otherColor = LedgerTheme.colors.textTertiary
    val otherPercent = rest.sumOf { it.percentageValue }

    Column {
        SectionLabel("SPENDING BREAKDOWN")
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedgerDonutChart(
                    slices = shown.map { LedgerDonutSlice(it.percentageValue.toFloat().coerceAtLeast(0.5f), it.color) } +
                        if (rest.isNotEmpty()) listOf(LedgerDonutSlice(otherPercent.toFloat().coerceAtLeast(0.5f), otherColor)) else emptyList(),
                    modifier = Modifier.size(132.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spent", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                        Text(state.spentTotal, style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
                    }
                }
                Spacer(Modifier.width(LedgerSpacing.Large))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                    shown.forEach { category ->
                        LegendRow(category.color, category.name, category.percentageValue)
                    }
                    if (rest.isNotEmpty()) {
                        LegendRow(otherColor, "Other", otherPercent)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, name: String, percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(name, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        Text("$percent%", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
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

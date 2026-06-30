package com.sherif.ledger.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerLineChart
import com.sherif.ledger.core.designsystem.component.LedgerIdentityType
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight

data class InsightsUiState(
    val totalSpent: String,
    val dateRange: String,
    val percentChange: String,
    val categories: List<CategoryInsightUi>,
)

data class CategoryInsightUi(
    val name: String,
    val amount: String,
    val percentage: String,
    val color: Color,
)

private val previewState = InsightsUiState(
    totalSpent = "1,950",
    dateRange = "Jun 1 - Jun 30",
    percentChange = "↑ 8% vs last month",
    categories = listOf(
        CategoryInsightUi("Groceries", "AED 680", "35%", Color(0xFFFACC15)),
        CategoryInsightUi("Shopping", "AED 420", "22%", Color(0xFF10B981)),
        CategoryInsightUi("Transport", "AED 250", "13%", Color(0xFF3B82F6)),
        CategoryInsightUi("Dining", "AED 230", "12%", Color(0xFFEF4444)),
        CategoryInsightUi("Entertainment", "AED 160", "8%", Color(0xFF8B5CF6)),
    ),
)

@Composable
fun InsightsScreen(
    state: InsightsUiState = previewState,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.XxLarge, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item("header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = LedgerSpacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Insights", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.label)
                Icon(
                    Icons.Filled.CalendarToday,
                    null,
                    tint = LedgerTheme.colors.label,
                    modifier = Modifier.size(LedgerTheme.iconSize.Medium).ledgerClickable { /* TODO */ },
                )
            }
        }

        item("tabs") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                InsightTabs()
            }
        }

        item("overview") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
            ) {
                Text(
                    text = "Spending overview",
                    style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                    color = LedgerTheme.colors.secondaryLabel
                )
                Text(state.dateRange, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                Spacer(Modifier.height(LedgerSpacing.Small))
                LedgerAmount(
                    amount = "AED ${state.totalSpent}",
                    style = LedgerAmountStyle.Display,
                    color = LedgerTheme.colors.label,
                )
                Text(state.percentChange, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.success)
                
                Spacer(Modifier.height(LedgerSpacing.Large))
                
                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    LedgerLineChart(
                        data = listOf(1200f, 1400f, 1300f, 1700f, 1800f, 1600f, 1900f, 1800f, 2400f),
                        lineColor = LedgerTheme.colors.expense,
                    )
                }
            }
        }

        item("categories_header") {
            LedgerSectionHeader(title = "Categories", trailing = "See all")
        }

        items(state.categories) { category ->
            CategoryItem(category)
        }
    }
}

@Composable
private fun InsightTabs() {
    Row(
        modifier = Modifier
            .ledgerSurface(level = LedgerSurfaceLevel.Level1, shape = LedgerRadius.Full)
            .padding(LedgerSpacing.XxSmall),
    ) {
        InsightTab("Week", selected = false)
        InsightTab("Month", selected = true)
        InsightTab("3M", selected = false)
        InsightTab("Year", selected = false)
    }
}

@Composable
private fun InsightTab(label: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .ledgerSurface(
                backgroundColor = if (selected) LedgerTheme.colors.surfaceLevel2 else Color.Transparent,
                borderColor = Color.Transparent,
                shape = LedgerRadius.Full,
            )
            .ledgerClickable { /* TODO */ }
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.XxSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = if (selected) LedgerTheme.colors.label else LedgerTheme.colors.tertiaryLabel,
        )
    }
}

@Composable
private fun CategoryItem(category: CategoryInsightUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerClickable { /* TODO */ }
            .padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerBrandIcon(
            name = category.name,
            type = LedgerIdentityType.Category,
            size = LedgerTheme.iconSize.Large,
        )
        Spacer(Modifier.width(LedgerSpacing.Medium))
        Text(category.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label, modifier = Modifier.weight(1f))
        Text(category.amount, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(category.percentage, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = LedgerTheme.colors.tertiaryLabel,
            modifier = Modifier.size(16.dp),
        )
    }
}

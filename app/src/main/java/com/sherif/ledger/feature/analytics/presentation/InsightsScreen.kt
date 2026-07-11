package com.sherif.ledger.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun InsightsScreen(
    state: InsightsUiState
) {
    Scaffold(
        topBar = {
            InsightsTopBar()
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.SectionGap)
        ) {
            item {
                InsightsSegmentedControl()
            }

            item {
                ThisMonthSection(state.spentTotal)
            }

            item {
                CashflowSection(state.incomeTotal, state.spentTotal)
            }

            item {
                TopCategoriesSection(state.categories)
            }
            
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun InsightsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.ScreenPadding, vertical = LedgerSpacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(44.dp))

        Text(
            text = "Insights",
            style = LedgerTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerTheme.colors.textPrimary
        )

        LedgerIconButton(
            icon = Icons.Default.CalendarToday,
            onClick = { /* TODO */ },
            tint = LedgerTheme.colors.textPrimary
        )
    }
}

@Composable
private fun InsightsSegmentedControl() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(LedgerTheme.colors.surfaceInset)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(LedgerTheme.colors.surfaceBase)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Overview", style = LedgerTheme.typography.labelLarge, color = LedgerTheme.colors.textPrimary)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Breakdown", style = LedgerTheme.typography.labelLarge, color = LedgerTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun ThisMonthSection(spent: String) {
    Column {
        Text("This Month", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text("Spent", style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        Text(text = spent, style = LedgerTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { 0.62f },
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape),
                color = LedgerTheme.colors.system,
                trackColor = LedgerTheme.colors.border
            )
            Spacer(Modifier.width(16.dp))
            Text("12 days left", style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textTertiary)
        }
    }
}

@Composable
private fun CashflowSection(income: String, spent: String) {
    Column {
        Text("Cashflow", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Income", style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textTertiary)
                Text(text = income, style = LedgerTheme.typography.titleLarge, color = LedgerTheme.colors.positive, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Expenses", style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textTertiary)
                Text(text = spent, style = LedgerTheme.typography.titleLarge, color = LedgerTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TopCategoriesSection(categories: List<CategoryInsightUi>) {
    Column {
        Text("Top Categories", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        categories.forEach { category ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.name, modifier = Modifier.width(100.dp), style = LedgerTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { category.percentageValue / 100f },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = category.color,
                    trackColor = LedgerTheme.colors.border
                )
                Spacer(Modifier.width(16.dp))
                Text(category.amount, style = LedgerTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("${category.percentageValue}%", style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textTertiary)
            }
        }
    }
}

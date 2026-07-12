package com.sherif.ledger.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            DashboardTopBar()
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
                TotalBalanceSection(state.totalBalance, state.balanceChangePercentage)
            }

            item {
                CategoryChipsSection(state.categories)
            }

            item {
                SummaryCardsSection(
                    monthlyExpenses = state.monthlyExpenses,
                    intelligenceSummary = state.intelligenceSummary,
                )
            }

            item {
                RecentActivityHeader(onSeeAllClick = onNavigateToTransactions)
            }

            state.recentActivity.forEach { group ->
                item {
                    Text(
                        text = group.title,
                        style = LedgerTheme.typography.labelLarge,
                        color = LedgerTheme.colors.textTertiary,
                        modifier = Modifier.padding(vertical = LedgerSpacing.Small)
                    )
                }
                items(group.items, key = { it.id }) { item ->
                    LedgerTransactionRow(
                        title = item.merchantName,
                        amount = item.amount,
                        explanation = item.explanation,
                        currency = "AED",
                        isExpense = item.isExpense,
                        time = item.time,
                        onClick = { /* TODO: Transaction Detail */ }
                    )
                    LedgerDivider(alpha = 0.05f)
                }
            }
            
            item {
                Spacer(Modifier.height(100.dp)) // Nav bar buffer
            }
        }
    }
}

@Composable
private fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.ScreenPadding, vertical = LedgerSpacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LedgerTheme.colors.surfaceInset),
            contentAlignment = Alignment.Center
        ) {
            Text("SR", style = LedgerTheme.typography.labelLarge)
        }

        Text(
            text = "ledger",
            style = LedgerTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = LedgerTheme.colors.textPrimary
        )

        LedgerIconButton(
            icon = Icons.Default.Notifications,
            onClick = { /* TODO */ },
            tint = LedgerTheme.colors.textPrimary
        )
    }
}

@Composable
private fun TotalBalanceSection(balance: String, change: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Financial State",
            style = LedgerTheme.typography.labelLarge,
            color = LedgerTheme.colors.textSecondary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = balance,
                style = LedgerTheme.typography.displayLarge,
                color = LedgerTheme.colors.textPrimary
            )
            Spacer(Modifier.width(LedgerSpacing.Small))
            if (change != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LedgerTheme.colors.positive.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = change,
                        style = LedgerTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = LedgerTheme.colors.positive
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsSection(categories: List<CategoryFilterUiModel>) {
    if (categories.isEmpty()) return
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
        contentPadding = PaddingValues(vertical = LedgerSpacing.Small)
    ) {
        items(categories) { category ->
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (category.isSelected) LedgerTheme.colors.textPrimary else LedgerTheme.colors.surfaceInset)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category.label,
                    style = LedgerTheme.typography.labelLarge,
                    color = if (category.isSelected) LedgerTheme.colors.surfaceBase else LedgerTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryCardsSection(
    monthlyExpenses: String,
    intelligenceSummary: List<String>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.ContentGap)
    ) {
        // Monthly Summary Card
        LedgerSurface(
            modifier = Modifier.weight(1f),
            level = LedgerSurfaceLevel.Inset,
            shape = LedgerRadius.Large,
            contentPadding = PaddingValues(LedgerSpacing.Medium)
        ) {
            Text("Monthly", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text("Summary", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                "Spending Pulse",
                style = LedgerTheme.typography.bodySmall,
                color = LedgerTheme.colors.textTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = monthlyExpenses,
                style = LedgerTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }

        // Ledger Intelligence Card — real backend-derived facts only. Empty until
        // RelationshipEngine has actually found something; never a fabricated
        // confidence figure or "System healthy" placeholder.
        LedgerSurface(
            modifier = Modifier.weight(1f),
            level = LedgerSurfaceLevel.Inset,
            shape = LedgerRadius.Large,
            contentPadding = PaddingValues(LedgerSpacing.Medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ledger", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(24.dp).clip(CircleShape).background(LedgerTheme.colors.system.copy(alpha = 0.2f)))
            }
            Text("Intelligence", style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                intelligenceSummary.getOrNull(1) ?: "No patterns identified yet",
                style = LedgerTheme.typography.bodySmall,
                color = LedgerTheme.colors.textTertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = intelligenceSummary.getOrNull(0) ?: "Analyzing",
                style = LedgerTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun RecentActivityHeader(onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "RECENT ACTIVITY",
            style = LedgerTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = LedgerTheme.colors.textSecondary
        )
        TextButton(onClick = onSeeAllClick) {
            Text("See all", color = LedgerTheme.colors.positive)
        }
    }
}


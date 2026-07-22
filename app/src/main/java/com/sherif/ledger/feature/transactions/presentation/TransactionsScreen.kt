package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onTransactionClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TransactionsTopBar(onSearchClick)
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding)
        ) {
            item {
                TransactionFilterChips()
            }

            state.groups.forEach { group ->
                item {
                    Text(
                        text = group.title,
                        style = LedgerTheme.typography.labelLarge,
                        color = LedgerTheme.colors.textTertiary,
                        modifier = Modifier.padding(vertical = LedgerSpacing.Medium)
                    )
                }
                
                items(group.transactions, key = { it.id }) { txn ->
                    LedgerTransactionRow(
                        title = txn.merchant,
                        amount = txn.amount,
                        explanation = if (txn.category == MerchantCategory.Salary) "Income" else "Matched",
                        currency = "AED",
                        isExpense = txn.category != MerchantCategory.Salary,
                        onClick = { onTransactionClick(txn.id) }
                    )
                    LedgerDivider(alpha = 0.05f)
                }
            }
            
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun TransactionsTopBar(onSearchClick: () -> Unit) {
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
            text = "Transactions",
            style = LedgerTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerTheme.colors.textPrimary
        )

        LedgerIconButton(
            icon = Icons.Default.Search,
            onClick = onSearchClick,
            contentDescription = "Search",
            tint = LedgerTheme.colors.textPrimary
        )
    }
}

@Composable
private fun TransactionFilterChips() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
    ) {
        val filters = listOf("All", "Income", "Expenses", "Transfers")
        filters.forEachIndexed { index, filter ->
            val isSelected = index == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (isSelected) LedgerTheme.colors.textPrimary else LedgerTheme.colors.surfaceInset)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    style = LedgerTheme.typography.labelLarge,
                    color = if (isSelected) LedgerTheme.colors.surfaceBase else LedgerTheme.colors.textSecondary
                )
            }
        }
    }
}

package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onTransactionClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            LedgerScreenHeader(
                title = "Transactions",
                onBackClick = onBackClick,
                modifier = Modifier.padding(horizontal = LedgerSpacing.ScreenPadding),
                actions = {
                    LedgerIconButton(icon = Icons.Default.Add, onClick = onAddClick, contentDescription = "Add transaction", tint = LedgerTheme.colors.textPrimary)
                    LedgerIconButton(icon = Icons.Default.Search, onClick = onSearchClick, contentDescription = "Search", tint = LedgerTheme.colors.textPrimary)
                },
            )
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding,
                end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            )
        ) {
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
                        modifier = Modifier.animateItem(
                            fadeInSpec = LedgerAnimations.itemAppear(),
                            placementSpec = LedgerAnimations.itemPlacement(),
                            fadeOutSpec = LedgerAnimations.itemDisappear(),
                        ),
                        title = txn.merchant,
                        amount = txn.amount,
                        explanation = if (txn.category == MerchantCategory.Salary) "Income" else txn.category.name,
                        currency = "AED",
                        isExpense = txn.isExpense,
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



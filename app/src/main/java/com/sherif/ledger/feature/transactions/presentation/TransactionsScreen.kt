package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    onBackClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TransactionsTopBar(onSearchClick, onBackClick)
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding)
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
                        title = txn.merchant,
                        amount = txn.amount,
                        explanation = if (txn.category == MerchantCategory.Salary) "Income" else txn.category.name,
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
private fun TransactionsTopBar(onSearchClick: () -> Unit, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.ScreenPadding, vertical = LedgerSpacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackClick,
            contentDescription = "Back",
            tint = LedgerTheme.colors.textPrimary
        )

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


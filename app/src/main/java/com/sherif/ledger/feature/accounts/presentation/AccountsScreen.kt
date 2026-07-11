package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onNavigateToInsights: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            AccountsTopBar()
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
        ) {
            state.sections.forEach { section ->
                items(section.accounts, key = { it.id }) { account ->
                    AccountItemRow(account)
                    LedgerDivider(alpha = 0.05f)
                }
            }

            item {
                Spacer(Modifier.height(LedgerSpacing.Large))
                LedgerButton(
                    text = "Add Account",
                    onClick = { /* TODO */ },
                    style = LedgerButtonStyle.Ghost,
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun AccountsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.ScreenPadding, vertical = LedgerSpacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(44.dp)) // Offset for center title

        Text(
            text = "Accounts",
            style = LedgerTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerTheme.colors.textPrimary
        )

        LedgerIconButton(
            icon = Icons.Default.Add,
            onClick = { /* TODO */ },
            tint = LedgerTheme.colors.textPrimary
        )
    }
}

@Composable
private fun AccountItemRow(account: AccountUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)
    ) {
        // Bank Icon
        LedgerBrandIcon(
            name = account.name,
            size = 40.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = account.name,
                style = LedgerTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.textPrimary
            )
            Text(
                text = account.subtitle,
                style = LedgerTheme.typography.bodySmall,
                color = LedgerTheme.colors.textSecondary
            )
            
            // Mock payment due for credit cards to match mock 1:1
            if (account.subtitle.lowercase().contains("credit")) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LedgerTheme.colors.attention.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Payment due in 5 days",
                        style = LedgerTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = LedgerTheme.colors.attention
                    )
                }
            }
        }

        LedgerAmount(
            amount = account.balance,
            style = LedgerAmountStyle.Regular,
            color = if (account.isNegative) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary
        )
    }
}

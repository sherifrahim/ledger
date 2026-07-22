package com.sherif.ledger.feature.transactions.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
fun TransactionDetailsScreen(
    onBackClick: () -> Unit = {},
    state: TransactionDetailsUiState,
) {
    Scaffold(
        topBar = {
            DetailsTopBar(onBackClick)
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(LedgerSpacing.Large))
                LedgerBrandIcon(name = state.merchant, size = 80.dp)
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Text(state.merchant, style = LedgerTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(state.merchantCategory, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textSecondary)
                
                Spacer(Modifier.height(LedgerSpacing.Large))
                Text(
                    text = state.sign + state.amount,
                    style = LedgerTheme.typography.displayLarge.copy(fontSize = 40.sp),
                    fontWeight = FontWeight.Black
                )
                
                Spacer(Modifier.height(LedgerSpacing.Small))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(LedgerTheme.colors.surfaceInset)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (state.isIncome) "Income" else "Expense",
                        style = LedgerTheme.typography.labelLarge,
                        color = LedgerTheme.colors.textSecondary
                    )
                }
                
                Spacer(Modifier.height(LedgerSpacing.Massive))
            }

            item {
                DetailsListSection(state)
            }

            item {
                Spacer(Modifier.height(LedgerSpacing.Large))
                LedgerButton(
                    text = "Add note",
                    onClick = { /* TODO */ },
                    style = LedgerButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailsTopBar(onBackClick: () -> Unit) {
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

        LedgerIconButton(
            icon = Icons.Default.Share,
            onClick = { /* TODO */ },
            contentDescription = "Share",
            tint = LedgerTheme.colors.textPrimary
        )
    }
}

@Composable
private fun DetailsListSection(state: TransactionDetailsUiState) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        DetailRow("Date", state.date)
        LedgerDivider(alpha = 0.05f)
        DetailRow("Time", state.time)
        LedgerDivider(alpha = 0.05f)
        DetailRow("Payment Method", state.paymentMethod)
        LedgerDivider(alpha = 0.05f)
        DetailRow("Card", "**** ${state.accountNumber.takeLast(4)}")
        LedgerDivider(alpha = 0.05f)
        DetailRow("Reference", state.reference)
        LedgerDivider(alpha = 0.05f)
        DetailRow("Location", "Abu Dhabi, UAE")
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textTertiary)
        Text(value, style = LedgerTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LedgerTheme.colors.textPrimary)
    }
}






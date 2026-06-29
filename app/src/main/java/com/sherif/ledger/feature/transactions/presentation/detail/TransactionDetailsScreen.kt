package com.sherif.ledger.feature.transactions.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.layout.LedgerMetadataRow
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.preview.PreviewTransactions
import com.sherif.ledger.core.designsystem.component.MerchantHeader

private val previewState = run {
    val t = PreviewTransactions.amazonToday
    TransactionDetailsUiState(
        merchant = t.merchant.name,
        merchantCategory = t.merchant.category,
        merchantAccentHue = t.merchant.accentHue,
        amount = t.amount,
        sign = if (t.isIncome) "+" else "-",
        isIncome = t.isIncome,
        date = t.date,
        time = t.time,
        status = t.status,
        paymentMethod = t.paymentMethod,
        accountName = t.account.name,
        accountNumber = t.account.accountNumber,
        reference = t.reference,
        history = listOf(
            MerchantHistoryItem("AED 89", "20 Jun"),
            MerchantHistoryItem("AED 245", "14 Jun"),
            MerchantHistoryItem("AED 32", "3 Jun"),
        ),
        notes = "Weekly groceries and home essentials from Amazon Fresh.",
    )
}

@Composable
fun TransactionDetailsScreen(
    state: TransactionDetailsUiState = previewState,
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
            ) {
                MerchantHeader(
                    name = state.merchant,
                    category = state.merchantCategory,
                    accentHue = state.merchantAccentHue,
                    avatarSize = LedgerTheme.iconSize.Huge,
                )
                
                LedgerAmount(
                    amount = "${state.sign}AED ${state.amount}",
                    style = LedgerAmountStyle.Display,
                    color = if (state.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.label,
                )
            }
        }

        item("details") {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Group)) {
                LedgerSectionHeader(title = "Transaction facts")
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                    LedgerMetadataRow(label = "Date", value = state.date)
                    LedgerMetadataRow(label = "Time", value = state.time)
                    LedgerMetadataRow(label = "Status", value = state.status)
                    LedgerMetadataRow(label = "Payment", value = state.paymentMethod)
                    LedgerMetadataRow(label = "Account", value = state.accountName)
                    LedgerMetadataRow(label = "Reference", value = state.reference)
                }
            }
        }

        if (state.notes != null) {
            item("notes") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                    LedgerSectionHeader(title = "Notes")
                    Text(
                        text = state.notes,
                        style = LedgerTextStyles.Body,
                        color = LedgerTheme.colors.secondaryLabel,
                    )
                }
            }
        }

        if (state.history.isNotEmpty()) {
            item("history") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                    LedgerSectionHeader(
                        title = "History at ${state.merchant}",
                        trailing = "See all",
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                        state.history.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(item.date, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                                LedgerAmount(amount = item.amount, style = LedgerAmountStyle.Small)
                            }
                            if (index != state.history.lastIndex) LedgerHairline()
                        }
                    }
                }
            }
        }
    }
}

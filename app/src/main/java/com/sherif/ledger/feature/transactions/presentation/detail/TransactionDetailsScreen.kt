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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.preview.PreviewTransactions
import com.sherif.ledger.feature.transactions.presentation.detail.components.AmountHero
import com.sherif.ledger.feature.transactions.presentation.detail.components.InformationSection
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
        notes = null,
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
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge),
    ) {
        item("merchant") {
            MerchantHeader(
                name = state.merchant,
                category = state.merchantCategory,
                accentHue = state.merchantAccentHue,
            )
        }

        item("amount") {
            AmountHero(
                amount = state.amount,
                sign = state.sign,
                isIncome = state.isIncome,
                status = state.status,
                time = state.time,
            )
        }

        item("info") {
            InformationSection(
                items = listOf(
                    "Date" to state.date,
                    "Time" to state.time,
                    "Payment" to state.paymentMethod,
                    "Account" to "${state.accountName} ${state.accountNumber}",
                    "Reference" to state.reference,
                ),
            )
        }

        if (state.history.isNotEmpty()) {
            item("history_header") {
                LedgerSectionHeader(
                    title = "Previous at ${state.merchant}",
                    trailing = "${state.history.size} transactions",
                )
            }
            item("history") {
                LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(LedgerSpacing.Inline)) {
                    state.history.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Small),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(item.date, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                            Text(
                                "${if (item.isIncome) "+" else "-"}${item.amount}",
                                style = LedgerTextStyles.Label,
                                color = if (item.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense,
                            )
                        }
                        if (index != state.history.lastIndex) LedgerHairline()
                    }
                }
            }
        }

        item("notes") {
            LedgerSectionHeader(title = "Notes")
            Spacer(Modifier.height(LedgerSpacing.Content))
            LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(LedgerSpacing.Group)) {
                Text(
                    state.notes ?: "No notes yet",
                    style = LedgerTextStyles.Body,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
            }
        }
    }
}

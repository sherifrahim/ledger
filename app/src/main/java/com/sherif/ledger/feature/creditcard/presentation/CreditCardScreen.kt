package com.sherif.ledger.feature.creditcard.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAutoSizeText
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerCardDefaults
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.feature.creditcard.presentation.viewmodel.CreditCardUiState

/**
 * Credit Card Manager — one card's real picture: what it costs you to spend on
 * it right now (outstanding), what's left to spend (available), and what this
 * month has already cost. Every figure here comes from
 * [com.sherif.ledger.core.domain.usecase.creditcard.GetCreditCardDetailsUseCase],
 * which is itself built on [com.sherif.ledger.core.domain.service.transaction.AccountBalanceService] —
 * nothing is a second source of truth for a number Accounts already derives.
 */
@Composable
fun CreditCardScreen(
    state: CreditCardUiState,
    onBackClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onManageLimit: () -> Unit = {},
) {
    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        if (!state.isLoading && !state.found) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                LedgerScreenHeader(
                    title = "Card",
                    onBackClick = onBackClick,
                    modifier = Modifier.padding(horizontal = LedgerSpacing.ScreenPadding),
                )
                LedgerEmptyState(
                    title = "Card not found",
                    subtitle = "This card may have been removed.",
                    icon = Icons.Outlined.CreditCard,
                    modifier = Modifier.padding(top = LedgerSpacing.Large),
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item {
                LedgerScreenHeader(
                    title = state.cardName.ifBlank { "Card" },
                    subtitle = state.cardTail?.let { "···$it" },
                    onBackClick = onBackClick,
                )
            }

            item {
                com.sherif.ledger.core.designsystem.component.CreditCardVisual(
                    bankName = state.cardName.ifBlank { "Card" },
                    tail = state.cardTail,
                )
            }

            item { CardSummary(state, onManageLimit) }

            if (state.transactions.isNotEmpty()) {
                item {
                    Text(
                        "TRANSACTIONS",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = LedgerTheme.colors.textTertiary,
                    )
                }
                items(state.transactions, key = { it.id }) { txn ->
                    Column {
                        LedgerTransactionRow(
                            title = txn.merchant,
                            amount = txn.amount,
                            currency = state.currency,
                            isExpense = txn.isExpense,
                            time = txn.date,
                            onClick = { onTransactionClick(txn.id) },
                        )
                        LedgerDivider(alpha = 0.05f)
                    }
                }
            } else if (!state.isLoading) {
                item {
                    LedgerEmptyState(
                        title = "No transactions yet",
                        subtitle = "Purchases on this card will appear here as they're captured.",
                        icon = Icons.Outlined.CreditCard,
                        modifier = Modifier.padding(top = LedgerSpacing.Large),
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSummary(state: CreditCardUiState, onManageLimit: () -> Unit) {
    val colors = LedgerTheme.colors
    com.sherif.ledger.core.designsystem.component.LedgerHeroCard {
        Text("Outstanding", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = colors.textSecondary)
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerAutoSizeText(
            text = "${state.currency} ${state.outstanding}",
            style = LedgerTextStyles.Hero,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )

        state.utilization?.let { fraction ->
            Spacer(Modifier.height(LedgerSpacing.Small))
            // Above 90% of the limit is the zone a decline becomes likely on the
            // next purchase — worth a colour change, not just a number.
            val barColor = if (fraction >= 0.9f) colors.negative else colors.accent
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.surfaceInset)) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(barColor),
                )
            }
            Spacer(Modifier.height(LedgerSpacing.Tiny))
            Text(
                "${(fraction * 100).toInt()}% of limit used",
                style = LedgerTextStyles.Caption,
                color = colors.textTertiary,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
            SummaryStat("Limit", state.limit?.let { "${state.currency} $it" } ?: "Not set", Modifier.weight(1f))
            SummaryStat("Available", state.available?.let { "${state.currency} $it" } ?: "—", Modifier.weight(1f))
            SummaryStat("Spent this month", "${state.currency} ${state.monthSpend}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            if (state.limit == null) "Set credit limit" else "Edit credit limit",
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
            color = colors.accent,
            modifier = Modifier.ledgerClickable(onClick = onManageLimit),
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary, maxLines = 1)
    }
}

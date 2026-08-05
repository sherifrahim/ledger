package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

/**
 * Accounts — wired to live data (Milestone P1).
 *
 * Every figure is real: net worth, assets, liabilities and each account's balance
 * come from [com.sherif.ledger.feature.accounts.presentation.viewmodel.AccountsViewModel],
 * which replays persisted transactions via AccountBalanceService (Financial Truth,
 * ADR-0000 — no stored/cached balance). Styling follows DESIGN_REFERENCE: a Total
 * Balance hero over grouped account rows. No fabricated content.
 */
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onNavigateToInsights: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onAccountClick: (Long) -> Unit = {},
    onMergeAccountsClick: () -> Unit = {},
) {
    val currency = state.netWorthCurrency
    val hasAccounts = state.sections.any { it.accounts.isNotEmpty() }
    val accountCount = state.sections.sumOf { it.accounts.size }

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item { AccountsHeader(onBackClick) }

            item { TotalBalanceCard(currency, state.netWorth, state.netWorthIsNegative, state.assetsTotal, state.liabilitiesTotal) }

            if (hasAccounts) {
                state.sections.forEach { section ->
                    item {
                        Column {
                            SectionLabel(section.title.uppercase())
                            Spacer(Modifier.height(LedgerSpacing.Small))
                            LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
                                section.accounts.forEachIndexed { i, account ->
                                    AccountRow(
                                        account, currency,
                                        onClick = account.id.toLongOrNull()
                                            ?.takeIf { account.isCreditCard }
                                            ?.let { id -> { onAccountClick(id) } },
                                    )
                                    if (i < section.accounts.lastIndex) LedgerDivider(alpha = 0.05f)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    LedgerEmptyState(
                        title = "No accounts yet",
                        subtitle = "Ledger creates an account automatically from your first captured " +
                            "bank message, then tracks its balance for you.",
                        icon = Icons.Outlined.AccountBalanceWallet,
                        modifier = Modifier.padding(top = LedgerSpacing.Large),
                    )
                }
            }

            state.insight?.let { insight ->
                item {
                    LedgerCard(elevation = LedgerCardDefaults.ElevationLow, onClick = onNavigateToInsights) {
                        Text(insight.title, style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
                        Spacer(Modifier.height(LedgerSpacing.Tiny))
                        Text(insight.subtitle, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
                    }
                }
            }

            // Two rows for what is really one bank account does happen — an
            // institution the resolver hadn't learned yet, or data from before its
            // duplicate-prevention existed. Merging needs two accounts to pick from.
            if (accountCount >= 2) {
                item {
                    Text(
                        "Merge duplicate accounts",
                        style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.SemiBold),
                        color = LedgerTheme.colors.accent,
                        modifier = Modifier
                            .ledgerClickable(onClick = onMergeAccountsClick)
                            .padding(vertical = LedgerSpacing.Small),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountsHeader(onBackClick: () -> Unit) {
    LedgerScreenHeader(title = "Accounts", onBackClick = onBackClick)
}

@Composable
private fun TotalBalanceCard(currency: String, netWorth: String, isNegative: Boolean, assets: String, liabilities: String) {
    LedgerHeroCard {
        // "Net Worth", not "Total Balance". This figure is assets MINUS card debt,
        // while the Dashboard's "Total Balance" is the cash you hold — two different
        // numbers under one label is how a user ends up not trusting either.
        Text("Net Worth", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerAutoSizeText(
            text = (if (isNegative) "−" else "") + "$currency $netWorth",
            style = LedgerTextStyles.Hero,
            color = if (isNegative) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge)) {
            Breakdown("Assets", "$currency $assets", LedgerTheme.colors.positive)
            Breakdown("Liabilities", "$currency $liabilities", LedgerTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun Breakdown(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = valueColor)
    }
}

@Composable
private fun AccountRow(account: AccountUi, currency: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        // Tighter than the default gap: every dp in the gutter is a dp taken from
        // the account name, and names are what distinguish one row from another.
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        LedgerBrandIcon(name = account.name, type = LedgerIdentityType.Bank, size = 34.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                account.name,
                style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LedgerTheme.colors.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                // Type plus the tail, so "ADCB Account" twice reads as two accounts
                // rather than as one shown twice.
                listOfNotNull(
                    account.subtitle.lowercase().replaceFirstChar { it.uppercase() },
                    account.accountTail?.let { "···$it" },
                ).joinToString(" "),
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textTertiary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        LedgerAmount(
            // Negative balances carry an explicit minus sign — colour alone is not a
            // signal a user can rely on (and is invisible to colour-blind users).
            // A balance carries a minus when it is negative and nothing when it is
            // not — unlike a transaction, where the sign is the direction of travel.
            // U+2212, so the sign stays tight against the tabular digits.
            amount = if (account.isNegative) "−${account.balance}" else account.balance,
            currency = account.currency,
            style = LedgerAmountStyle.Regular,
            color = if (account.isNegative) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = LedgerTheme.colors.textTertiary,
    )
}

package com.sherif.ledger.feature.debug.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.util.formatSignedPlainDecimal
import com.sherif.ledger.feature.debug.presentation.viewmodel.BalanceInspectorViewModel
import com.sherif.ledger.feature.debug.presentation.viewmodel.CandidateAccountRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.CategoryRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.ExcludedAccountRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.IncludedAccountRow
import com.sherif.ledger.feature.debug.presentation.viewmodel.NonPrimaryCurrencyRow

/**
 * RC5 Part 2 — permanent Developer Console page. Explains every AED in the
 * Dashboard's Financial State by re-presenting the SAME two existing
 * computations (FinancialTraceCollector.buildReport, already RC4's balance
 * replay; GetFinancialAnalyticsUseCase.computeNetWorth, what the Dashboard
 * itself calls) side by side. See BalanceInspectorViewModel's doc comment —
 * this screen never computes a balance itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceInspectorScreen(
    onBackClick: () -> Unit,
    viewModel: BalanceInspectorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance Inspector") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0,
    ) { padding ->
        if (state.loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                CircularProgressIndicator(color = LedgerTheme.colors.tint)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = LedgerSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
        ) {
            item { Spacer(Modifier.height(LedgerSpacing.Medium)) }

            item {
                SectionCard {
                    Text("FINANCIAL STATE", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                    Text(
                        text = formatMoney(state.dashboardDisplayedMinor, state.currencyCode),
                        style = LedgerTextStyles.Headline,
                        color = if (state.dashboardDisplayedMinor < 0) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
                    )
                    Text(
                        "What the Dashboard actually displays right now (GetFinancialAnalyticsUseCase.computeNetWorth).",
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.tertiaryLabel,
                    )
                }
            }

            item { SectionTitle("Category Breakdown") }
            items(state.categories) { category -> CategoryRowView(category, state.currencyCode) }

            item {
                Column {
                    state.untrackedLabels.forEach { label ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
                            Text("Not tracked in this version", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        }
                        Spacer(Modifier.height(LedgerSpacing.Tiny))
                    }
                }
            }

            item {
                SectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Calculated Total", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
                        Text(
                            formatMoney(state.calculatedTotalMinor, state.currencyCode),
                            style = LedgerTextStyles.Label,
                            color = LedgerTheme.colors.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(LedgerSpacing.Tiny))
                    if (state.mismatchMinor != 0L) {
                        Text(
                            "MISMATCH: Dashboard and the traced calculation disagree by " +
                                formatMoney(state.mismatchMinor, state.currencyCode) +
                                " — this should never happen; both read the same AccountBalanceService.",
                            style = LedgerTextStyles.Caption,
                            color = LedgerTheme.colors.negative,
                        )
                    } else {
                        Text(
                            "Matches the Dashboard exactly — no unexplained value.",
                            style = LedgerTextStyles.Caption,
                            color = LedgerTheme.colors.positive,
                        )
                    }
                }
            }

            item { SectionTitle("Included Accounts (Balance Trace)") }
            items(state.includedAccounts) { account -> IncludedAccountRowView(account, state.currencyCode) }

            if (state.excludedAccounts.isNotEmpty()) {
                item { SectionTitle("Excluded Accounts") }
                items(state.excludedAccounts) { account -> ExcludedAccountRowView(account) }
            }

            if (state.nonPrimaryCurrencyAccounts.isNotEmpty()) {
                item { SectionTitle("Other-Currency Accounts (not converted, not mixed in)") }
                items(state.nonPrimaryCurrencyAccounts) { account -> NonPrimaryCurrencyRowView(account) }
            }

            if (state.candidateAccounts.isNotEmpty()) {
                item { SectionTitle("Candidate Accounts — unrecognized institution, awaiting review") }
                items(state.candidateAccounts) { account ->
                    CandidateAccountRowView(
                        account = account,
                        onPromote = { viewModel.promoteCandidate(account.id) },
                        onDismiss = { viewModel.dismissCandidate(account.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(LedgerSpacing.XxLarge)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
        color = LedgerTheme.colors.tertiaryLabel,
        modifier = Modifier.padding(vertical = LedgerSpacing.Tiny),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(LedgerSpacing.Medium),
    ) {
        content()
    }
}

@Composable
private fun CategoryRowView(category: CategoryRow, currency: CurrencyCode) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(category.label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
        // Liability convention: a positive "amount owed" balance is shown as
        // "- AED X" because it SUBTRACTS from net worth (see AccountType.isLiability) —
        // that's a display convention, not a claim the stored value is negative.
        // Asset convention: show the REAL signed value. Unconditionally taking
        // abs() here for asset categories was a real bug — it would silently
        // show an overdrawn (negative) checking/cash balance as positive,
        // exactly the class of bug Part 5 already fixed once on the Dashboard.
        val display = if (category.isLiability) {
            "- " + formatMoney(kotlin.math.abs(category.balanceMinor), currency)
        } else {
            formatMoney(category.balanceMinor, currency)
        }
        val isNegativeDisplay = category.isLiability || category.balanceMinor < 0
        Text(
            display,
            style = LedgerTextStyles.BodyMedium,
            color = if (isNegativeDisplay) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun IncludedAccountRowView(account: IncludedAccountRow, currency: CurrencyCode) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(LedgerSpacing.Small),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
            Text(
                formatMoney(account.balanceMinor, currency),
                style = LedgerTextStyles.Label,
                color = if (account.balanceMinor < 0) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Type: ${account.type} · id=${account.id}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text(
                "Running total: ${formatMoney(account.runningTotalMinor, currency)}",
                style = LedgerTextStyles.Caption,
                fontFamily = FontFamily.Monospace,
                color = LedgerTheme.colors.tertiaryLabel,
            )
        }
    }
}

@Composable
private fun ExcludedAccountRowView(account: ExcludedAccountRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(LedgerSpacing.Small),
    ) {
        Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        Text("Type: ${account.type} · id=${account.id}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Text(account.reason, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.attention)
    }
}

@Composable
private fun NonPrimaryCurrencyRowView(account: NonPrimaryCurrencyRow) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(LedgerSpacing.Small),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
            Text(formatMoney(account.balanceMinor, account.currencyCode), style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        }
        Text(
            "Currency differs from the primary total above — never converted, never mixed in",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.attention,
        )
    }
}

@Composable
private fun CandidateAccountRowView(
    account: CandidateAccountRow,
    onPromote: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(LedgerSpacing.Small),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
            Text(formatMoney(account.balanceMinor, account.currencyCode), style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        }
        Text(
            "Type: ${account.type} — institution not recognized, never merged into any other account",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
            TextButton(onClick = onPromote) { Text("Promote to real account") }
        }
    }
}

private fun formatMoney(minorUnits: Long, currency: CurrencyCode): String =
    "${currency.name} ${formatSignedPlainDecimal(minorUnits, currency)}"

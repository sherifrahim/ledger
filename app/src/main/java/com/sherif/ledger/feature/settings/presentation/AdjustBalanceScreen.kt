package com.sherif.ledger.feature.settings.presentation

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerAmountInputField
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.util.formatSignedPlainDecimal
import com.sherif.ledger.core.domain.util.parsePlainDecimalToMinor
import com.sherif.ledger.feature.settings.presentation.viewmodel.AdjustBalanceAccountUi
import com.sherif.ledger.feature.settings.presentation.viewmodel.AdjustBalanceViewModel

/**
 * Reachable anytime from Profile → "Adjust Starting Balance" — see
 * SeedOpeningBalanceUseCase's doc comment for why this exists: a bounded
 * historical-import window has no way to know what an account already held
 * before the window started, so the computed balance is only ever as
 * accurate as the last correction made here (or during onboarding).
 */
@Composable
fun AdjustBalanceScreen(
    onBackClick: () -> Unit = {},
    viewModel: AdjustBalanceViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val trackedSince by viewModel.trackedSince.collectAsState()
    val entries = remember { mutableStateMapOf<Long, String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick, contentDescription = "Back")
        }

        Spacer(Modifier.height(LedgerSpacing.Large))
        Text(
            text = "Adjust Starting Balance",
            style = LedgerTextStyles.Headline,
            color = LedgerTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = "If an account's balance doesn't match your bank (common after importing only part of your history), " +
                "enter its real current balance here — Ledger will use it going forward.",
            style = LedgerTextStyles.BodyMedium,
            color = LedgerTheme.colors.textSecondary,
        )

        trackedSince?.let {
            Spacer(Modifier.height(LedgerSpacing.Small))
            Text(
                text = "Ledger tracked: $it",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textTertiary,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Large))

        accounts.forEach { account ->
            Text(
                text = account.accountName,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(LedgerSpacing.Tiny))
            ReconciliationCard(account)
            Spacer(Modifier.height(LedgerSpacing.Small))
            LedgerAmountInputField(
                value = entries[account.accountId] ?: "",
                onValueChange = { entries[account.accountId] = it },
                currencySymbol = CurrencyRegistry.get(account.currencyCode).symbol,
                placeholder = formatSignedPlainDecimal(account.computedBalanceMinor, account.currencyCode),
            )
            Spacer(Modifier.height(LedgerSpacing.Large))
        }

        if (saved) {
            Text(
                text = "Saved.",
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.positive,
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
        }

        Spacer(Modifier.weight(1f))

        LedgerButton(
            text = "Save",
            onClick = {
                val corrections = accounts.mapNotNull { account ->
                    val decimalDigits = CurrencyRegistry.get(account.currencyCode).decimalDigits
                    parsePlainDecimalToMinor(entries[account.accountId], decimalDigits)?.let { account.accountId to it }
                }.toMap()
                entries.clear()
                viewModel.applyCorrections(corrections)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LedgerSpacing.Medium))
    }
}

/**
 * Decomposes an account's balance so the figure is explainable rather than a
 * black box: opening balance (what was there before tracking) + captured net
 * (everything Ledger recorded since) = Ledger's current figure. Makes an
 * off balance self-diagnosing — e.g. an opening balance of 0 shows immediately.
 */
@Composable
private fun ReconciliationCard(account: AdjustBalanceAccountUi) {
    LedgerSurface(
        level = LedgerSurfaceLevel.Inset,
        shape = LedgerRadius.Large,
        contentPadding = PaddingValues(LedgerSpacing.Medium),
    ) {
        Column {
            ReconRow("Opening balance", account.openingBalanceMinor, account.currencyCode)
            Spacer(Modifier.height(LedgerSpacing.Small))
            ReconRow("Captured since", account.capturedNetMinor, account.currencyCode, showPlus = true)
            Spacer(Modifier.height(LedgerSpacing.Small))
            LedgerDivider(alpha = 0.08f)
            Spacer(Modifier.height(LedgerSpacing.Small))
            ReconRow("Ledger's figure", account.computedBalanceMinor, account.currencyCode, emphasize = true)
        }
    }
}

@Composable
private fun ReconRow(
    label: String,
    valueMinor: Long,
    currency: CurrencyCode,
    showPlus: Boolean = false,
    emphasize: Boolean = false,
) {
    val number = formatSignedPlainDecimal(valueMinor, currency)
    val display = if (showPlus && valueMinor > 0) "+$number" else number
    val color = if (emphasize) LedgerTheme.colors.textPrimary else LedgerTheme.colors.textSecondary
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTextStyles.BodyMedium, color = color)
        Text(
            "${CurrencyRegistry.get(currency).symbol} $display",
            style = LedgerTextStyles.BodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = color,
        )
    }
}

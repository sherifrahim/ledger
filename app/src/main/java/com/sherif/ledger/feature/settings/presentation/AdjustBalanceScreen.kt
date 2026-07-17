package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerAmountInputField
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.util.formatSignedPlainDecimal
import com.sherif.ledger.core.domain.util.parsePlainDecimalToMinor
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
            LedgerIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
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

        Spacer(Modifier.height(LedgerSpacing.Large))

        accounts.forEach { account ->
            Text(
                text = account.accountName,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.textSecondary,
            )
            Spacer(Modifier.height(LedgerSpacing.Tiny))
            LedgerAmountInputField(
                value = entries[account.accountId] ?: "",
                onValueChange = { entries[account.accountId] = it },
                currencySymbol = CurrencyRegistry.get(account.currencyCode).symbol,
                placeholder = formatSignedPlainDecimal(account.computedBalanceMinor, account.currencyCode),
            )
            Spacer(Modifier.height(LedgerSpacing.Medium))
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

package com.sherif.ledger.feature.onboarding.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerAmountInputField
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.calendar.LedgerDateRangeCalendar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.util.formatSignedPlainDecimal
import com.sherif.ledger.core.domain.util.parsePlainDecimalToMinor
import com.sherif.ledger.feature.onboarding.presentation.viewmodel.SmsOnboardingViewModel
import java.time.format.DateTimeFormatter

@Composable
fun SmsOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SmsOnboardingViewModel = hiltViewModel()
) {
    val hasConfirmedRange by viewModel.hasConfirmedRange.collectAsState()
    val showBalanceConfirmation by viewModel.showBalanceConfirmation.collectAsState()

    when {
        showBalanceConfirmation -> BalanceConfirmationScreen(viewModel, onComplete)
        !hasConfirmedRange -> ImportRangeSelectionScreen(viewModel)
        else -> ImportScanScreen(viewModel, onComplete)
    }
}

/**
 * Part 2: shown once, on first launch, before the historical SMS import ever
 * runs. Nothing is scanned until the user confirms a window here — the
 * previous behavior (scanning the entire SMS inbox with no lower or upper
 * bound) is what Part 1's diagnostic bundle traced the inflated Dashboard
 * numbers back to.
 */
@Composable
private fun ImportRangeSelectionScreen(viewModel: SmsOnboardingViewModel) {
    val selected by viewModel.selectedRangeOption.collectAsState()
    val customStart by viewModel.customStartDate.collectAsState()
    val customEnd by viewModel.customEndDate.collectAsState()
    val canContinue = viewModel.canContinue()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
    ) {
        Spacer(Modifier.height(LedgerSpacing.XLarge))

        Text(
            text = "Import Existing SMS Transactions",
            style = LedgerTextStyles.Headline,
            color = LedgerTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = "Choose how much history Ledger should import.",
            style = LedgerTextStyles.BodyMedium,
            color = LedgerTheme.colors.textSecondary,
        )

        Spacer(Modifier.height(LedgerSpacing.Large))

        ImportRangeOption.values().forEach { option ->
            RangeOptionRow(
                label = if (option == ImportRangeOption.THIS_WEEK) "${option.label} (Recommended)" else option.label,
                isSelected = selected == option,
                onClick = { viewModel.selectRangeOption(option) },
            )
            Spacer(Modifier.height(LedgerSpacing.Tiny))
        }

        if (selected == ImportRangeOption.CUSTOM) {
            Spacer(Modifier.height(LedgerSpacing.Small))
            LedgerDateRangeCalendar(
                rangeStart = customStart,
                rangeEnd = customEnd,
                onRangeChange = { start, end -> viewModel.setCustomRange(start, end) },
                modifier = Modifier.padding(vertical = LedgerSpacing.Small),
            )
            val formatter = DateTimeFormatter.ofPattern("d MMM yyyy")
            if (customStart != null) {
                Text(
                    text = if (customEnd != null) {
                        "${customStart!!.format(formatter)} – ${customEnd!!.format(formatter)}"
                    } else {
                        "${customStart!!.format(formatter)} – select end date"
                    },
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        LedgerButton(
            text = "Continue",
            onClick = { viewModel.confirmRange() },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LedgerSpacing.Medium))
    }
}

@Composable
private fun RangeOptionRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LedgerRadius.Medium)
            .clickable(onClick = onClick)
            .padding(vertical = LedgerSpacing.Small, horizontal = LedgerSpacing.Tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) LedgerTheme.colors.textPrimary else LedgerTheme.colors.border,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LedgerTheme.colors.textPrimary),
                )
            }
        }
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(
            text = label,
            style = LedgerTextStyles.BodyLarge,
            color = LedgerTheme.colors.textPrimary,
        )
    }
}

/** The pre-existing permission-request / scan / result flow, unchanged except that it now runs against the confirmed range above. */
@Composable
private fun ImportScanScreen(viewModel: SmsOnboardingViewModel, onComplete: () -> Unit) {
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startImport()
        } else {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Sms,
            contentDescription = null,
            tint = LedgerTheme.colors.tint,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(LedgerSpacing.Large))

        Text(
            text = "Import Transaction History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = LedgerTheme.colors.label,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(LedgerSpacing.Medium))

        Text(
            text = "Ledger can scan your existing bank SMS alerts to build your financial history instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = LedgerTheme.colors.secondaryLabel,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(LedgerSpacing.Massive))

        when {
            isImporting -> {
                CircularProgressIndicator(color = LedgerTheme.colors.tint)
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Text("Scanning inbox...", color = LedgerTheme.colors.secondaryLabel)
            }
            importResult != null -> {
                val message = when (importResult) {
                    -1 -> "Import failed. You can try again later from settings."
                    0 -> "No transaction SMS found in your inbox."
                    else -> "Imported $importResult message(s) from your inbox."
                }
                Text(
                    text = message,
                    color = LedgerTheme.colors.label,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Button(
                    onClick = { viewModel.proceedPastImport(onComplete) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.tint)
                ) {
                    Text("Continue")
                }
            }
            else -> {
                Button(
                    onClick = { launcher.launch(android.Manifest.permission.READ_SMS) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.tint)
                ) {
                    Text("Scan Inbox")
                }

                TextButton(
                    onClick = { viewModel.skipImport(onComplete) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now", color = LedgerTheme.colors.tertiaryLabel)
                }
            }
        }
    }
}

/**
 * Part 2 follow-up: shown once, right after a successful scan, before the
 * user ever sees a number on the Dashboard. See `SeedOpeningBalanceUseCase`
 * for why this exists — a bounded import window has no way to know what an
 * account already held before the window started, so the computed balance
 * is only correct if the user confirms (or corrects) it here.
 */
@Composable
private fun BalanceConfirmationScreen(viewModel: SmsOnboardingViewModel, onComplete: () -> Unit) {
    val accounts by viewModel.balanceConfirmationAccounts.collectAsState()
    val entries = remember { mutableStateMapOf<Long, String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
    ) {
        // The account list scrolls INSIDE this region while the actions below stay
        // pinned. Previously everything sat in one unscrollable Column, so a user
        // with several accounts (a current account plus a few credit cards) had the
        // "Save & Finish" button pushed off-screen with no way to reach it —
        // onboarding could not be completed at all.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(LedgerSpacing.XLarge))
            Text(
                text = "Confirm Starting Balance",
                style = LedgerTextStyles.Headline,
                color = LedgerTheme.colors.textPrimary,
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
            Text(
                text = "Ledger only imported the window you chose, so it doesn't know what you already had before that. " +
                    "Enter your real current balance for each account to get this right — or leave it blank to skip.",
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
                // Show what Ledger computed from the imported window, so the correction
                // is transparent: the user sees the machine's figure and enters the real
                // one, and the difference is recorded as the balance held before the window.
                Text(
                    text = "Ledger calculated ${formatSignedPlainDecimal(account.computedBalanceMinor, account.currencyCode)} " +
                        "from your imported messages. Enter your real balance and Ledger records the difference as your starting point.",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.textTertiary,
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
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))

        LedgerButton(
            text = "Save & Finish",
            onClick = {
                val corrections = accounts.mapNotNull { account ->
                    val decimalDigits = CurrencyRegistry.get(account.currencyCode).decimalDigits
                    parsePlainDecimalToMinor(entries[account.accountId], decimalDigits)?.let { account.accountId to it }
                }.toMap()
                viewModel.confirmBalances(corrections, onComplete)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        TextButton(
            onClick = { viewModel.confirmBalances(emptyMap(), onComplete) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Skip, I'll fix this later", color = LedgerTheme.colors.tertiaryLabel)
        }
        Spacer(Modifier.height(LedgerSpacing.Medium))
    }
}

@Composable
fun rememberCoroutineOf() = rememberCoroutineScope()

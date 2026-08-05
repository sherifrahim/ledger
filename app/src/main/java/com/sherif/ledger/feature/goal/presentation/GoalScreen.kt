package com.sherif.ledger.feature.goal.presentation

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAmountInputField
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerTextField
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.GoalProgress
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.core.domain.util.parsePlainDecimalToMinor
import com.sherif.ledger.feature.goal.presentation.viewmodel.FundingAccountUi
import com.sherif.ledger.feature.goal.presentation.viewmodel.GoalUiState
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

/**
 * Goals — what the user is saving towards, measured by the funding account's
 * real balance rather than by a number they have to keep updating.
 */
@Composable
fun GoalScreen(
    state: GoalUiState,
    onBackClick: () -> Unit = {},
    onAddGoal: (name: String, targetMinor: Long, accountId: Long, currency: CurrencyCode) -> Unit = { _, _, _, _ -> },
    onRemoveGoal: (Long) -> Unit = {},
) {
    var adding by remember { mutableStateOf(false) }

    if (adding) {
        GoalEditorDialog(
            accounts = state.fundingAccounts,
            onDismiss = { adding = false },
            onConfirm = { name, minor, accountId, currency ->
                onAddGoal(name, minor, accountId, currency)
                adding = false
            },
        )
    }

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding,
                end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
        ) {
            item {
                LedgerScreenHeader(title = "Goals", onBackClick = onBackClick)
            }

            if (state.goals.isEmpty()) {
                item {
                    LedgerEmptyState(
                        title = "No goals yet",
                        subtitle = "Pick an account and a target, and Ledger tracks it using that " +
                            "account's real balance — there is nothing to update by hand.",
                        icon = Icons.Outlined.Savings,
                        modifier = Modifier.padding(top = LedgerSpacing.Large),
                    )
                }
            } else {
                items(state.goals, key = { it.goal.id }) { progress ->
                    GoalRow(progress = progress, onRemove = { onRemoveGoal(progress.goal.id) })
                }
            }

            if (state.fundingAccounts.isNotEmpty()) {
                item {
                    LedgerButton(
                        text = "Add a goal",
                        onClick = { adding = true },
                        style = LedgerButtonStyle.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalRow(progress: GoalProgress, onRemove: () -> Unit) {
    val colors = LedgerTheme.colors
    LedgerCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    progress.goal.name,
                    style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                // Naming the account is not decoration: it is what makes the number
                // above it checkable against the Accounts screen.
                Text(
                    "Funded by ${progress.accountName}",
                    style = LedgerTextStyles.Caption,
                    color = colors.textTertiary,
                    maxLines = 1,
                )
            }
            Text(
                if (progress.isReached) "Reached" else "${(progress.fraction * 100).toInt()}%",
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.SemiBold),
                color = if (progress.isReached) colors.positive else colors.textSecondary,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Small))

        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.surfaceInset),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (progress.isReached) colors.positive else colors.accent),
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Tiny))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (progress.isReached) {
                    "${MoneyFormatter.format(progress.saved, includeSymbol = true)} saved"
                } else {
                    "${MoneyFormatter.format(progress.saved, includeSymbol = true)} of " +
                        MoneyFormatter.format(progress.goal.target, includeSymbol = true)
                },
                style = LedgerTextStyles.Caption,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Remove",
                style = LedgerTextStyles.Caption,
                color = colors.textTertiary,
                modifier = Modifier.ledgerClickable(onClick = onRemove),
            )
        }
    }
}

@Composable
private fun GoalEditorDialog(
    accounts: List<FundingAccountUi>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Long, CurrencyCode) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(accounts.firstOrNull()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New goal") },
        text = {
            Column {
                LedgerTextField(value = name, onValueChange = { name = it }, placeholder = "e.g. Emergency fund")
                Spacer(Modifier.height(LedgerSpacing.Medium))
                LedgerAmountInputField(
                    value = target,
                    onValueChange = { target = it },
                    currencySymbol = selected?.currency?.name ?: "AED",
                    placeholder = "20000", // LedgerAmountInputField adds the "e.g." itself
                )
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Text("Funded by", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                Spacer(Modifier.height(LedgerSpacing.Tiny))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny)) {
                    items(accounts, key = { it.id }) { account ->
                        val isSelected = selected?.id == account.id
                        Text(
                            account.name,
                            style = LedgerTextStyles.Label,
                            color = if (isSelected) LedgerTheme.colors.surfaceBase else LedgerTheme.colors.textPrimary,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) LedgerTheme.colors.textPrimary else LedgerTheme.colors.surfaceInset,
                                )
                                .ledgerClickable { selected = account }
                                .padding(horizontal = LedgerSpacing.Small, vertical = 6.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            LedgerButton(
                text = "Create",
                onClick = {
                    val account = selected
                    val minor = parsePlainDecimalToMinor(target, decimalDigits = 2)
                    // Every field is required for the goal to mean anything, so an
                    // incomplete form closes rather than creating half a goal.
                    if (account != null && minor != null && name.isNotBlank()) {
                        onConfirm(name.trim(), minor, account.id, account.currency)
                    } else {
                        onDismiss()
                    }
                },
                style = LedgerButtonStyle.Solid,
            )
        },
        dismissButton = { LedgerButton(text = "Cancel", onClick = onDismiss, style = LedgerButtonStyle.Ghost) },
    )
}

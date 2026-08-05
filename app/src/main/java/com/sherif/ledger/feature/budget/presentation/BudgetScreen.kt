package com.sherif.ledger.feature.budget.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountInputField
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.domain.model.BudgetStatus
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.core.domain.util.parsePlainDecimalToMinor
import com.sherif.ledger.feature.budget.presentation.viewmodel.BudgetUiState
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

/**
 * Budgets — a monthly ceiling per category, and how close this month is to it.
 *
 * Every "spent" figure comes from the same analytics pass that drives Insights,
 * so this screen can never disagree with the spending breakdown about what went
 * on groceries. See GetBudgetStatusUseCase.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    state: BudgetUiState,
    onBackClick: () -> Unit = {},
    onSetBudget: (category: String, limitMinor: Long, currency: CurrencyCode) -> Unit = { _, _, _ -> },
    onRemoveBudget: (String) -> Unit = {},
) {
    var editing by remember { mutableStateOf<String?>(null) }

    editing?.let { category ->
        BudgetEditorDialog(
            category = category,
            currency = state.currency,
            onDismiss = { editing = null },
            onConfirm = { minor ->
                onSetBudget(category, minor, state.currency)
                editing = null
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
                LedgerScreenHeader(title = "Budgets", subtitle = "This month", onBackClick = onBackClick)
            }

            if (state.statuses.isEmpty()) {
                item {
                    LedgerEmptyState(
                        title = "No budgets yet",
                        subtitle = "Set a monthly ceiling for a category and Ledger will track it " +
                            "against what you actually spend — using the same figures as your " +
                            "spending breakdown, so the two can never disagree.",
                        icon = Icons.Outlined.PieChart,
                        modifier = Modifier.padding(top = LedgerSpacing.Large),
                    )
                }
            } else {
                items(state.statuses, key = { it.budget.category }) { status ->
                    BudgetRow(
                        status = status,
                        onEdit = { editing = status.budget.category },
                        onRemove = { onRemoveBudget(status.budget.category) },
                    )
                }
            }

            if (state.suggestedCategories.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    Text(
                        "ADD A BUDGET",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = LedgerTheme.colors.textTertiary,
                    )
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    // Only categories the user actually spends in. Listing the whole
                    // enum would offer ceilings for things they have never bought.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
                        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
                    ) {
                        state.suggestedCategories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .border(LedgerTheme.border.Hairline, LedgerTheme.colors.cardBorder, CircleShape)
                                    .ledgerClickable { editing = category }
                                    .padding(horizontal = LedgerSpacing.Small, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = LedgerTheme.colors.textSecondary,
                                    modifier = Modifier.width(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    prettyCategory(category),
                                    style = LedgerTextStyles.Label,
                                    color = LedgerTheme.colors.textPrimary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetRow(status: BudgetStatus, onEdit: () -> Unit, onRemove: () -> Unit) {
    val colors = LedgerTheme.colors
    // Over budget is stated in words and by a filled bar, not by colour alone —
    // colour is invisible to a colour-blind user and this is the whole point of
    // the screen.
    val barColor = if (status.isOver) colors.negative else colors.accent

    LedgerCard(onClick = onEdit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                prettyCategory(status.budget.category),
                style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            LedgerAmount(
                amount = MoneyFormatter.format(status.spent, includeSymbol = false),
                currency = status.spent.currencyCode.name,
                style = LedgerAmountStyle.Regular,
                color = if (status.isOver) colors.negative else colors.textPrimary,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Small))

        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(colors.surfaceInset),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(status.fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(barColor),
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Tiny))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (status.isOver) {
                    "Over by ${MoneyFormatter.format(
                        Money(status.spent.minorUnits - status.budget.limit.minorUnits, status.spent.currencyCode),
                        includeSymbol = true,
                    )}"
                } else {
                    "${MoneyFormatter.format(
                        Money(status.remainingMinor, status.spent.currencyCode), includeSymbol = true,
                    )} left of ${MoneyFormatter.format(status.budget.limit, includeSymbol = true)}"
                },
                style = LedgerTextStyles.Caption,
                color = if (status.isOver) colors.negative else colors.textSecondary,
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
private fun BudgetEditorDialog(
    category: String,
    currency: CurrencyCode,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Monthly budget for ${prettyCategory(category)}") },
        text = {
            LedgerAmountInputField(
                value = text,
                onValueChange = { text = it },
                currencySymbol = currency.name,
                placeholder = "1500", // LedgerAmountInputField adds the "e.g." itself
            )
        },
        confirmButton = {
            LedgerButton(
                text = "Set",
                onClick = {
                    // A blank or unparseable entry closes without setting anything;
                    // there is no half-formed budget to warn about.
                    parsePlainDecimalToMinor(text, decimalDigits = 2)?.let(onConfirm) ?: onDismiss()
                },
                style = LedgerButtonStyle.Solid,
            )
        },
        dismissButton = {
            LedgerButton(text = "Cancel", onClick = onDismiss, style = LedgerButtonStyle.Ghost)
        },
    )
}

private fun prettyCategory(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

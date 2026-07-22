package com.sherif.ledger.feature.split.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerTextField
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.split.presentation.viewmodel.SplitViewModel

@Composable
fun SplitScreen(
    onBackClick: () -> Unit = {},
    viewModel: SplitViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    fun money(minor: Long) = MoneyFormatter.format(Money(minor, state.currency), includeSymbol = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(horizontal = LedgerSpacing.Screen),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = LedgerSpacing.Small), verticalAlignment = Alignment.CenterVertically) {
            LedgerIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick, contentDescription = "Back")
        }
        Spacer(Modifier.height(LedgerSpacing.Medium))
        Text("Split", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        Text(
            "${state.merchant} · ${money(state.totalMinor)}",
            style = LedgerTextStyles.BodyMedium,
            color = LedgerTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(LedgerSpacing.Large))

        if (state.loading) {
            Text("Loading…", style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textTertiary)
        } else if (state.hasSplit) {
            SettleMode(state, ::money, onSettle = viewModel::setSettled, onRemove = viewModel::removeSplit)
        } else {
            PickerMode(
                state = state,
                money = ::money,
                onToggle = viewModel::toggleParticipant,
                onAdd = viewModel::addParticipant,
                onSplit = viewModel::createEqualSplit,
            )
        }
    }
}

@Composable
private fun ColumnScope.PickerMode(
    state: SplitUiState,
    money: (Long) -> String,
    onToggle: (String) -> Unit,
    onAdd: (String) -> Unit,
    onSplit: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    val anySelected = state.participants.any { it.selected }

    Text("Who shared this?", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
    Spacer(Modifier.height(LedgerSpacing.Small))

    Column(Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxWidth()) {
            item("self") {
                PersonRow(name = "You", trailing = money(state.yourShareMinor), checked = true, enabled = false, onClick = {})
                LedgerDivider(alpha = 0.05f)
            }
            items(state.participants, key = { it.id }) { p ->
                PersonRow(
                    name = p.name,
                    trailing = if (p.selected) money(p.shareMinor) else "",
                    checked = p.selected,
                    enabled = true,
                    onClick = { onToggle(p.id) },
                )
                LedgerDivider(alpha = 0.05f)
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = "Add a person",
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
            Text(
                "Add",
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                color = if (newName.isBlank()) LedgerTheme.colors.textTertiary else LedgerTheme.colors.system,
                modifier = Modifier
                    .padding(start = LedgerSpacing.Medium)
                    .clickable(enabled = newName.isNotBlank()) { onAdd(newName); newName = "" },
            )
        }
    }

    LedgerButton(
        text = if (anySelected) "Split equally" else "Select who shared this",
        onClick = onSplit,
        enabled = anySelected,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(LedgerSpacing.Medium))
}

@Composable
private fun ColumnScope.SettleMode(
    state: SplitUiState,
    money: (Long) -> String,
    onSettle: (String, Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Text("Who owes you", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
    Spacer(Modifier.height(LedgerSpacing.Small))

    Column(Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxWidth()) {
            items(state.participants, key = { it.id }) { p ->
                PersonRow(
                    name = p.name,
                    trailing = money(p.shareMinor) + if (p.settled) " · settled" else "",
                    checked = p.settled,
                    enabled = p.shareId != null,
                    onClick = { p.shareId?.let { onSettle(it, !p.settled) } },
                )
                LedgerDivider(alpha = 0.05f)
            }
            item("you") {
                Row(Modifier.fillMaxWidth().padding(vertical = LedgerSpacing.Small), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Your share", style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
                    Text(money(state.yourShareMinor), style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
                }
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Outstanding", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
            Text(money(state.outstandingMinor), style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
        }
    }

    LedgerButton(
        text = "Remove split",
        onClick = onRemove,
        style = LedgerButtonStyle.Ghost,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(LedgerSpacing.Medium))
}

@Composable
private fun PersonRow(name: String, trailing: String, checked: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = if (enabled) ({ onClick() }) else null,
            colors = CheckboxDefaults.colors(checkedColor = LedgerTheme.colors.textPrimary),
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(name, style = LedgerTextStyles.BodyLarge, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f).padding(start = LedgerSpacing.Small))
        if (trailing.isNotBlank()) {
            Text(trailing, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
        }
    }
}

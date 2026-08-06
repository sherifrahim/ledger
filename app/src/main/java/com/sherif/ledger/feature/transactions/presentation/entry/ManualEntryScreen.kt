package com.sherif.ledger.feature.transactions.presentation.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerAmountInputField
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerTextField
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.core.domain.util.parsePlainDecimalToMinor
import com.sherif.ledger.feature.transactions.presentation.entry.viewmodel.ManualAccountOption
import com.sherif.ledger.feature.transactions.presentation.entry.viewmodel.ManualEntryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    onBackClick: () -> Unit = {},
    onSaved: () -> Unit = {},
    viewModel: ManualEntryViewModel = hiltViewModel(),
) {
    val accounts by viewModel.accounts.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val error by viewModel.error.collectAsState()

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var transferDirection by remember { mutableStateOf(TransferDirection.OUTGOING) }
    var selectedAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull()) }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(saved) { if (saved) onSaved() }

    val account = selectedAccount
    val currency = account?.currency
    val amountMinor = currency?.let { parsePlainDecimalToMinor(amount, CurrencyRegistry.get(it).decimalDigits) }
    val canSave = account != null && amountMinor != null && amountMinor > 0L && !saving

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick, contentDescription = "Back")
        }
        Spacer(Modifier.height(LedgerSpacing.Large))
        Text("Add transaction", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            "Record something Ledger didn't capture automatically — cash, a transfer, anything without a bank alert.",
            style = LedgerTextStyles.BodyMedium,
            color = LedgerTheme.colors.textSecondary,
        )

        Spacer(Modifier.height(LedgerSpacing.Large))

        FieldLabel("Type")
        Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
            SelectChip("Expense", selected = selectedType == TransactionType.EXPENSE) { selectedType = TransactionType.EXPENSE }
            SelectChip("Income", selected = selectedType == TransactionType.INCOME) { selectedType = TransactionType.INCOME }
            SelectChip("Transfer", selected = selectedType == TransactionType.TRANSFER) { selectedType = TransactionType.TRANSFER }
        }

        if (selectedType == TransactionType.TRANSFER) {
            Spacer(Modifier.height(LedgerSpacing.Medium))
            FieldLabel("Direction")
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                SelectChip("Sent", selected = transferDirection == TransferDirection.OUTGOING) { transferDirection = TransferDirection.OUTGOING }
                SelectChip("Received", selected = transferDirection == TransferDirection.INCOMING) { transferDirection = TransferDirection.INCOMING }
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Large))

        FieldLabel("Amount")
        LedgerAmountInputField(
            value = amount,
            onValueChange = { amount = it },
            currencySymbol = currency?.let { CurrencyRegistry.get(it).symbol } ?: "",
            placeholder = "0.00",
        )

        Spacer(Modifier.height(LedgerSpacing.Large))

        FieldLabel("Description")
        LedgerTextField(value = description, onValueChange = { description = it }, placeholder = "e.g. Cash lunch")

        if (accounts.size > 1) {
            Spacer(Modifier.height(LedgerSpacing.Large))
            FieldLabel("Account")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                items(accounts, key = { it.id }) { opt ->
                    SelectChip(opt.name, selected = opt.id == account?.id) { selectedAccount = opt }
                }
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Large))
        FieldLabel("Date")
        val dateLabel = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(LedgerRadius.Medium)
                .background(LedgerTheme.colors.surfaceLevel1)
                .clickable { showDatePicker = true }
                .padding(LedgerSpacing.Small),
        ) {
            Text(dateLabel, style = LedgerTextStyles.BodyLarge, color = LedgerTheme.colors.textPrimary)
        }

        error?.let {
            Spacer(Modifier.height(LedgerSpacing.Medium))
            Text(it, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.negative)
        }

        Spacer(Modifier.weight(1f))

        LedgerButton(
            text = if (saving) "Saving…" else "Save transaction",
            onClick = {
                val acc = account ?: return@LedgerButton
                val minor = amountMinor ?: return@LedgerButton
                viewModel.save(
                    accountId = acc.id,
                    amountMinor = minor,
                    currency = acc.currency,
                    type = selectedType,
                    timestamp = Instant.ofEpochMilli(dateMillis),
                    description = description,
                    transferDirection = if (selectedType == TransactionType.TRANSFER) transferDirection else null,
                )
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LedgerSpacing.Medium))
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
    Spacer(Modifier.height(LedgerSpacing.Tiny))
}

@Composable
private fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LedgerTheme.colors.textPrimary else LedgerTheme.colors.surfaceInset
    val fg = if (selected) LedgerTheme.colors.surfaceBase else LedgerTheme.colors.textPrimary
    Text(
        text = label,
        style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        modifier = Modifier
            .clip(LedgerRadius.Full)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Small),
    )
}

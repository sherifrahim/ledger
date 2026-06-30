package com.sherif.ledger.feature.transactions.presentation.detail

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.MerchantHeader
import com.sherif.ledger.core.designsystem.component.layout.LedgerMetadataRow
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.haptics.LedgerHaptics
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.core.preview.PreviewTransactions

private val previewState = run {
    val t = PreviewTransactions.amazonToday
    TransactionDetailsUiState(
        merchant = t.merchant.name,
        merchantCategory = t.merchant.category,
        merchantAccentHue = t.merchant.accentHue,
        amount = t.amount,
        sign = if (t.isIncome) "+" else "-",
        isIncome = t.isIncome,
        date = t.date,
        time = t.time,
        status = t.status,
        paymentMethod = t.paymentMethod,
        accountName = t.account.name,
        accountNumber = t.account.accountNumber,
        reference = t.reference,
        history = listOf(
            MerchantHistoryItem("AED 89", "20 Jun"),
            MerchantHistoryItem("AED 245", "14 Jun"),
            MerchantHistoryItem("AED 32", "3 Jun"),
        ),
        notes = "Weekly groceries and home essentials from Amazon Fresh.",
    )
}

@Composable
fun TransactionDetailsScreen(
    onBackClick: () -> Unit = {},
    state: TransactionDetailsUiState = previewState,
) {
    var showMenu by remember { mutableStateOf(false) }
    val haptics = LedgerHaptics.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item("nav") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = LedgerSpacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LedgerTheme.colors.label,
                    modifier = Modifier
                        .size(LedgerTheme.iconSize.Medium)
                        .ledgerClickable { 
                            haptics.selection()
                            onBackClick() 
                        },
                )
                Text("Transaction", style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Box {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = LedgerTheme.colors.label,
                        modifier = Modifier
                            .size(LedgerTheme.iconSize.Medium)
                            .ledgerClickable { 
                                haptics.selection()
                                showMenu = true 
                            },
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(LedgerTheme.colors.surfaceLevel1)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export PDF", style = LedgerTextStyles.Label, color = LedgerTheme.colors.label) },
                            onClick = { 
                                haptics.impact()
                                showMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Report Issue", style = LedgerTextStyles.Label, color = LedgerTheme.colors.label) },
                            onClick = { 
                                haptics.impact()
                                showMenu = false 
                            }
                        )
                    }
                }
            }
        }

        item("header") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
            ) {
                MerchantHeader(
                    name = state.merchant,
                    category = state.merchantCategory,
                    avatarSize = LedgerTheme.iconSize.Huge,
                )
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LedgerAmount(
                        amount = "${state.sign}AED ${state.amount}",
                        style = LedgerAmountStyle.Display,
                        color = if (state.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense,
                    )
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    // Payment Method Pill
                    Box(
                        modifier = Modifier
                            .ledgerSurface(
                                backgroundColor = LedgerTheme.colors.surfaceLevel1,
                                borderColor = Color.Transparent,
                                shape = LedgerRadius.Full,
                            )
                            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.XxSmall),
                    ) {
                        Text(
                            state.paymentMethod,
                            style = LedgerTextStyles.Caption,
                            color = LedgerTheme.colors.secondaryLabel,
                        )
                    }
                }
            }
        }

        item("details") {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                LedgerMetadataRow(label = "Date", value = state.date)
                LedgerMetadataRow(label = "Time", value = state.time)
                LedgerMetadataRow(label = "Category", value = state.merchantCategory)
                LedgerMetadataRow(label = "Status", value = state.status)
                LedgerMetadataRow(label = "Payment method", value = state.paymentMethod)
                LedgerMetadataRow(label = "Reference ID", value = state.reference)
            }
        }

        item("actions") {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                ActionRow(icon = Icons.AutoMirrored.Filled.Notes, label = "Add note")
                ActionRow(icon = Icons.Filled.GridView, label = "Split expense")
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .ledgerClickable { /* TODO */ }
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LedgerTheme.colors.secondaryLabel,
                modifier = Modifier.size(LedgerTheme.iconSize.Small)
            )
            Spacer(Modifier.width(LedgerSpacing.Small))
            Text(label, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = LedgerTheme.colors.tertiaryLabel,
            modifier = Modifier.size(LedgerTheme.iconSize.Small)
        )
    }
}

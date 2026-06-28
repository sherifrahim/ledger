package com.sherif.ledger.feature.accounts.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.components.AccountRow
import com.sherif.ledger.feature.accounts.presentation.preview.AccountsPreviewData

@Composable
fun AccountsScreen(
    state: AccountsUiState = AccountsPreviewData.state,
) {
    var expandedAccountId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(start = LedgerSpacing.Screen, end = LedgerSpacing.Screen, top = LedgerSpacing.XLarge, bottom = LedgerSpacing.ScreenBottom),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth()) {
                Text("Accounts", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.label)
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                Text("Net Worth", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                Spacer(Modifier.height(LedgerSpacing.Inline))
                Text("${state.netWorthCurrency} ${state.netWorth}", style = LedgerTextStyles.Amount, color = Color.White)
                Spacer(Modifier.height(LedgerSpacing.Group))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Assets", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Spacer(Modifier.height(LedgerSpacing.XxSmall))
                        Text(state.assetsTotal, style = LedgerTextStyles.Label, color = LedgerTheme.colors.income)
                    }
                    Column {
                        Text("Liabilities", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Spacer(Modifier.height(LedgerSpacing.XxSmall))
                        Text(state.liabilitiesTotal, style = LedgerTextStyles.Label, color = LedgerTheme.colors.expense)
                    }
                }
            }
        }

        state.sections.forEach { section ->
            item(key = "section_${section.title}") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(section.title, style = LedgerTextStyles.Section, color = LedgerTheme.colors.secondaryLabel)
                        Text(section.total, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                    }
                    LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(0.dp)) {
                        section.accounts.forEachIndexed { index, account ->
                            AccountRow(
                                account = account,
                                expanded = expandedAccountId == account.id,
                                onExpandToggle = { expandedAccountId = if (expandedAccountId == account.id) null else account.id },
                            )
                            if (index != section.accounts.lastIndex) {
                                LedgerHairline(modifier = Modifier.padding(start = 70.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

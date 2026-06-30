package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereGlow
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.components.AccountRow
import com.sherif.ledger.feature.accounts.presentation.preview.AccountsPreviewData
import androidx.compose.foundation.layout.Arrangement

@Composable
fun AccountsScreen(
    state: AccountsUiState = AccountsPreviewData.state,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.Large, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item(key = "title") {
            Text(
                text = "Accounts",
                style = LedgerTextStyles.Headline,
                color = LedgerTheme.colors.label,
                modifier = Modifier.statusBarsPadding(),
            )
        }

        item(key = "total_balance") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .ledgerSurface(level = LedgerSurfaceLevel.Level1),
            ) {
                LedgerAtmosphereGlow(Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Total balance",
                        style = LedgerTextStyles.Caption,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    LedgerAmount(
                        amount = "AED 2,840.25",
                        style = LedgerAmountStyle.Display,
                        color = LedgerTheme.colors.success,
                    )
                }
            }
        }

        state.sections.forEach { section ->
            item(key = "section_${section.title}") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Group)) {
                    LedgerSectionHeader(title = section.title.uppercase())
                    Column(modifier = Modifier.fillMaxWidth()) {
                        section.accounts.forEachIndexed { index, account ->
                            AccountRow(
                                account = account,
                                onClick = { /* TODO */ }
                            )
                            if (index != section.accounts.lastIndex) {
                                LedgerHairline(modifier = Modifier.padding(start = LedgerSpacing.AvatarIndent))
                            }
                        }
                    }
                }
            }
        }

        item(key = "add_account") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .ledgerSurface(level = LedgerSurfaceLevel.Level1)
                    .ledgerClickable { /* TODO */ }
                    .padding(LedgerSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = LedgerTheme.colors.success,
                    modifier = Modifier.size(LedgerTheme.iconSize.Medium)
                )
                Spacer(Modifier.width(LedgerSpacing.Medium))
                Text(
                    text = "Add account",
                    style = LedgerTextStyles.Label,
                    color = LedgerTheme.colors.label
                )
            }
        }
    }
}

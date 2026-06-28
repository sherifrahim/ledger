#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Accounts v1.2 — stateless AccountRow..."

cat > "$B/feature/accounts/presentation/components/AccountRow.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerMotion
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountUi

/**
 * Stateless expandable account row.
 *
 * Expansion state is owned by the caller. This lets the screen hold
 * state today and a ViewModel hold it tomorrow without changing this
 * component. The pattern becomes canonical for every expandable row
 * in Ledger.
 *
 * @param account     Display model for the account.
 * @param expanded    Whether the detail section is visible.
 * @param onExpandToggle Called when the user taps the row.
 * @param logoPainter Optional image painter for merchant/bank logos.
 */
@Composable
fun AccountRow(
    account: AccountUi,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier,
    logoPainter: Painter? = null,
) {
    val accent = Color(account.accentHue)
    val balanceColor = if (account.isNegative) LedgerTheme.colors.expense else LedgerTheme.colors.label

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountAvatar(name = account.name, accent = accent, painter = logoPainter)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
            ) {
                Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(account.subtitle, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            Spacer(Modifier.width(LedgerSpacing.Content))
            Text(account.balance, style = LedgerTextStyles.Label, color = balanceColor)
            Spacer(Modifier.width(LedgerSpacing.Inline))
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = LedgerTheme.colors.tertiaryLabel,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = LedgerMotion.CardSpringDamping,
                    stiffness = LedgerMotion.CardSpringStiffness,
                ),
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = LedgerMotion.CardSpringDamping,
                    stiffness = LedgerMotion.CardSpringStiffness,
                ),
            ),
        ) {
            ExpandedDetail(account)
        }
    }
}

@Composable
private fun ExpandedDetail(account: AccountUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp, end = LedgerSpacing.Group, bottom = LedgerSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
    ) {
        if (account.accountNumber.isNotEmpty()) {
            DetailLine("Account", account.accountNumber)
        }
        if (account.lastActivity.isNotEmpty()) {
            DetailLine("Last activity", account.lastActivity)
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Text(value, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
    }
}

@Composable
private fun AccountAvatar(
    name: String,
    accent: Color,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = LedgerTheme.opacity.Fill)),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            Text(name.take(1).uppercase(), style = LedgerTextStyles.Label, color = accent)
        }
    }
}
EOF

cat > "$B/feature/accounts/presentation/AccountsScreen.kt" << 'EOF'
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
    // Accordion: one row expanded at a time. Null = all collapsed.
    // Today this lives here. Tomorrow it moves to AccountsViewModel.
    var expandedAccountId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen,
            end = LedgerSpacing.Screen,
            top = LedgerSpacing.XLarge,
            bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth()) {
                Text("Accounts", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.label)
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                Text("Net Worth", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                Spacer(Modifier.height(LedgerSpacing.Inline))
                Text(
                    "${state.netWorthCurrency} ${state.netWorth}",
                    style = LedgerTextStyles.Amount,
                    color = Color.White,
                )
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
                    LedgerSurface(
                        level = LedgerSurfaceLevel.Level1,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        section.accounts.forEachIndexed { index, account ->
                            AccountRow(
                                account = account,
                                expanded = expandedAccountId == account.id,
                                onExpandToggle = {
                                    expandedAccountId = if (expandedAccountId == account.id) null else account.id
                                },
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
EOF

echo "Done. 2 files updated."
echo "Run: git add -A && git commit -m 'refactor(accounts): stateless AccountRow, screen-owned accordion state'"
echo "Then: ./gradlew assembleDebug"

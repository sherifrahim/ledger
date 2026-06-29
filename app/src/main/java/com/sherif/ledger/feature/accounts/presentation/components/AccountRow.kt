package com.sherif.ledger.feature.accounts.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerMotion
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountUi

/**
 * Stateless expandable account row with LDL press compression.
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
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = LedgerMotion.CardSpringDamping, stiffness = LedgerMotion.CardSpringStiffness),
        label = "chevron",
    )

    Column(modifier = modifier.fillMaxWidth().ledgerClickable(onClick = onExpandToggle)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerAvatar(name = account.name, color = accent, modifier = Modifier.size(40.dp), painter = logoPainter)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall)) {
                Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(account.subtitle, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            Spacer(Modifier.width(LedgerSpacing.Content))
            Text(account.balance, style = LedgerTextStyles.Label, color = balanceColor)
            Spacer(Modifier.width(LedgerSpacing.Inline))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = LedgerTheme.colors.tertiaryLabel,
                modifier = Modifier.size(18.dp).rotate(chevronRotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)) + fadeIn(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)),
            exit = shrinkVertically(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)) + fadeOut(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 68.dp, end = LedgerSpacing.Group, bottom = LedgerSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
            ) {
                if (account.accountNumber.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Account", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Text(account.accountNumber, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
                    }
                }
                if (account.lastActivity.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last activity", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Text(account.lastActivity, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
                    }
                }
            }
        }
    }
}

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
            enter = expandVertically(animationSpec = spring(dampingRatio = LedgerMotion.CardSpringDamping, stiffness = LedgerMotion.CardSpringStiffness)),
            exit = shrinkVertically(animationSpec = spring(dampingRatio = LedgerMotion.CardSpringDamping, stiffness = LedgerMotion.CardSpringStiffness)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 70.dp, end = LedgerSpacing.Group, bottom = LedgerSpacing.Small),
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

@Composable
private fun AccountAvatar(name: String, accent: Color, modifier: Modifier = Modifier, painter: Painter? = null) {
    Box(
        modifier = modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = LedgerTheme.opacity.Fill)),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            androidx.compose.foundation.Image(painter = painter, contentDescription = name, modifier = Modifier.size(40.dp).clip(CircleShape))
        } else {
            Text(name.take(1).uppercase(), style = LedgerTextStyles.Label, color = accent)
        }
    }
}

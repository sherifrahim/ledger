package com.sherif.ledger.feature.accounts.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerIdentityType
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountUi

/**
 * Standard LDL account row.
 *
 * Refactored to match Screen 5: Icon, Name, Dots, Amount, Type.
 */
@Composable
fun AccountRow(
    account: AccountUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val balanceColor = if (account.isNegative) LedgerTheme.colors.expense else LedgerTheme.colors.label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .padding(vertical = LedgerSpacing.Medium), // Standardized for RC-008
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerBrandIcon(
            name = account.name,
            size = LedgerTheme.iconSize.Large,
            type = LedgerIdentityType.Bank,
        )
        Spacer(Modifier.width(LedgerSpacing.Medium))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall)) {
            Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
            if (account.accountNumber.isNotEmpty()) {
                Text(
                    "•••• ${account.accountNumber.takeLast(4)}",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
            }
        }
        Spacer(Modifier.width(LedgerSpacing.Medium))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall)) {
            LedgerAmount(
                amount = account.balance,
                style = LedgerAmountStyle.Regular, // Standardized for RC-008
                color = balanceColor,
            )
            if (account.subtitle.isNotEmpty()) {
                Text(
                    account.subtitle,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
            }
        }
    }
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL section header for grouped content lists.
 *
 * Title on the left, optional trailing text on the right. Used above
 * inset grouped surfaces to label them.
 */
@Composable
fun LedgerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = LedgerTheme.colors.secondaryLabel,
    trailing: String? = null,
    trailingColor: Color = LedgerTheme.colors.tertiaryLabel,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = LedgerTextStyles.Section, color = titleColor)
        if (trailing != null) {
            Text(
                text = trailing,
                style = LedgerTextStyles.Caption,
                color = if (onTrailingClick != null) LedgerTheme.colors.tint else trailingColor,
                modifier = if (onTrailingClick != null) {
                    Modifier.ledgerClickable(onClick = onTrailingClick)
                } else Modifier,
            )
        }
    }
}

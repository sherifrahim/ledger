package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    titleColor: Color = LedgerTheme.colors.tertiaryLabel,
    trailing: String? = null,
    trailingColor: Color = LedgerTheme.colors.tertiaryLabel,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = LedgerTextStyles.Caption.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = titleColor,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                color = if (onTrailingClick != null) LedgerTheme.colors.tint else trailingColor,
                modifier = if (onTrailingClick != null) {
                    Modifier.ledgerClickable(onClick = onTrailingClick)
                } else Modifier,
            )
        }
    }
}

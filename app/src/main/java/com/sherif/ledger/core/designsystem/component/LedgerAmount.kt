package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

enum class LedgerAmountStyle {
    Small,
    Regular,
    Large,
    Display,
}

/**
 * Ledger V3 Amount Component
 * 
 * Focuses on authoritative typographic alignment without decorative monospace.
 */
@Composable
fun LedgerAmount(
    amount: String,
    modifier: Modifier = Modifier,
    currency: String? = null,
    style: LedgerAmountStyle = LedgerAmountStyle.Regular,
    color: Color = LedgerTheme.colors.textPrimary,
    textAlign: TextAlign = TextAlign.Start,
) {
    val textStyle: TextStyle = when (style) {
        LedgerAmountStyle.Small -> LedgerTextStyles.Label
        LedgerAmountStyle.Regular -> LedgerTextStyles.BodyMedium
        LedgerAmountStyle.Large -> LedgerTextStyles.Title
        LedgerAmountStyle.Display -> LedgerTextStyles.Display
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Atomic)
    ) {
        if (currency != null) {
            Text(
                text = currency,
                style = textStyle.copy(
                    fontSize = textStyle.fontSize * 0.6f,
                    color = LedgerTheme.colors.textSecondary,
                    textAlign = textAlign
                )
            )
        }
        Text(
            text = amount,
            style = textStyle.copy(textAlign = textAlign),
            color = color,
            maxLines = 1,
        )
    }
}

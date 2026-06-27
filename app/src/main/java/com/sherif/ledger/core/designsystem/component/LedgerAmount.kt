package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

enum class LedgerAmountStyle {
    Small,
    Regular,
    Large,
}

/**
 * LDS amount text for money values.
 *
 * The caller provides the final display string so formatting and currency
 * decisions stay in the appropriate feature or domain layer. Direction and
 * status are expressed through [color], drawn from LDL financial semantics.
 */
@Composable
fun LedgerAmount(
    amount: String,
    modifier: Modifier = Modifier,
    style: LedgerAmountStyle = LedgerAmountStyle.Regular,
    color: Color = LedgerTheme.colors.label,
) {
    val textStyle: TextStyle = when (style) {
        LedgerAmountStyle.Small -> LedgerTextStyles.Mono.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        LedgerAmountStyle.Regular -> LedgerTextStyles.Mono
        LedgerAmountStyle.Large -> LedgerTextStyles.Title.copy(fontFamily = FontFamily.Monospace)
    }

    Text(
        text = amount,
        modifier = modifier,
        style = textStyle,
        color = color,
    )
}

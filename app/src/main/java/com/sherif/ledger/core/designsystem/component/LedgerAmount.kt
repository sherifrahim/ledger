package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

enum class LedgerAmountStyle {
    Small,
    Regular,
    Large,
    Display,
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
    textAlign: TextAlign = TextAlign.Start,
) {
    val textStyle: TextStyle = when (style) {
        LedgerAmountStyle.Small -> LedgerTextStyles.Mono.copy(
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        LedgerAmountStyle.Regular -> LedgerTextStyles.Mono.copy(
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
        
        LedgerAmountStyle.Large -> LedgerTextStyles.Amount.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            lineHeight = 32.sp,
        )
        
        LedgerAmountStyle.Display -> LedgerTextStyles.Display.copy(
            fontFamily = FontFamily.Monospace,
        )
    }

    Text(
        text = amount,
        modifier = modifier,
        style = textStyle.copy(textAlign = textAlign),
        color = color,
        maxLines = 1,
    )
}

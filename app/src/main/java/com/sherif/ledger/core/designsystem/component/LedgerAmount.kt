package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
 * A monetary figure with its currency code.
 *
 * Two things this gets right that a plain Row of two Texts does not:
 *
 *  - **The code sits on the number's baseline.** This used to align the two by
 *    `Alignment.Bottom`, which aligns the bottom of each text's *line box* — and
 *    since the code was rendered at 0.6× the amount's style, including 0.6× its
 *    line height, its box was shorter and the code floated visibly below the
 *    digits in every list, card and hero in the app. `alignByBaseline` aligns the
 *    letterforms themselves, which is what "AED 1,568.52" is supposed to mean.
 *  - **The digits are tabular.** Every glyph 0–9 takes the same advance width, so
 *    a column of amounts lines up on the decimal point instead of shimmering as
 *    the values change.
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
        LedgerAmountStyle.Small -> LedgerTextStyles.Label.copy(fontFeatureSettings = "tnum")
        LedgerAmountStyle.Regular -> LedgerTextStyles.AmountRow
        LedgerAmountStyle.Large -> LedgerTextStyles.Title.copy(fontFeatureSettings = "tnum")
        LedgerAmountStyle.Display -> LedgerTextStyles.Display
    }

    // The code tracks the amount's size at roughly the golden ratio, but as a real
    // type size rather than a scaled style, so its line height stays independent.
    val currencyStyle = LedgerTextStyles.AmountCurrency.copy(
        fontSize = (textStyle.fontSize.value * 0.62f).sp,
        lineHeight = textStyle.lineHeight,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Atomic),
    ) {
        if (currency != null) {
            Text(
                text = currency,
                style = currencyStyle,
                color = LedgerTheme.colors.textTertiary,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Text(
            text = amount,
            style = textStyle.copy(textAlign = textAlign),
            color = color,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
    }
}

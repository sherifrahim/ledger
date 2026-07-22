package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single-line text that shrinks to fit its width instead of wrapping.
 *
 * Ledger's large financial figures (Total Balance, hero amounts) must render on ONE
 * line on every device — regardless of screen width or the user's system font-scale
 * (dynamic type). A fixed `sp` size overflows to a second line on narrower phones or
 * larger accessibility text; this steps the font size down until the text fits (down
 * to [minFontSize]), keeping the UI uniform across devices.
 */
@Composable
fun LedgerAutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 22.sp,
) {
    var resolved by remember(text, style) { mutableStateOf(style) }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = resolved,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && resolved.fontSize > minFontSize) {
                resolved = resolved.copy(fontSize = resolved.fontSize * 0.92f)
            }
        },
    )
}

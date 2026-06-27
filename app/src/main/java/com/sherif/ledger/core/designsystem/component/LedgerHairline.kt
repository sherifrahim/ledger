package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LedgerHairline is the LDL separator, replacing Material's Divider.
 *
 * LDL groups content with thin hairlines instead of elevation or heavy rules, so
 * this is intentionally the thinnest visible line. Insets are the caller's
 * responsibility, matching inset grouped lists where separators indent to align
 * with content rather than running to the screen edge.
 */
@Composable
fun LedgerHairline(
    modifier: Modifier = Modifier,
    color: Color = LedgerTheme.colors.separator,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LedgerTheme.border.Hairline)
            .background(color),
    )
}

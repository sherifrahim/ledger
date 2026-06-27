package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LedgerSurface is the fundamental bounded surface in LDL.
 *
 * Unlike a Material Card, a surface communicates hierarchy through a semantic
 * [LedgerSurfaceLevel] resolved by the theme, never through elevation. LDL keeps
 * surfaces flat and separates them with tone and hairlines, matching the Apple
 * grouped-list grammar that anchors Ledger's foundation.
 *
 * Components should request a surface level rather than a raw color, so the
 * meaning of a surface stays stable even when the palette changes. It is built
 * on a Material Surface only as plumbing, with elevation forced flat.
 */
@Composable
fun LedgerSurface(
    modifier: Modifier = Modifier,
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Level1,
    shape: Shape = LedgerRadius.Small,
    hairlineBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerTheme.colors
    val border = if (hairlineBorder) {
        BorderStroke(LedgerTheme.border.Hairline, colors.separator)
    } else {
        null
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.surface(level),
        contentColor = colors.label,
        border = border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

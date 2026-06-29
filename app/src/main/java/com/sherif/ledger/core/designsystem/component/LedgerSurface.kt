package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL foundation for surfaces.
 *
 * Applies the canonical "Carved" aesthetic: clip, tonal background,
 * hairline border, and optional micro-spring interaction.
 */
fun Modifier.ledgerSurface(
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Level1,
    shape: Shape = LedgerRadius.Small,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 0.5.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
): Modifier = composed {
    val colors = LedgerTheme.colors
    val resolvedBackground = backgroundColor ?: colors.surface(level)
    val resolvedBorderColor = borderColor ?: colors.separator.copy(
        alpha = LedgerTheme.motion.SurfaceBorderAlpha,
    )

    this
        .clip(shape)
        .background(resolvedBackground)
        .border(width = borderWidth, color = resolvedBorderColor, shape = shape)
        .then(
            if (onClick != null) {
                Modifier.ledgerClickable(enabled = enabled, onClick = onClick)
            } else Modifier,
        )
}

/**
 * LDL grouped content surface.
 *
 * Replaces Material [androidx.compose.material3.Surface] entirely.
 * Depth comes from three signals:
 * 1. Tonal shift (background color via [level])
 * 2. Hairline border (separator at low opacity)
 * 3. Press compression (when [onClick] is provided)
 *
 * No Material elevation, no Material shadow, no Material ripple.
 * The surface feels carved into the page rather than floating above it.
 */
@Composable
fun LedgerSurface(
    modifier: Modifier = Modifier,
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Level1,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Group),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .ledgerSurface(level = level, onClick = onClick)
            .padding(contentPadding),
        content = content,
    )
}

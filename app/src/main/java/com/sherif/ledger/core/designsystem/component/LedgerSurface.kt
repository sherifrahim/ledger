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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.LedgerThemeType
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL foundation for surfaces.
 *
 * Implements the atomic visual expression for Classic and Glass themes.
 *
 * Classic: The original "Carved" aesthetic with tonal backgrounds and hairlines.
 * Glass: Diffusion-first aesthetic with translucency and vertical edge-lighting.
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
    val isGlass = colors.themeType != LedgerThemeType.Classic

    val resolvedBackground = when {
        backgroundColor != null -> backgroundColor
        isGlass -> colors.glassSurface
        else -> colors.surface(level)
    }

    val resolvedBorderColor = when {
        borderColor != null -> borderColor
        isGlass -> colors.glassBorder
        else -> colors.separator.copy(alpha = LedgerTheme.motion.SurfaceBorderAlpha)
    }

    // Edge Lighting (Glass only): High-intensity top-edge highlight
    val edgeLightModifier = if (isGlass) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                0.0f to colors.edgeLight,
                0.1f to colors.edgeLight.copy(alpha = 0.1f),
                1.0f to Color.Transparent
            ),
            shape = shape
        )
    } else Modifier

    this
        .clip(shape)
        .background(resolvedBackground)
        .then(edgeLightModifier)
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
 */
@Composable
fun LedgerSurface(
    modifier: Modifier = Modifier,
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Level1,
    shape: Shape = LedgerRadius.Small,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Group),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .ledgerSurface(level = level, shape = shape, onClick = onClick)
            .padding(contentPadding),
        content = content,
    )
}

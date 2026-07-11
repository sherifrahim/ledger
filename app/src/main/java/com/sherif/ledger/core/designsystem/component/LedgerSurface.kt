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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Ledger V3 Surface Engine
 * 
 * Implements the "Machined Financial Instrument" material language.
 * Focuses on continuous surfaces, quiet shadows, and architectural sections.
 */
fun Modifier.ledgerSurface(
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Inset,
    shape: Shape = LedgerRadius.Medium,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp = LedgerTheme.border.Hairline,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
): Modifier = composed {
    val colors = LedgerTheme.colors
    val resolvedBackground = backgroundColor ?: colors.surface(level)
    val resolvedBorderColor = borderColor ?: colors.border

    this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
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
 * Ledger V3 Container Primitive
 * 
 * Replaces all V1/V2 card components.
 */
@Composable
fun LedgerSurface(
    modifier: Modifier = Modifier,
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Inset,
    shape: Shape = LedgerRadius.Medium,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Medium),
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .ledgerSurface(
                level = level,
                shape = shape,
                elevation = elevation,
                onClick = onClick
            )
            .padding(contentPadding),
        content = content,
    )
}

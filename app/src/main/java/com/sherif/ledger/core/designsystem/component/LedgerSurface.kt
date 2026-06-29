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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

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
    val shape = LedgerRadius.Small
    val borderColor = LedgerTheme.colors.separator.copy(alpha = 0.20f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .clip(shape)
            .background(LedgerTheme.colors.surface(level))
            .border(width = 0.5.dp, color = borderColor, shape = shape)
            .padding(contentPadding),
        content = content,
    )
}

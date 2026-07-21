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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL Card — the primary content surface of the Ledger Design Language.
 *
 * Card philosophy (Milestone 1.5): information lives on calm, soft-edged cards
 * that *float* rather than shout.
 *  - **Light theme:** the page and the card are both paper-white; the card is
 *    lifted off the page by a single, diffuse shadow plus a whisper hairline —
 *    never a grey fill. This is the "soft surfaces" characteristic of the visual
 *    reference (closer to Things 3 / Apple Wallet than a banking app).
 *  - **Dark theme:** shadows are invisible on near-black, so the card instead
 *    steps one level lighter than the page and leans on a subtle border. Both
 *    themes are treated as first-class.
 *
 * Use [LedgerCard] for standalone feature cards (hero, story, forecast, review
 * items). Keep [LedgerSurface] for nested insets, chips and grouped rows.
 */
@Composable
fun LedgerCard(
    modifier: Modifier = Modifier,
    shape: Shape = LedgerRadius.Large,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Large),
    elevation: Dp = LedgerCardDefaults.Elevation,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerTheme.colors
    // Soft, low-alpha shadow in light; disabled in dark where it would read as mud.
    val shadowMod = if (!colors.isDark && elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = colors.shadowColor,
            spotColor = colors.shadowColor,
        )
    } else Modifier

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(shadowMod)
            .clip(shape)
            .background(colors.surfaceCard)
            .border(LedgerTheme.border.Hairline, colors.cardBorder, shape)
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

object LedgerCardDefaults {
    /** Diffuse resting elevation for content cards (light theme). */
    val Elevation: Dp = 10.dp
    /** A flatter card for dense, stacked contexts (e.g. list rows). */
    val ElevationLow: Dp = 4.dp
}

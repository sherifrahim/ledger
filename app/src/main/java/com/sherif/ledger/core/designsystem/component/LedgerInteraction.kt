package com.sherif.ledger.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations

/**
 * LDL click modifier: no ripple, subtle press compression.
 *
 * On press the surface scales to 0.985 with a micro-spring, then
 * bounces back on release. The compression communicates "this is
 * interactive" without the Material ripple splash.
 *
 * Consumers: LedgerSurface (when onClick provided), AccountRow,
 * ReviewCard, and any future interactive surface.
 */
fun Modifier.ledgerClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.985f else 1.0f,
        animationSpec = LedgerAnimations.microSpring(),
        label = "press_scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}

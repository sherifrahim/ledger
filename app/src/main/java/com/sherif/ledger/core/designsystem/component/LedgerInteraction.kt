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
import com.sherif.ledger.core.designsystem.theme.LedgerMotion

/**
 * LDL click modifier: no ripple, subtle press compression.
 *
 * On press the surface compresses slightly with a micro spring, then
 * settles naturally on release. This replaces the default Material ripple
 * with Ledger's interaction language.
 */
fun Modifier.ledgerClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) LedgerMotion.PressScale else 1.0f,
        animationSpec = LedgerMotion.microSpring(),
        label = "press_scale",
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}
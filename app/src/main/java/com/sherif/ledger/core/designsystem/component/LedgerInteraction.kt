package com.sherif.ledger.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.sherif.ledger.core.designsystem.theme.LedgerMotion
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import kotlinx.coroutines.delay

/**
 * LDL click modifier: no ripple, an iOS-style press.
 *
 * Three things separate this from a plain `clickable` with a scale animation, and
 * all three are what make a press feel like the surface responded rather than like
 * an animation played:
 *
 *  - **Down and up are not the same motion.** Pressing in is quick and nearly
 *    critically damped — the surface should already be down by the time the finger
 *    registers it. Releasing is softer and slower, so it settles rather than snaps.
 *    A single spring for both directions is the classic tell of a press that feels
 *    "cheap": it either rebounds on the way down or feels sluggish on the way up.
 *  - **A press that is over before it is seen still shows.** A fast tap can move
 *    from down to up inside a single frame, so the compression never renders at
 *    all. The pressed state is held for a short floor so every tap is acknowledged.
 *  - **Opacity moves with scale.** A few percent of dimming reads as the surface
 *    taking the touch; scale alone reads as the surface shrinking.
 */
fun Modifier.ledgerClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val haptics = LedgerTheme.haptics
    val interactionSource = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }

    // Held for a minimum duration so a tap shorter than one animation frame still
    // renders its compression instead of silently doing nothing.
    LaunchedEffect(interactionSource) {
        var downAt = 0L
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    downAt = System.currentTimeMillis()
                    pressed = true
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    val held = System.currentTimeMillis() - downAt
                    if (held < LedgerMotion.MinPressVisibleMs) {
                        delay(LedgerMotion.MinPressVisibleMs - held)
                    }
                    pressed = false
                }
            }
        }
    }

    val active = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (active) LedgerMotion.PressScale else 1f,
        animationSpec = if (active) LedgerMotion.pressInSpring() else LedgerMotion.pressOutSpring(),
        label = "press_scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (active) LedgerMotion.PressOpacity else 1f,
        animationSpec = if (active) LedgerMotion.pressInSpring() else LedgerMotion.pressOutSpring(),
        label = "press_alpha",
    )

    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = {
            haptics.selection()
            onClick()
        },
    )
}

package com.sherif.ledger.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Timings for the launch sequence, in milliseconds from process start —
 * isolated here, not hand-tuned inline, so the choreography can be adjusted
 * without touching the animation logic itself. Total default duration is
 * 1600ms, inside the 1.2-1.8s target.
 *
 * Stage shape (Apple Wallet/Music/Pay register, not a "startup logo" beat):
 * icon fades/settles in, a very soft ambient light appears behind it, it
 * holds still, then the whole thing dissolves into the real first screen —
 * no spin, no bounce, no overshoot, anywhere in this file.
 */
data class SplashTimings(
    val iconFadeInStartMs: Int = 150,
    val iconFadeInDurationMs: Int = 400,
    val ambientLightStartMs: Int = 500,
    val ambientLightDurationMs: Int = 500,
    val holdUntilMs: Int = 1200,
    val transitionDurationMs: Int = 400,
) {
    val totalDurationMs: Int get() = holdUntilMs + transitionDurationMs
}

/** cubic-bezier(0.215, 0.61, 0.355, 1) — the standard "ease-out-cubic" curve; imperceptibly gentle, never a hard stop. */
private val EaseOutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f)

/**
 * The launch sequence itself. [onFinished] fires once, after the full
 * timeline completes, so the caller can stop compositing this on top of the
 * real first screen (already rendering underneath — see MainActivity).
 * Content is a single centered mark; no artwork, no gradients on the
 * background itself, per the design brief.
 */
@Composable
fun LedgerSplashScreen(
    modifier: Modifier = Modifier,
    timings: SplashTimings = SplashTimings(),
    onFinished: () -> Unit = {},
) {
    val colors = LedgerTheme.colors
    val backgroundColor = if (colors.isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val accent = Color(0xFF10B981) // Emerald — the same green as the launcher-icon mark; soft ambient light behind it.

    val iconOpacity = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.96f) }
    val ambientOpacity = remember { Animatable(0f) }
    val splashOpacity = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            delay(timings.iconFadeInStartMs.toLong())
            launch { iconOpacity.animateTo(1f, tween(timings.iconFadeInDurationMs, easing = EaseOutCubic)) }
            launch { iconScale.animateTo(1f, tween(timings.iconFadeInDurationMs, easing = EaseOutCubic)) }
        }
        launch {
            delay(timings.ambientLightStartMs.toLong())
            // Low ceiling on purpose — "not a glow," soft light only.
            ambientOpacity.animateTo(0.14f, tween(timings.ambientLightDurationMs, easing = EaseOutCubic))
        }
        delay(timings.holdUntilMs.toLong())
        splashOpacity.animateTo(0f, tween(timings.transitionDurationMs, easing = LinearEasing))
        onFinished()
    }

    // Everything lives in one Box so draw order is explicit: icon/ambient
    // light first, then a full-bleed overlay whose alpha fades the WHOLE
    // scene out at the end — a fade-through dissolve onto whatever the
    // caller has already composed underneath, never a hard cut.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = ambientOpacity.value), Color.Transparent),
                    ),
                ),
        )
        LedgerMark(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer(alpha = iconOpacity.value, scaleX = iconScale.value, scaleY = iconScale.value),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor.copy(alpha = splashOpacity.value)),
        )
    }
}

/**
 * The same three-plate stacked mark as the launcher icon
 * (ic_launcher_foreground.xml), drawn in Compose so it can animate. Emerald
 * glass gradient — brightest on top, deepest on the base plate — so the splash
 * and the launcher icon are a single identity. Coordinates are the icon's own
 * 108x108 viewport, remapped onto this composable's size.
 */
@Composable
private fun LedgerMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.width / 108f

        // One isometric rhombus plate: apex (cx, top), right (cx+hw, mid),
        // bottom (cx, bottom), left (cx-hw, mid). Matches the vector paths.
        fun plate(topY: Float, midY: Float, bottomY: Float): Path = Path().apply {
            moveTo(54f * s, topY * s)
            lineTo(80f * s, midY * s)
            lineTo(54f * s, bottomY * s)
            lineTo(28f * s, midY * s)
            close()
        }

        fun plateBrush(topY: Float, bottomY: Float, top: Color, bottom: Color): Brush =
            Brush.linearGradient(
                colors = listOf(top, bottom),
                start = Offset(28f * s, topY * s),
                end = Offset(80f * s, bottomY * s),
            )

        // Base (deepest) → drawn first so the upper plates overlap it.
        drawPath(
            plate(topY = 55f, midY = 68f, bottomY = 81f),
            brush = plateBrush(55f, 81f, Color(0xFF10B981), Color(0xFF047857)),
        )
        // Middle.
        drawPath(
            plate(topY = 41f, midY = 54f, bottomY = 67f),
            brush = plateBrush(41f, 67f, Color(0xFF34D399), Color(0xFF10B981)),
        )
        // Top (brightest, glassy highlight).
        drawPath(
            plate(topY = 27f, midY = 40f, bottomY = 53f),
            brush = plateBrush(27f, 53f, Color(0xFFA7F3D0), Color(0xFF34D399)),
        )
    }
}

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
    val markColor = if (colors.isDark) Color(0xFFFFFFFF) else Color(0xFF0F0F0F)
    val accent = Color(0xFF3B82F6) // LedgerV3Palette.Azure — same accent as the launcher icon.

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
            color = markColor,
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

/** The same "L" mark as the launcher icon (ic_launcher_foreground.xml), drawn in Compose so it can animate. */
@Composable
private fun LedgerMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Same proportions as the vector icon's path, remapped from its
        // 108x108 viewport onto this composable's own size.
        val scaleX = w / 108f
        val scaleY = h / 108f
        val path = Path().apply {
            moveTo(40f * scaleX, 26f * scaleY)
            lineTo(54f * scaleX, 26f * scaleY)
            lineTo(54f * scaleX, 72f * scaleY)
            lineTo(74f * scaleX, 72f * scaleY)
            lineTo(74f * scaleX, 86f * scaleY)
            lineTo(40f * scaleX, 86f * scaleY)
            close()
        }
        drawPath(path, color = color)
    }
}

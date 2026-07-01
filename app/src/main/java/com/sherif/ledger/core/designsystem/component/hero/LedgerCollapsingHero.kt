package com.sherif.ledger.core.designsystem.component.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.sherif.ledger.core.designsystem.tokens.LedgerGradients

/**
 * Dimension and transition defaults for [LedgerCollapsingHero].
 *
 * Every magic number that controls the hero's structure lives here so the
 * component body stays free of inline constants. Values are hypotheses:
 * adjust after running on a real device, not from a specification.
 */
object LedgerHeroDefaults {

    /** Hero height when fully expanded (scroll offset zero). */
    val ExpandedHeight: Dp = 340.dp // Increased for RC15 responsive layout

    /** Hero height when fully collapsed (pinned compact header). */
    val CollapsedHeight: Dp = 56.dp

    /** Bottom corner radius at full expansion. Reaches 0 dp when compact. */
    val CornerRadius: Dp = 32.dp

    /** Default gradient background. */
    val Background: Brush = LedgerGradients.Emerald
}

/**
 * A reusable scroll-driven collapsing hero surface.
 *
 * The container interpolates its height, corner radius, and background
 * between an expanded and a compact state as [collapseProgress] moves
 * from 0 (expanded) to 1 (compact). It makes no decisions about the
 * content inside: two composable slots receive the current progress so
 * each caller can choreograph its own entry, exit, and internal
 * transitions independently.
 *
 * Both slots are always composed and overlaid. The expanded slot fills
 * the hero. The compact slot is pinned to the top at [collapsedHeight].
 * Callers control visibility (typically via `graphicsLayer { alpha }`)
 * so the container stays agnostic to crossfade timing.
 *
 * This is LDL infrastructure. It carries no business logic, no
 * dashboard state, and no navigation awareness. Screens provide meaning
 * through the slots.
 *
 * @param collapseProgress 0f = fully expanded, 1f = fully compact.
 * @param expandedContent Slot for the full hero layout. Receives progress.
 * @param compactContent  Slot for the pinned compact header. Receives progress.
 */
@Composable
fun LedgerCollapsingHero(
    collapseProgress: Float,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = LedgerHeroDefaults.ExpandedHeight,
    collapsedHeight: Dp = LedgerHeroDefaults.CollapsedHeight,
    cornerRadius: Dp = LedgerHeroDefaults.CornerRadius,
    background: Brush = LedgerHeroDefaults.Background,
    contentBackground: @Composable () -> Unit = {},
    expandedContent: @Composable (collapseProgress: Float) -> Unit,
    compactContent: @Composable (collapseProgress: Float) -> Unit,
) {
    val currentHeight = lerp(expandedHeight, collapsedHeight, collapseProgress)
    val currentRadius = lerp(cornerRadius, 0.dp, collapseProgress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentHeight)
            .clip(
                RoundedCornerShape(
                    bottomStart = currentRadius,
                    bottomEnd = currentRadius,
                ),
            )
            .background(background),
    ) {
        contentBackground()
        Box(Modifier.fillMaxSize()) {
            expandedContent(collapseProgress)
        }
AnimatedVisibility(
    visible = collapseProgress >= 0.85f,
    enter = fadeIn(),
    exit = fadeOut(),
) {
AnimatedVisibility(
    visible = collapseProgress >= HeroTransitions.CompactEnter,
    enter = fadeIn(),
    exit = fadeOut()
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(collapsedHeight)
    ) {
        compactContent(collapseProgress)
    }
}
    }
}
}

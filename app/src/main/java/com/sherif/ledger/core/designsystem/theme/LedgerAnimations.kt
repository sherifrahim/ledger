package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset

/**
 * Canonical animation vocabulary for Ledger.
 *
 * Every animation references these presets. Tuning motion is a
 * single-file change. No inline animation specs anywhere in the app.
 */
object LedgerAnimations {

    // ── Springs ──

    /** Tight spring for icon scale, chip selection, micro-interactions. */
    fun <T> microSpring() = spring<T>(dampingRatio = 0.7f, stiffness = 600f)

    /** Standard spring for cards, expansion, surface transitions. */
    fun <T> standardSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Calm spring for hero collapse, screen settle. */
    fun <T> calmSpring() = spring<T>(
        dampingRatio = LedgerMotion.HeroSpringDamping,
        stiffness = LedgerMotion.HeroSpringStiffness,
    )

    // ── List choreography ──

    /** Standard list item entrance: fade + slight upward slide. */
    fun listEnter(delayMs: Int = 0): EnterTransition =
        fadeIn(tween(LedgerMotion.StandardTweenMs, delayMillis = delayMs)) +
            slideInVertically(tween(LedgerMotion.StandardTweenMs, delayMillis = delayMs)) { it / 8 }

    /** Standard content exit. */
    fun contentExit(): ExitTransition =
        fadeOut(tween(LedgerMotion.FastTweenMs))

    /** Stagger delay for item at [index] in a list. */
    fun staggerDelay(index: Int, baseMs: Int = LedgerMotion.StaggerBaseMs): Int =
        index * baseMs

    // ── Live list mutation ──
    //
    // What happens when a list CHANGES, as opposed to when it first appears.
    // Acting on a Review card removes it; categorising one re-sorts the queue;
    // a capture lands at the top of the feed. Without these the list jumps to its
    // new arrangement between two frames, which reads as a glitch rather than as a
    // consequence of what the user just did — and it is the difference most
    // responsible for an app feeling unfinished while every individual screen
    // looks correct.

    /** Rows sliding to a new position. Springs, so an interrupted change redirects. */
    fun itemPlacement(): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

    /** A row arriving. Slightly slower than the departure so the list settles behind it. */
    fun itemAppear(): FiniteAnimationSpec<Float> = tween(LedgerMotion.Short, easing = LedgerMotion.Standard)

    /** A row leaving. Quick — the user already decided; don't make them watch. */
    fun itemDisappear(): FiniteAnimationSpec<Float> = tween(LedgerMotion.Immediate, easing = LedgerMotion.Standard)

    // ── Navigation transitions ──

    val screenEnter: EnterTransition = LedgerNavigationMotion.StandardEnter

    val screenExit: ExitTransition = LedgerNavigationMotion.StandardExit

    val screenPopEnter: EnterTransition = LedgerNavigationMotion.StandardPopEnter

    val screenPopExit: ExitTransition = LedgerNavigationMotion.StandardPopExit

    /** Sibling-to-sibling movement between bottom-bar tabs — never a push. */
    val tabEnter: EnterTransition = LedgerNavigationMotion.TabEnter

    val tabExit: ExitTransition = LedgerNavigationMotion.TabExit
}

package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically

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

    // ── Navigation transitions ──

    val screenEnter: EnterTransition = LedgerNavigationMotion.StandardEnter

    val screenExit: ExitTransition = LedgerNavigationMotion.StandardExit

    val screenPopEnter: EnterTransition = LedgerNavigationMotion.StandardPopEnter

    val screenPopExit: ExitTransition = LedgerNavigationMotion.StandardPopExit
}

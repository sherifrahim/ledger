package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * LDL motion vocabulary.
 *
 * Every animation in Ledger should reference these instead of local constants.
 * Springs produce natural settling for user-initiated motion, while tweens
 * provide predictable timing for system-initiated events.
 *
 * This file is the single source of truth for motion across Ledger.
 */
object LedgerMotion {

    // -----------------------------------------------------------------------
    // Duration presets (milliseconds)
    // -----------------------------------------------------------------------

    const val Fast = 120
    const val Medium = 220
    const val Slow = 360

    const val FastTweenMs = 150
    const val StandardTweenMs = 250
    const val SlowTweenMs = 400

    /**
     * Base delay between sequential list items.
     *
     * Used for staggered entrance animations throughout the app.
     */
    const val StaggerBaseMs = 40

    // -----------------------------------------------------------------------
    // Spring presets
    // -----------------------------------------------------------------------

    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f

    const val CardSpringDamping = 0.85f
    const val CardSpringStiffness = 400f

    const val ScreenSpringDamping = 0.92f
    const val ScreenSpringStiffness = 200f

    /**
     * Hero transitions.
     *
     * Large surfaces, collapsing headers, balance hero.
     */
    fun heroSpring(): SpringSpec<Float> = spring(
        dampingRatio = HeroSpringDamping,
        stiffness = HeroSpringStiffness,
    )

    /**
     * Card expansion / collapse.
     */
    fun cardSpring(): SpringSpec<Float> = spring(
        dampingRatio = CardSpringDamping,
        stiffness = CardSpringStiffness,
    )

    /**
     * Screen-level transitions.
     */
    fun screenSpring(): SpringSpec<Float> = spring(
        dampingRatio = ScreenSpringDamping,
        stiffness = ScreenSpringStiffness,
    )
}

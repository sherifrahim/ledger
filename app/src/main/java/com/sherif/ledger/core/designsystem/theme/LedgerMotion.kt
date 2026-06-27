package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * LDL motion vocabulary.
 *
 * Every animation in Ledger should reference these instead of local constants.
 * Springs produce natural settle for user-initiated motion. Tweens produce
 * predictable timing for system-initiated events.
 */
object LedgerMotion {
    // Duration constants (ms) for simple references.
    const val Fast = 120
    const val Medium = 220
    const val Slow = 360

    // Spring presets: damping + stiffness pairs.
    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f
    const val CardSpringDamping = 0.85f
    const val CardSpringStiffness = 400f
    const val ScreenSpringDamping = 0.92f
    const val ScreenSpringStiffness = 200f

    /** Hero snap and collapse settle. Calm, weighty. */
    fun heroSpring(): SpringSpec<Float> = spring(
        dampingRatio = HeroSpringDamping,
        stiffness = HeroSpringStiffness,
    )

    /** Card expand/collapse. Slightly bouncier. */
    fun cardSpring(): SpringSpec<Float> = spring(
        dampingRatio = CardSpringDamping,
        stiffness = CardSpringStiffness,
    )

    /** Full screen transitions. Very smooth, slow settle. */
    fun screenSpring(): SpringSpec<Float> = spring(
        dampingRatio = ScreenSpringDamping,
        stiffness = ScreenSpringStiffness,
    )

    // Tween presets for system-initiated animations.
    const val FastTweenMs = 150
    const val StandardTweenMs = 250
    const val SlowTweenMs = 400
}

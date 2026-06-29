package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * LDL motion vocabulary.
 *
 * Every animation in Ledger references these instead of local constants.
 * Springs for user-initiated motion, tweens for system events.
 */
object LedgerMotion {
    const val Fast = 120
    const val Medium = 220
    const val Slow = 360

    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f
    const val CardSpringDamping = 0.85f
    const val CardSpringStiffness = 400f
    const val ScreenSpringDamping = 0.92f
    const val ScreenSpringStiffness = 200f

    fun heroSpring(): SpringSpec<Float> = spring(dampingRatio = HeroSpringDamping, stiffness = HeroSpringStiffness)
    fun cardSpring(): SpringSpec<Float> = spring(dampingRatio = CardSpringDamping, stiffness = CardSpringStiffness)
    fun screenSpring(): SpringSpec<Float> = spring(dampingRatio = ScreenSpringDamping, stiffness = ScreenSpringStiffness)

    const val FastTweenMs = 150
    const val StandardTweenMs = 250
    const val SlowTweenMs = 400

    /** Stagger base delay between sequential list items. */
    const val StaggerBaseMs = 40
}

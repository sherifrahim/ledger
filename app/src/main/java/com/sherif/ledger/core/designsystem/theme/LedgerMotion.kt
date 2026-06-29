package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Ledger Design Language (LDL) motion vocabulary.
 *
 * Every animation in Ledger references this file instead of defining
 * durations, scales or spring values locally.
 *
 * This file is the single source of truth for interaction tuning.
 */
object LedgerMotion {

    // ------------------------------------------------------------------------
    // Duration presets (milliseconds)
    // ------------------------------------------------------------------------

    const val Fast = 120
    const val Medium = 220
    const val Slow = 360

    const val FastTweenMs = 150
    const val StandardTweenMs = 250
    const val SlowTweenMs = 400

    /** Base delay between sequential list animations. */
    const val StaggerBaseMs = 40

    // ------------------------------------------------------------------------
    // Micro interactions
    // ------------------------------------------------------------------------

    /** Surface compression while pressed. */
    const val PressScale = 0.985f

    /** Slight opacity reduction while pressed. */
    const val PressOpacity = 0.96f

    /** Optional icon emphasis when selected. */
    const val SelectedIconScale = 1.12f

    /** Disabled content alpha. */
    const val DisabledAlpha = 0.40f

    // ------------------------------------------------------------------------
    // Surface tuning
    // ------------------------------------------------------------------------

    /** Hairline / grouped surface border alpha. */
    const val SurfaceBorderAlpha = 0.20f

    /** Optional ambient highlight alpha. */
    const val SurfaceHighlightAlpha = 0.04f

    // ------------------------------------------------------------------------
    // Spring presets
    // ------------------------------------------------------------------------

    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f

    const val CardSpringDamping = 0.85f
    const val CardSpringStiffness = 400f

    const val ScreenSpringDamping = 0.92f
    const val ScreenSpringStiffness = 200f

    const val MicroSpringDamping = 0.70f
    const val MicroSpringStiffness = 600f

    /**
     * Hero transitions.
     *
     * Large surfaces, collapsing headers,
     * balance hero and screen headers.
     */
    fun heroSpring(): SpringSpec<Float> = spring(
        dampingRatio = HeroSpringDamping,
        stiffness = HeroSpringStiffness,
    )

    /**
     * Cards, grouped surfaces and expandable rows.
     */
    fun cardSpring(): SpringSpec<Float> = spring(
        dampingRatio = CardSpringDamping,
        stiffness = CardSpringStiffness,
    )

    /**
     * Whole-screen transitions.
     */
    fun screenSpring(): SpringSpec<Float> = spring(
        dampingRatio = ScreenSpringDamping,
        stiffness = ScreenSpringStiffness,
    )

    /**
     * Icons, chips, buttons and press interactions.
     */
    fun microSpring(): SpringSpec<Float> = spring(
        dampingRatio = MicroSpringDamping,
        stiffness = MicroSpringStiffness,
    )
}

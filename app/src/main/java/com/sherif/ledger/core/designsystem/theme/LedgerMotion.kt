package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Ledger V3 Motion Vocabulary (Materialization)
 * 
 * Focuses on calm, inevitable reveals without bounce or theatrical count-ups.
 */
object LedgerMotion {

    // Easing Curves
    val Resolve = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // Fast start, long tail
    val Standard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    // Durations
    const val Immediate = 100
    const val Short = 250
    const val Normal = 400
    const val Long = 700

    // V2 Compatibility Presets
    const val Fast = Immediate
    const val Medium = Short
    const val Slow = Long
    const val FastTweenMs = Immediate
    const val StandardTweenMs = Normal
    const val SlowTweenMs = Long
    const val StaggerBaseMs = 40
    
    const val PressScale = 0.985f
    const val PressOpacity = 0.96f
    const val SelectedIconScale = 1.12f
    const val DisabledAlpha = 0.40f
    
    const val SurfaceBorderAlpha = 0.20f
    const val SurfaceHighlightAlpha = 0.04f

    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f

    // Micro-interactions
    const val PressedScale = 0.98f
    const val PressedOpacity = 0.95f

    /**
     * Standard spring for subtle interaction feedback (if needed).
     * No bounce.
     */
    fun <T> calmSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> microSpring() = spring<T>(
        dampingRatio = 0.70f,
        stiffness = 600f
    )
    
    fun <T> heroSpring() = spring<T>(
        dampingRatio = HeroSpringDamping,
        stiffness = HeroSpringStiffness
    )
}

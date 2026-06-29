package com.sherif.ledger.core.designsystem.haptics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * LDL semantic haptic engine.
 *
 * Provides a vocabulary for tactile feedback that follows Ledger's
 * physical philosophy.
 */
interface LedgerHapticEngine {
    /** Selection change or soft tick. */
    fun selection()

    /** Success confirmation. */
    fun success()

    /** Error or critical alert. */
    fun error()

    /** Physical impact, like a surface settling. */
    fun impact()
}

/**
 * Default implementation using standard Android haptics.
 */
private class AndroidLedgerHapticEngine(
    private val hapticFeedback: HapticFeedback,
) : LedgerHapticEngine {
    override fun selection() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    override fun success() {
        // Android doesn't have a built-in success haptic in the standard API
        // without low-level VibrationEffect, but we can simulate it.
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    override fun error() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    override fun impact() {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

val LocalLedgerHaptics = staticCompositionLocalOf<LedgerHapticEngine> {
    object : LedgerHapticEngine {
        override fun selection() {}
        override fun success() {}
        override fun error() {}
        override fun impact() {}
    }
}

object LedgerHaptics {
    val current: LedgerHapticEngine
        @Composable @ReadOnlyComposable get() = LocalLedgerHaptics.current
}

/**
 * Provides the [LedgerHapticEngine] to the [content].
 */
@Composable
fun LedgerHapticProvider(content: @Composable () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    val engine = remember(hapticFeedback) { AndroidLedgerHapticEngine(hapticFeedback) }

    CompositionLocalProvider(
        LocalLedgerHaptics provides engine,
        content = content,
    )
}

package com.sherif.ledger.core.designsystem.haptics

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * LDL semantic haptic engine.
 *
 * Provides a vocabulary for tactile feedback that follows Ledger's
 * physical philosophy: subtle, refined, never buzzy. Each call maps to a
 * standard Android [HapticFeedbackConstants] — a single predefined effect,
 * NOT a custom VibrationEffect waveform/pattern. This keeps every phone's
 * own tuning intact (OnePlus/Pixel/Samsung all render these constants with
 * their native actuator profile) and always respects the user's system
 * haptic setting.
 */
interface LedgerHapticEngine {
    /** Selection change or a soft tap tick — the everyday interaction feel. */
    fun selection()

    /** Success confirmation. */
    fun success()

    /** Error or critical alert. */
    fun error()

    /** Physical impact, like a surface settling. */
    fun impact()
}

/**
 * Default implementation driven by the host [View].
 *
 * We go through [View.performHapticFeedback] rather than Compose's
 * [androidx.compose.ui.hapticfeedback.HapticFeedback] because the platform
 * View API exposes the full, subtle constant set (CONTEXT_CLICK, CONFIRM,
 * REJECT) — Compose historically only surfaced LongPress/TextHandleMove,
 * which is exactly the coarse, "too heavy" feel we're moving away from.
 *
 * CONTEXT_CLICK is the deliberate everyday choice: present and confident,
 * but light — not the heavy LongPress buzz. CONFIRM/REJECT (API 30+) are the
 * OS's own tuned success/failure ticks, with a graceful CONTEXT_CLICK
 * fallback on API 29 so nothing ever falls back to a heavy pattern.
 */
private class AndroidLedgerHapticEngine(
    private val view: View,
) : LedgerHapticEngine {

    private fun perform(constant: Int) {
        // No flags → the system's own haptic-intensity/enabled setting is honored.
        view.performHapticFeedback(constant)
    }

    override fun selection() = perform(HapticFeedbackConstants.CONTEXT_CLICK)

    override fun success() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        },
    )

    override fun error() = perform(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.REJECT
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        },
    )

    override fun impact() = perform(HapticFeedbackConstants.CONTEXT_CLICK)
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
    val view = LocalView.current
    val engine = remember(view) { AndroidLedgerHapticEngine(view) }

    CompositionLocalProvider(
        LocalLedgerHaptics provides engine,
        content = content,
    )
}

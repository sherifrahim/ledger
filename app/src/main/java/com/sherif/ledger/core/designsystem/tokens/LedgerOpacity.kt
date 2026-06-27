package com.sherif.ledger.core.designsystem.tokens

/**
 * Opacity tokens for LDL states and subtle fills.
 *
 * Named opacities keep disabled and tinted-fill treatments consistent instead of
 * scattering magic alpha values. Anything that is not a named state should use a
 * solid semantic color, not a faded one.
 */
object LedgerOpacity {
    /** Content that is present but not interactive. */
    const val Disabled = 0.38f

    /** Subtle tinted fills, such as a category swatch behind an avatar glyph. */
    const val Fill = 0.12f
}

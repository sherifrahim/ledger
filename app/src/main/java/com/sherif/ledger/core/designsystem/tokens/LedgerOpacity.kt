package com.sherif.ledger.core.designsystem.tokens

/**
 * LDL opacity tokens.
 *
 * Named opacities replace scattered magic alpha values. Each token describes
 * an intent so composables read as meaning rather than measurement.
 */
object LedgerOpacity {
    /** Non-interactive content that must remain visible. */
    const val Disabled = 0.38f
    /** Tinted fills behind avatars and swatches. */
    const val Fill = 0.12f
    /** Barely visible backgrounds (ghost buttons, subtle containers). */
    const val Subtle = 0.06f
    /** Quiet supporting text and icons (hero labels, metadata). */
    const val Muted = 0.22f
    /** Secondary content visible but not prominent. */
    const val Secondary = 0.45f
    /** Scrim or overlay behind modals. */
    const val Overlay = 0.60f

    // ── Glass Ecosystem ──
    
    /** Translucent container surface. */
    const val Glass = 0.65f
    /** Very soft diffusion (Atmosphere). */
    const val Diffusion = 0.12f
    /** Thin edge lighting highlight. */
    const val EdgeLight = 0.25f
    /** Subtle shadow depth in glass mode. */
    const val SoftShadow = 0.08f
}

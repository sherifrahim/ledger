package com.sherif.ledger.core.designsystem.theme

/**
 * Ledger appearance — an explicit Light / Dark choice (no "follow system"; the
 * user picks). Liquid Glass is a separate, orthogonal switch layered on top of
 * whichever of these is active (see [LocalLedgerGlass]).
 */
enum class LedgerThemeType {
    Light,
    Dark,
}

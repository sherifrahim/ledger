package com.sherif.ledger.core.designsystem.component

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

/**
 * A small, muted palette for merchants [LedgerBrandRegistry] has no real brand
 * mark for — deliberately separate from [com.sherif.ledger.core.designsystem.theme.LedgerColors]'
 * four semantic accents (Mint/Rose/Azure/Amber), which stay reserved for their
 * actual meaning (positive/negative/system/warning) and must never be spent on
 * decorating an arbitrary merchant initial.
 *
 * Hashed by name, not random, so the same unmatched merchant gets the same
 * colour every time it renders — the point (see [LedgerBrandIcon]) is that two
 * DIFFERENT unmatched merchants read as different identities instead of the
 * single shared pale-blue placeholder every one of them used before this.
 */
object LedgerAvatarPalette {
    private val hues = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF0D9488), // Teal
        Color(0xFFC2410C), // Terracotta
        Color(0xFF7C3AED), // Violet
        Color(0xFFB45309), // Ochre
        Color(0xFF0369A1), // Slate blue
        Color(0xFFBE185D), // Plum
        Color(0xFF15803D), // Forest
    )

    fun forName(name: String): Color {
        val key = name.trim().lowercase()
        if (key.isEmpty()) return hues.first()
        val index = key.hashCode().absoluteValue % hues.size
        return hues[index]
    }
}

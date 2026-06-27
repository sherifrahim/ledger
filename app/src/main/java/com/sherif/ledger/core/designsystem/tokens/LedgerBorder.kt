package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.ui.unit.dp

/**
 * Border widths for LDL.
 *
 * LDL separates content with hairlines instead of shadows or heavy rules, so a
 * single shared Hairline keeps every separator and surface edge identical rather
 * than scattering ad-hoc widths across components.
 */
object LedgerBorder {
    /** The standard hairline boundary between LDL content groups. */
    val Hairline = 1.dp
}

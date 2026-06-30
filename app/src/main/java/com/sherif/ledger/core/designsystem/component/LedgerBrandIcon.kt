package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Normalized Brand Identity component for Ledger.
 *
 * Replaces generic placeholders with authentic, data-driven identities.
 * Ensures consistent geometric presentation across all vendors and actors.
 */
@Composable
fun LedgerBrandIcon(
    name: String,
    modifier: Modifier = Modifier,
    type: LedgerIdentityType = LedgerIdentityType.Merchant,
    size: Dp = LedgerTheme.iconSize.Large,
) {
    val identity = remember(name, type) { LedgerBrandRegistry.resolve(name, type) }
    val accent = identity.color ?: LedgerTheme.colors.tint
    val bg = identity.backgroundColor ?: accent.copy(alpha = 0.12f)
    val contentColor = if (identity.backgroundColor != null) accent else accent

    Box(
        modifier = modifier
            .size(size)
            .ledgerSurface(
                shape = LedgerRadius.Medium, // Standardized LDL geometry
                backgroundColor = bg,
                borderColor = Color.Transparent,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (identity.icon != null) {
            Icon(
                imageVector = identity.icon,
                contentDescription = name,
                tint = contentColor,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.22f) // Normalized padding for optical weight
            )
        } else {
            val label = identity.monogram ?: name.take(1).uppercase()
            Text(
                text = label,
                style = LedgerTextStyles.Label.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = contentColor,
            )
        }
    }
}

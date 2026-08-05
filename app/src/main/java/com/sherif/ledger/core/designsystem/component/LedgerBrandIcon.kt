package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
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
    // Design review finding F3 (2026-08-06): every still-unmatched merchant fell
    // back to the exact same flat pale-blue circle (LedgerTheme.colors.tint),
    // which read as broken/random the moment two adjacent rows in a list had one
    // real brand icon and one identical generic placeholder. A hashed identity
    // colour — same idea as Slack/Gmail's unknown-contact avatars — makes every
    // still-unmatched merchant look intentionally distinct instead of uniformly
    // generic, without touching LedgerColors' four semantic accents (Mint/Rose/
    // Azure/Amber stay reserved for their actual meaning, never decoration).
    val accent = identity.color ?: LedgerAvatarPalette.forName(name)
    val bg = identity.backgroundColor ?: accent.copy(alpha = LedgerTheme.opacity.Fill)
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
        if (identity.painter != null) {
            Image(
                painter = identity.painter,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(LedgerRadius.Medium)
            )
        } else if (identity.icon != null) {
            Icon(
                imageVector = identity.icon,
                contentDescription = name,
                tint = contentColor,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.20f) // Refined for RC-008 optical weight
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

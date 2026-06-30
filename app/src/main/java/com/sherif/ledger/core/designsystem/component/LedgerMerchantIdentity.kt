package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL Merchant Identity component.
 *
 * Replaces generic initials with a branded visual system. Supports logos,
 * high-fidelity glyphs, and a refined fallback for unknown merchants.
 */
@Composable
fun LedgerMerchantIdentity(
    name: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    logoPainter: Painter? = null,
    size: Dp = LedgerTheme.iconSize.Large,
) {
    Box(
        modifier = modifier
            .size(size)
            .ledgerSurface(
                shape = CircleShape,
                backgroundColor = accentColor.copy(alpha = LedgerTheme.opacity.Fill),
                borderColor = Color.Transparent,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (logoPainter != null) {
            Image(
                painter = logoPainter,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                style = LedgerTextStyles.Label,
                color = accentColor,
            )
        }
    }
}

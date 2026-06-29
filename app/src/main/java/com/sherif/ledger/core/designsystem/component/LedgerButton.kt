package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

enum class LedgerButtonStyle { Primary, Secondary, Text }

/**
 * LDL standard button.
 *
 * Purged of Material 3 Button dependencies. Uses [ledgerClickable] for
 * micro-spring interaction and scale-based feedback.
 */
@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: LedgerButtonStyle = LedgerButtonStyle.Primary,
) {
    val contentPadding = PaddingValues(
        horizontal = LedgerSpacing.XLarge,
        vertical = LedgerSpacing.Small,
    )
    val shape = LedgerRadius.Small
    val colors = LedgerTheme.colors

    val (backgroundColor, contentColor, borderColor) = when (style) {
        LedgerButtonStyle.Primary -> Triple(
            if (enabled) colors.tint else colors.tint.copy(alpha = 0.4f),
            colors.onTint,
            Color.Transparent,
        )
        LedgerButtonStyle.Secondary -> Triple(
            Color.Transparent,
            if (enabled) colors.tint else colors.tint.copy(alpha = 0.4f),
            if (enabled) colors.tint else colors.tint.copy(alpha = 0.4f),
        )
        LedgerButtonStyle.Text -> Triple(
            Color.Transparent,
            if (enabled) colors.tint else colors.tint.copy(alpha = 0.4f),
            Color.Transparent,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(0.5.dp, borderColor, shape)
                } else Modifier,
            )
            .background(backgroundColor)
            .ledgerClickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = LedgerTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}

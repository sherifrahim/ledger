package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

enum class LedgerButtonStyle {
    Solid,  // Principal action, high contrast (ink on paper)
    Accent, // Principal affirmative action — brand green (e.g. Approve)
    Tonal,  // Secondary action, low contrast
    Ghost;  // Subtle action, no background

    companion object {
        val Primary = Solid
        val Secondary = Tonal
        val Text = Ghost
    }
}

/**
 * Ledger V3 Button System
 * 
 * Replaces V2 spring-heavy buttons with architectural, immediate reveal controls.
 */
@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: LedgerButtonStyle = LedgerButtonStyle.Solid,
    icon: ImageVector? = null,
) {
    val colors = LedgerTheme.colors
    
    val (backgroundColor, contentColor) = when (style) {
        LedgerButtonStyle.Solid -> colors.textPrimary to colors.surfaceBase
        LedgerButtonStyle.Accent -> colors.accent to Color.White
        LedgerButtonStyle.Tonal -> colors.surfaceInset to colors.textPrimary
        LedgerButtonStyle.Ghost -> Color.Transparent to colors.textPrimary
    }
    val borderless = style == LedgerButtonStyle.Solid || style == LedgerButtonStyle.Accent

    val finalContentColor = if (enabled) contentColor else colors.textTertiary
    val finalBackgroundColor = if (enabled) backgroundColor else {
        if (style == LedgerButtonStyle.Ghost) Color.Transparent else colors.surfaceInset.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .ledgerSurface(
                backgroundColor = finalBackgroundColor,
                borderColor = if (borderless) Color.Transparent else colors.border,
                onClick = onClick,
                enabled = enabled,
                shape = LedgerRadius.Medium
            )
            .padding(horizontal = LedgerSpacing.Large, vertical = LedgerSpacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.InlineGap)
        ) {
            if (icon != null) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = finalContentColor
                )
            }
            Text(
                text = text,
                style = LedgerTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = finalContentColor,
            )
        }
    }
}

/**
 * Standardized icon-only button for V3.
 */
@Composable
fun LedgerIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LedgerTheme.colors.textPrimary,
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .ledgerSurface(
                backgroundColor = backgroundColor,
                borderColor = Color.Transparent,
                onClick = onClick,
                enabled = enabled,
                shape = LedgerRadius.Full
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
    }
}

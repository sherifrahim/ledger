package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * The one surface per screen that is allowed to look expensive rather than
 * quiet — the balance hero, the net-worth hero, the card face. Everything
 * else in the design system ([LedgerCard], [LedgerSurface]) is deliberately
 * calm so this reads as the thing the screen is actually about, the way a
 * physical card sitting on a flat desk draws the eye in Apple Wallet.
 *
 * A flat single-colour fill is what a hero card looked like before this —
 * correct information, no sense of light or material. This adds three
 * things a physical surface actually has: a gradient base (never one flat
 * colour), two soft colour blooms low in the corners (the same accent/azure
 * pairing the Liquid Glass ambient backdrop uses, so the two feel like one
 * material language), and a diagonal sheen across the top third — the
 * highlight a glossy surface catches under light, at a low enough alpha
 * that it reads as depth rather than as an obvious decal.
 */
@Composable
fun LedgerHeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Large),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerTheme.colors
    val shape = LedgerRadius.XLarge

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (colors.isDark) 0.dp else 18.dp,
                shape = shape,
                clip = false,
                ambientColor = colors.shadowColor,
                spotColor = colors.shadowColor,
            )
            .clip(shape)
            .drawBehind {
                val base = if (colors.isDark) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF181D22), Color(0xFF0E1013)),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF2F4F6)),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    )
                }
                drawRect(base)

                // Two low corner blooms — same colour pairing as the Liquid Glass
                // ambient backdrop, so a hero card and the glass behind it read as
                // one material rather than two unrelated effects.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = if (colors.isDark) 0.16f else 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 0.06f, size.height * 1.05f),
                        radius = size.maxDimension * 0.65f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = if (colors.isDark) 0.12f else 0.06f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width * 1.02f, size.height * -0.1f),
                        radius = size.maxDimension * 0.7f,
                    ),
                )

                // The sheen: a diagonal highlight across the top third, like light
                // grazing a glossy surface. Faint by design — a card, not a sticker.
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (colors.isDark) 0.06f else 0.5f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.7f, size.height * 0.55f),
                    ),
                )
            }
            .border(LedgerTheme.border.Hairline, colors.cardBorder.copy(alpha = 0.6f), shape)
            .padding(contentPadding),
        content = content,
    )
}

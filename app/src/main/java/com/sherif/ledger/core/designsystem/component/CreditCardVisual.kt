package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.SquircleShape

/**
 * The one piece of imagery Credit Card Manager has that plain numbers can't
 * give it: a real card face. There is no photograph of the owner's actual
 * card here — this app has no licence to real bank card art, and a wrong
 * network mark or a mismatched card design would read as fake rather than
 * as premium. Every fintech app that shows a card without a photographed one
 * (Apple Wallet included) draws a bespoke card instead; this does the same,
 * built from data the app actually has: the bank's own brand colour (already
 * in [LedgerBrandRegistry] from the account list icons) stretched into a
 * proper gradient, plus the real masked tail.
 */
@Composable
fun CreditCardVisual(
    bankName: String,
    tail: String?,
    modifier: Modifier = Modifier,
    brandColor: Color? = null,
) {
    val base = brandColor ?: LedgerBrandRegistry.resolve(bankName, LedgerIdentityType.Bank).color
        ?: LedgerTheme.colors.system
    val shape = SquircleShape(cornerRadius = 20.dp, smoothness = 0.85f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // ISO 7810 ID-1 card ratio
            .shadow(elevation = 20.dp, shape = shape, clip = false, ambientColor = base, spotColor = base)
            .clip(shape)
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(
                            lerp(base, Color.Black, 0.35f),
                            base,
                            lerp(base, Color.Black, 0.55f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
                )
                // Diagonal sheen — the same light-catch treatment LedgerHeroCard
                // uses, so a card and a balance hero read as one material family.
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.65f, size.height * 0.75f),
                    ),
                )
                // A soft bloom low-right, echoing the card's own colour back at
                // low alpha — depth without inventing a texture that isn't there.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 1.05f),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
    ) {
        Column(Modifier.fillMaxSize().padding(LedgerSpacing.Large)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ChipGlyph()
                Text(
                    bankName,
                    style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp),
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                )
            }

            Box(Modifier.weight(1f))

            Text(
                tail?.let { "•••• •••• •••• $it" } ?: "•••• •••• •••• ••••",
                style = LedgerTextStyles.Title.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontFeatureSettings = "tnum",
                ),
                color = Color.White.copy(alpha = 0.95f),
            )
        }
    }
}

/** A small rounded-rect chip with its own gold gradient — the one universally
 *  recognisable card element that isn't tied to any single bank's identity. */
@Composable
private fun ChipGlyph() {
    Box(
        Modifier
            .size(width = 34.dp, height = 26.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE8D391), Color(0xFFBFA24E), Color(0xFFE8D391)),
                ),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF8A7332).copy(alpha = 0.5f)),
        )
    }
}

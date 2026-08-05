package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
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
                // Base: five stops rather than a plain two-colour fade. Real card
                // stock isn't flat-lit — it has an anisotropic brushed-metal
                // highlight band running across it. Faking that band (not a flat
                // gradient) is most of the difference between "a rounded rect
                // filled with a colour" and "a card."
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(
                            lerp(base, Color.Black, 0.45f),
                            lerp(base, Color.Black, 0.15f),
                            lerp(base, Color.White, 0.18f),
                            lerp(base, Color.Black, 0.2f),
                            lerp(base, Color.Black, 0.6f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
                )
                // Vignette — every real photographed card is slightly darker at
                // its corners than its centre from how light falls on a curved
                // edge; a flat-lit rectangle reads as an on-screen graphic rather
                // than an object.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = size.maxDimension * 0.72f,
                    ),
                )
                // Diagonal sheen — the same light-catch treatment LedgerHeroCard
                // uses, so a card and a balance hero read as one material family.
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.6f, size.height * 0.7f),
                    ),
                )
                // A second, sharper highlight — a thin bright edge near the top,
                // the way brushed metal catches a single hard specular line
                // rather than one soft glow.
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        start = Offset(size.width * 0.1f, size.height * 0.05f),
                        end = Offset(size.width * 0.55f, size.height * 0.22f),
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
                    style = LedgerTextStyles.Label.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 1.5f), 3f),
                    ),
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1,
                )
            }

            Box(Modifier.weight(1f))

            // Embossed rather than flat-printed: a soft dark shadow sitting just
            // under the glyphs is what makes raised card numbering read as
            // physically stamped instead of drawn on top of the card.
            Text(
                tail?.let { "•••• •••• •••• $it" } ?: "•••• •••• •••• ••••",
                style = LedgerTextStyles.Title.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontFeatureSettings = "tnum",
                    shadow = Shadow(Color.Black.copy(alpha = 0.4f), Offset(0f, 2f), 4f),
                ),
                color = Color.White.copy(alpha = 0.97f),
            )
        }
    }
}

/** A small rounded-rect chip with its own gold gradient — the one universally
 *  recognisable card element that isn't tied to any single bank's identity. */
@Composable
private fun ChipGlyph() {
    val shape = RoundedCornerShape(5.dp)
    Box(
        Modifier
            .size(width = 34.dp, height = 26.dp)
            .shadow(elevation = 2.dp, shape = shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF3E2A9), Color(0xFFCBA84F), Color(0xFF9C7B2E), Color(0xFFE8D391)),
                ),
            ),
    ) {
        // The grid of contact pads a real EMV chip has — three horizontal
        // divider lines rather than one, so it reads as a chip and not just a
        // gold rectangle.
        Column(Modifier.fillMaxSize().padding(vertical = 5.dp), verticalArrangement = Arrangement.SpaceEvenly) {
            repeat(3) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF6B531F).copy(alpha = 0.55f)))
            }
        }
        Box(Modifier.align(Alignment.Center).fillMaxHeight().padding(vertical = 3.dp).width(1.dp).background(Color(0xFF6B531F).copy(alpha = 0.4f)))
    }
}

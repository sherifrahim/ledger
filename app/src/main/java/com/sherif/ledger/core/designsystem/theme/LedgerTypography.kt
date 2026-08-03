package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sherif.ledger.R

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Ledger V3 Typography (Editorial & Authoritative)
 */
object LedgerTextStyles {

    // Hero financial figure — the single largest number on a screen (e.g. Safe to
    // Spend). Tabular so digits never shift width as the value animates.
    val Hero = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum",
    )

    // Authoritative Financial Figures
    val Display = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum", // Tabular Numerals
    )

    val Headline = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    )

    val Title = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    )

    // Body & Narrative
    val BodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    )

    val BodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )

    val Label = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )

    val Caption = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )

    // Money in a list. Same size as BodyMedium so a row's amount and its title sit
    // on one optical line, but tabular ("tnum") so every digit occupies the same
    // advance width — without it the decimal points of a scrolling column of
    // amounts wander by a pixel or two per row, which is the single most obvious
    // tell that a finance app was not drawn by someone counting.
    val AmountRow = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontFeatureSettings = "tnum",
    )

    // The currency code that accompanies an amount. Deliberately its own style
    // rather than a scaled-down copy of the amount's: scaling a style by 0.6
    // also scales its line height, which is what pushed the code off the number's
    // baseline everywhere it appeared.
    val AmountCurrency = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
    )

    // Explanatory Intelligence
    val Narrative = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    )

    // V2 Compatibility Aliases
    val Section = Title
    val Body = BodyLarge
    val Mono = BodyMedium.copy(fontFamily = FontFamily.Monospace)
}

val LedgerTypography = Typography(
    displayLarge = LedgerTextStyles.Display,
    headlineLarge = LedgerTextStyles.Headline,
    headlineMedium = LedgerTextStyles.Title,
    titleLarge = LedgerTextStyles.Title,
    titleMedium = LedgerTextStyles.BodyLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = LedgerTextStyles.BodyLarge,
    bodyMedium = LedgerTextStyles.BodyMedium,
    labelLarge = LedgerTextStyles.Label,
    labelMedium = LedgerTextStyles.Narrative,
    bodySmall = LedgerTextStyles.Caption,
)

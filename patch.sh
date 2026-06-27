#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Writing LDL foundation + collapsing hero..."

# ---- core/designsystem/tokens/LedgerBorder.kt ----
mkdir -p "$B/core/designsystem/tokens"
cat > "$B/core/designsystem/tokens/LedgerBorder.kt" << 'KEOF'
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
KEOF

# ---- core/designsystem/tokens/LedgerOpacity.kt ----
mkdir -p "$B/core/designsystem/tokens"
cat > "$B/core/designsystem/tokens/LedgerOpacity.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.tokens

/**
 * Opacity tokens for LDL states and subtle fills.
 *
 * Named opacities keep disabled and tinted-fill treatments consistent instead of
 * scattering magic alpha values. Anything that is not a named state should use a
 * solid semantic color, not a faded one.
 */
object LedgerOpacity {
    /** Content that is present but not interactive. */
    const val Disabled = 0.38f

    /** Subtle tinted fills, such as a category swatch behind an avatar glyph. */
    const val Fill = 0.12f
}
KEOF

# ---- core/designsystem/component/LedgerHairline.kt ----
mkdir -p "$B/core/designsystem/component"
cat > "$B/core/designsystem/component/LedgerHairline.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LedgerHairline is the LDL separator, replacing Material's Divider.
 *
 * LDL groups content with thin hairlines instead of elevation or heavy rules, so
 * this is intentionally the thinnest visible line. Insets are the caller's
 * responsibility, matching inset grouped lists where separators indent to align
 * with content rather than running to the screen edge.
 */
@Composable
fun LedgerHairline(
    modifier: Modifier = Modifier,
    color: Color = LedgerTheme.colors.separator,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LedgerTheme.border.Hairline)
            .background(color),
    )
}
KEOF

# ---- core/designsystem/component/LedgerSurface.kt ----
mkdir -p "$B/core/designsystem/component"
cat > "$B/core/designsystem/component/LedgerSurface.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LedgerSurface is the fundamental bounded surface in LDL.
 *
 * Unlike a Material Card, a surface communicates hierarchy through a semantic
 * [LedgerSurfaceLevel] resolved by the theme, never through elevation. LDL keeps
 * surfaces flat and separates them with tone and hairlines, matching the Apple
 * grouped-list grammar that anchors Ledger's foundation.
 *
 * Components should request a surface level rather than a raw color, so the
 * meaning of a surface stays stable even when the palette changes. It is built
 * on a Material Surface only as plumbing, with elevation forced flat.
 */
@Composable
fun LedgerSurface(
    modifier: Modifier = Modifier,
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Level1,
    shape: Shape = LedgerRadius.Small,
    hairlineBorder: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LedgerTheme.colors
    val border = if (hairlineBorder) {
        BorderStroke(LedgerTheme.border.Hairline, colors.separator)
    } else {
        null
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = colors.surface(level),
        contentColor = colors.label,
        border = border,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
KEOF

# ---- core/designsystem/component/LedgerAmount.kt ----
mkdir -p "$B/core/designsystem/component"
cat > "$B/core/designsystem/component/LedgerAmount.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

enum class LedgerAmountStyle {
    Small,
    Regular,
    Large,
}

/**
 * LDS amount text for money values.
 *
 * The caller provides the final display string so formatting and currency
 * decisions stay in the appropriate feature or domain layer. Direction and
 * status are expressed through [color], drawn from LDL financial semantics.
 */
@Composable
fun LedgerAmount(
    amount: String,
    modifier: Modifier = Modifier,
    style: LedgerAmountStyle = LedgerAmountStyle.Regular,
    color: Color = LedgerTheme.colors.label,
) {
    val textStyle: TextStyle = when (style) {
        LedgerAmountStyle.Small -> LedgerTextStyles.Mono.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        LedgerAmountStyle.Regular -> LedgerTextStyles.Mono
        LedgerAmountStyle.Large -> LedgerTextStyles.Title.copy(fontFamily = FontFamily.Monospace)
    }

    Text(
        text = amount,
        modifier = modifier,
        style = textStyle,
        color = color,
    )
}
KEOF

# ---- core/designsystem/component/LedgerSectionTitle.kt ----
mkdir -p "$B/core/designsystem/component"
cat > "$B/core/designsystem/component/LedgerSectionTitle.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Standard LDL section heading.
 *
 * Sits above an inset grouped surface to label it, in the Apple grouped-list
 * grammar where the header is outside the rounded group rather than inside it.
 */
@Composable
fun LedgerSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = LedgerTextStyles.Section,
        color = LedgerTheme.colors.label,
    )
}
KEOF

# ---- core/designsystem/component/LedgerStatCard.kt ----
mkdir -p "$B/core/designsystem/component"
cat > "$B/core/designsystem/component/LedgerStatCard.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Compact LDL surface for one dashboard metric.
 *
 * Built on [LedgerSurface] at Level1, so a metric tile reads as a grouped
 * section on the Level0 page rather than a floating Material card. Pass
 * preformatted values from the screen layer; this carries no business logic.
 */
@Composable
fun LedgerStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LedgerTheme.colors.label,
    supportingText: String? = null,
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Medium),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XSmall),
        ) {
            Text(
                text = label,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.secondaryLabel,
            )
            LedgerAmount(
                amount = value,
                style = LedgerAmountStyle.Large,
                color = valueColor,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.secondaryLabel,
                )
            }
        }
    }
}
KEOF

# ---- core/designsystem/component/LedgerTransactionRow.kt ----
mkdir -p "$B/core/designsystem/component"
cat > "$B/core/designsystem/component/LedgerTransactionRow.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Standard LDL row for transaction-like summaries.
 *
 * Presentational only. Accepts display strings and does not depend on entities,
 * repositories, parsers, or ViewModels. Money direction is communicated through
 * [amountColor] from LDL financial semantics, never through decoration.
 */
@Composable
fun LedgerTransactionRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    metadata: String? = null,
    tag: String? = null,
    accentColor: Color = LedgerTheme.colors.tint,
    amountColor: Color = LedgerTheme.colors.label,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LedgerSpacing.XxLarge)
                .clip(MaterialTheme.shapes.small)
                .background(accentColor.copy(alpha = LedgerTheme.opacity.Fill)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                style = LedgerTextStyles.Label,
                color = accentColor,
            )
        }
        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                text = title,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label,
            )
            val supportingText = listOfNotNull(subtitle, metadata).joinToString(" - ")
            if (supportingText.isNotEmpty()) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.secondaryLabel,
                )
            }
            if (tag != null) {
                LedgerTag(text = tag)
            }
        }
        Spacer(modifier = Modifier.width(LedgerSpacing.Medium))
        LedgerAmount(amount = amount, color = amountColor)
    }
}
KEOF

# ---- core/designsystem/component/hero/LedgerCollapsingHero.kt ----
mkdir -p "$B/core/designsystem/component/hero"
cat > "$B/core/designsystem/component/hero/LedgerCollapsingHero.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.component.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.sherif.ledger.core.designsystem.tokens.LedgerGradients

/**
 * Dimension and transition defaults for [LedgerCollapsingHero].
 *
 * Every magic number that controls the hero's structure lives here so the
 * component body stays free of inline constants. Values are hypotheses:
 * adjust after running on a real device, not from a specification.
 */
object LedgerHeroDefaults {

    /** Hero height when fully expanded (scroll offset zero). */
    val ExpandedHeight: Dp = 280.dp

    /** Hero height when fully collapsed (pinned compact header). */
    val CollapsedHeight: Dp = 56.dp

    /** Bottom corner radius at full expansion. Reaches 0 dp when compact. */
    val CornerRadius: Dp = 32.dp

    /** Default gradient background. */
    val Background: Brush = LedgerGradients.Emerald
}

/**
 * A reusable scroll-driven collapsing hero surface.
 *
 * The container interpolates its height, corner radius, and background
 * between an expanded and a compact state as [collapseProgress] moves
 * from 0 (expanded) to 1 (compact). It makes no decisions about the
 * content inside: two composable slots receive the current progress so
 * each caller can choreograph its own entry, exit, and internal
 * transitions independently.
 *
 * Both slots are always composed and overlaid. The expanded slot fills
 * the hero. The compact slot is pinned to the top at [collapsedHeight].
 * Callers control visibility (typically via `graphicsLayer { alpha }`)
 * so the container stays agnostic to crossfade timing.
 *
 * This is LDL infrastructure. It carries no business logic, no
 * dashboard state, and no navigation awareness. Screens provide meaning
 * through the slots.
 *
 * @param collapseProgress 0f = fully expanded, 1f = fully compact.
 * @param expandedContent Slot for the full hero layout. Receives progress.
 * @param compactContent  Slot for the pinned compact header. Receives progress.
 */
@Composable
fun LedgerCollapsingHero(
    collapseProgress: Float,
    modifier: Modifier = Modifier,
    expandedHeight: Dp = LedgerHeroDefaults.ExpandedHeight,
    collapsedHeight: Dp = LedgerHeroDefaults.CollapsedHeight,
    cornerRadius: Dp = LedgerHeroDefaults.CornerRadius,
    background: Brush = LedgerHeroDefaults.Background,
    expandedContent: @Composable (collapseProgress: Float) -> Unit,
    compactContent: @Composable (collapseProgress: Float) -> Unit,
) {
    val currentHeight = lerp(expandedHeight, collapsedHeight, collapseProgress)
    val currentRadius = lerp(cornerRadius, 0.dp, collapseProgress)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentHeight)
            .clip(
                RoundedCornerShape(
                    bottomStart = currentRadius,
                    bottomEnd = currentRadius,
                ),
            )
            .background(background),
    ) {
        Box(Modifier.fillMaxSize()) {
            expandedContent(collapseProgress)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(collapsedHeight),
        ) {
            compactContent(collapseProgress)
        }
    }
}
KEOF

# ---- core/designsystem/theme/LedgerColors.kt ----
mkdir -p "$B/core/designsystem/theme"
cat > "$B/core/designsystem/theme/LedgerColors.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LedgerEmerald50 = Color(0xFFECFDF5)
val LedgerEmerald100 = Color(0xFFD1FAE5)
val LedgerEmerald200 = Color(0xFFA7F3D0)
val LedgerEmerald300 = Color(0xFF6EE7B7)
val LedgerEmerald400 = Color(0xFF34D399)
val LedgerEmerald500 = Color(0xFF10B981)
val LedgerEmerald600 = Color(0xFF059669)
val LedgerEmerald700 = Color(0xFF047857)
val LedgerEmerald800 = Color(0xFF065F46)
val LedgerEmerald900 = Color(0xFF064E3B)
val LedgerEmerald950 = Color(0xFF022C22)

val LedgerInk = Color(0xFF101815)
val LedgerGraphite = Color(0xFF47534F)
val LedgerMist = Color(0xFFF7FAF8)
val LedgerPaper = Color(0xFFFFFFFF)
val LedgerLine = Color(0xFFE1EAE5)

val LedgerDarkInk = Color(0xFFE6F2EC)
val LedgerDarkGraphite = Color(0xFFA7B8B0)
val LedgerDarkSurface = Color(0xFF111C18)
val LedgerDarkSurfaceHigh = Color(0xFF182520)
val LedgerDarkBackground = Color(0xFF08110E)
val LedgerDarkLine = Color(0xFF2B3B34)

val LedgerSuccess = Color(0xFF047857)
val LedgerWarning = Color(0xFFB7791F)
val LedgerError = Color(0xFFBA1A1A)

val LedgerLightColorScheme = lightColorScheme(
    primary = LedgerEmerald700,
    onPrimary = Color.White,
    primaryContainer = LedgerEmerald100,
    onPrimaryContainer = LedgerEmerald950,
    secondary = Color(0xFF4D635A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8DD),
    onSecondaryContainer = Color(0xFF0A1F18),
    tertiary = Color(0xFF446277),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6FF),
    onTertiaryContainer = Color(0xFF001E2E),
    error = LedgerError,
    onError = Color.White,
    background = LedgerMist,
    onBackground = LedgerInk,
    surface = LedgerPaper,
    onSurface = LedgerInk,
    surfaceVariant = Color(0xFFEAF2EE),
    onSurfaceVariant = LedgerGraphite,
    outline = Color(0xFF74827B),
    outlineVariant = LedgerLine,
    inverseSurface = LedgerInk,
    inverseOnSurface = LedgerMist,
    inversePrimary = LedgerEmerald300,
    scrim = Color.Black,
)

val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerEmerald300,
    onPrimary = LedgerEmerald950,
    primaryContainer = LedgerEmerald800,
    onPrimaryContainer = LedgerEmerald100,
    secondary = Color(0xFFB4CCC0),
    onSecondary = Color(0xFF20352D),
    secondaryContainer = Color(0xFF364B43),
    onSecondaryContainer = Color(0xFFD0E8DD),
    tertiary = Color(0xFFACCAE0),
    onTertiary = Color(0xFF143348),
    tertiaryContainer = Color(0xFF2C4A5F),
    onTertiaryContainer = Color(0xFFC8E6FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = LedgerDarkBackground,
    onBackground = LedgerDarkInk,
    surface = LedgerDarkSurface,
    onSurface = LedgerDarkInk,
    surfaceVariant = LedgerDarkSurfaceHigh,
    onSurfaceVariant = LedgerDarkGraphite,
    outline = Color(0xFF8D9D95),
    outlineVariant = LedgerDarkLine,
    inverseSurface = LedgerDarkInk,
    inverseOnSurface = LedgerDarkBackground,
    inversePrimary = LedgerEmerald700,
    scrim = Color.Black,
)

/**
 * Semantic surface hierarchy for LDL.
 *
 * A component declares the intent of its surface rather than a color or an
 * elevation. The theme resolves intent to a concrete tone per light and dark.
 * In LDL these levels are tonal steps, not shadows, matching the Apple
 * grouped-list grammar where depth reads through tone and hairlines.
 *
 * Level0 app background. Level1 the primary grouped section that holds content.
 * Level2 a nested block inside a section. Level3 transient top layer, sheets and
 * menus.
 */
enum class LedgerSurfaceLevel { Level0, Level1, Level2, Level3 }

/**
 * The LDL semantic color contract, read through [LedgerTheme.colors].
 *
 * Components never reference raw palette constants or Material color roles. They
 * ask for meaning: a surface level, a label, a separator, or a financial
 * semantic such as income or expense. This is the layer that lets Ledger drop
 * Material's visual identity while keeping Material as the toolkit underneath.
 *
 * Financial semantics carry the Monzo layer of LDL: direction and status of
 * money are always communicated by a dedicated color, never by decoration.
 */
data class LedgerColors(
    val isDark: Boolean,
    val surfaceLevel0: Color,
    val surfaceLevel1: Color,
    val surfaceLevel2: Color,
    val surfaceLevel3: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    val tint: Color,
    val onTint: Color,
    val income: Color,
    val expense: Color,
    val pending: Color,
    val refund: Color,
    val warning: Color,
    val success: Color,
    val neutral: Color,
) {
    /** Resolves a semantic surface level to its concrete tone. */
    fun surface(level: LedgerSurfaceLevel): Color = when (level) {
        LedgerSurfaceLevel.Level0 -> surfaceLevel0
        LedgerSurfaceLevel.Level1 -> surfaceLevel1
        LedgerSurfaceLevel.Level2 -> surfaceLevel2
        LedgerSurfaceLevel.Level3 -> surfaceLevel3
    }
}

val LedgerLightColors = LedgerColors(
    isDark = false,
    surfaceLevel0 = Color(0xFFF2F3F5),
    surfaceLevel1 = Color(0xFFFFFFFF),
    surfaceLevel2 = Color(0xFFF6F7F9),
    surfaceLevel3 = Color(0xFFFFFFFF),
    label = Color(0xFF0F1714),
    secondaryLabel = Color(0xFF5B6B64),
    tertiaryLabel = Color(0xFF8A9A92),
    separator = Color(0xFFE2E7E4),
    tint = LedgerEmerald700,
    onTint = Color(0xFFFFFFFF),
    income = LedgerEmerald700,
    expense = Color(0xFFC0362C),
    pending = Color(0xFFB7791F),
    refund = Color(0xFF2563EB),
    warning = Color(0xFFB7791F),
    success = LedgerEmerald700,
    neutral = Color(0xFF6B7770),
)

val LedgerDarkColors = LedgerColors(
    isDark = true,
    surfaceLevel0 = Color(0xFF08110E),
    surfaceLevel1 = Color(0xFF111C18),
    surfaceLevel2 = Color(0xFF182520),
    surfaceLevel3 = Color(0xFF1F2C27),
    label = Color(0xFFE6F2EC),
    secondaryLabel = Color(0xFFA7B8B0),
    tertiaryLabel = Color(0xFF7C8B84),
    separator = Color(0xFF2B3B34),
    tint = LedgerEmerald300,
    onTint = LedgerEmerald950,
    income = LedgerEmerald300,
    expense = Color(0xFFF97066),
    pending = Color(0xFFF0B860),
    refund = Color(0xFF60A5FA),
    warning = Color(0xFFF0B860),
    success = LedgerEmerald300,
    neutral = Color(0xFF9CA9A2),
)

/** Theme-provided LDL colors. Read via [LedgerTheme.colors], never directly. */
val LocalLedgerColors = staticCompositionLocalOf { LedgerLightColors }
KEOF

# ---- core/designsystem/theme/LedgerTheme.kt ----
mkdir -p "$B/core/designsystem/theme"
cat > "$B/core/designsystem/theme/LedgerTheme.kt" << 'KEOF'
package com.sherif.ledger.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.sherif.ledger.core.designsystem.tokens.LedgerBorder
import com.sherif.ledger.core.designsystem.tokens.LedgerOpacity
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Applies the Ledger Design Language.
 *
 * Material3 stays underneath purely as a toolkit: it still receives a
 * [androidx.compose.material3.ColorScheme] so stray Material widgets do not
 * render broken. LDL components, however, read meaning from [LedgerTheme], not
 * from Material color roles. That separation is what lets Ledger shed Material's
 * visual identity without rebuilding the component toolkit.
 *
 * Dynamic color remains available only as an explicit opt in and is off by
 * default, so Ledger keeps its own palette rather than adopting the device's.
 */
@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> LedgerDarkColorScheme
        else -> LedgerLightColorScheme
    }

    val ledgerColors = if (darkTheme) LedgerDarkColors else LedgerLightColors

    CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LedgerTypography,
            shapes = LedgerShapes,
            content = content,
        )
    }
}

/**
 * The single semantic entry point for LDL.
 *
 * Components reach design decisions through this object rather than importing
 * tokens piecemeal or reading Material roles, so the vocabulary stays one
 * consistent surface. Only [colors] varies with light and dark, so only it flows
 * through a CompositionLocal. The rest are stable tokens exposed directly, which
 * keeps the theme honest and avoids CompositionLocals that never change.
 */
object LedgerTheme {
    val colors: LedgerColors
        @Composable @ReadOnlyComposable get() = LocalLedgerColors.current

    val radius get() = LedgerRadius
    val border get() = LedgerBorder
    val opacity get() = LedgerOpacity
    val motion get() = LedgerMotion
    val elevation get() = LedgerElevation
}
KEOF

# ---- presentation/dashboard/DashboardScreen.kt ----
mkdir -p "$B/presentation/dashboard"
cat > "$B/presentation/dashboard/DashboardScreen.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.hero.LedgerCollapsingHero
import com.sherif.ledger.core.designsystem.component.hero.LedgerHeroDefaults
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.components.InsightSection
import com.sherif.ledger.presentation.dashboard.components.QuickStatsSection
import com.sherif.ledger.presentation.dashboard.components.RecentTransactionsSection
import com.sherif.ledger.presentation.dashboard.preview.DashboardPreviewData

/**
 * Transition thresholds for the dashboard hero content.
 * All values are hypotheses. Tune after device testing.
 */
private object HeroTransitions {
    const val GreetingExit = 0.3f
    const val GreetingTranslationPx = 80f
    const val MonthExit = 0.5f
    const val ExpandedExit = 0.7f
    const val CompactEnter = 0.6f
}

private object HeroSnap {
    const val Enabled = true
    const val ZoneStart = 0.3f
    const val ZoneEnd = 0.7f
    const val DampingRatio = 0.82f
    val Stiffness = Spring.StiffnessMediumLow
}

@Composable
fun DashboardScreen(
    state: DashboardUiState = DashboardPreviewData.state,
) {
    val expandedHeight = LedgerHeroDefaults.ExpandedHeight
    val collapsedHeight = LedgerHeroDefaults.CollapsedHeight
    val maxOffsetPx = with(LocalDensity.current) {
        (expandedHeight - collapsedHeight).toPx()
    }

    val listState = rememberLazyListState()

    val collapseProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / maxOffsetPx).coerceIn(0f, 1f)
            }
        }
    }

    if (HeroSnap.Enabled) {
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress && listState.firstVisibleItemIndex == 0) {
                val p = collapseProgress
                if (p in HeroSnap.ZoneStart..HeroSnap.ZoneEnd) {
                    val targetPx = if (p > 0.5f) maxOffsetPx else 0f
                    val currentPx = listState.firstVisibleItemScrollOffset.toFloat()
                    listState.animateScrollBy(
                        value = targetPx - currentPx,
                        animationSpec = spring(
                            dampingRatio = HeroSnap.DampingRatio,
                            stiffness = HeroSnap.Stiffness,
                        ),
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = LedgerSpacing.Large,
                end = LedgerSpacing.Large,
                bottom = LedgerSpacing.XLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero_spacer") {
                Spacer(Modifier.height(expandedHeight))
            }
            item(key = "stats") {
                QuickStatsSection(state)
            }
            item(key = "transactions") {
                RecentTransactionsSection(state)
            }
            item(key = "insights") {
                InsightSection(state)
            }
        }

        LedgerCollapsingHero(
            collapseProgress = collapseProgress,
            expandedContent = { progress -> ExpandedHeroContent(progress, state) },
            compactContent = { progress -> CompactHeroContent(progress, state) },
        )
    }
}

@Composable
private fun ExpandedHeroContent(progress: Float, state: DashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 20.dp, bottom = 28.dp)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Greeting: quiet, secondary. The user, not the data.
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = (1f - progress / HeroTransitions.GreetingExit).coerceIn(0f, 1f)
                translationY = -(progress * HeroTransitions.GreetingTranslationPx)
            },
        ) {
            Text(
                text = "${state.greeting},",
                style = LedgerTextStyles.Caption,
                color = Color.White.copy(alpha = 0.60f),
            )
            Text(
                text = state.userName,
                style = LedgerTextStyles.Title,
                color = Color.White,
            )
        }

        // The money. Nothing else competes.
        Column {
            Text(
                text = state.totalSpent,
                style = LedgerTextStyles.Display,
                color = Color.White,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = state.currentMonth,
                style = LedgerTextStyles.Caption,
                color = Color.White.copy(alpha = 0.56f),
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.MonthExit).coerceIn(0f, 1f)
                },
            )
        }
    }
}

@Composable
private fun CompactHeroContent(progress: Float, state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                alpha = ((progress - HeroTransitions.CompactEnter) /
                    (1f - HeroTransitions.CompactEnter)).coerceIn(0f, 1f)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.totalSpent,
            style = LedgerTextStyles.Section,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = state.currentMonth,
            style = LedgerTextStyles.Label,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}
KEOF

# ---- presentation/dashboard/DashboardUiState.kt ----
mkdir -p "$B/presentation/dashboard"
cat > "$B/presentation/dashboard/DashboardUiState.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard

data class DashboardUiState(
    val greeting: String,
    val userName: String,
    val currentMonth: String,
    val totalSpent: String,
    val budgetProgress: Float,
    val expense: String,
    val income: String,
    val savings: String,
    val recentTransactions: List<TransactionUiModel>,
    val insights: List<InsightUiModel>,
)

data class TransactionUiModel(
    val merchant: String,
    val category: String,
    val amount: String,
    val isExpense: Boolean = true,
)

data class InsightUiModel(
    val title: String,
    val subtitle: String,
)
KEOF

# ---- presentation/dashboard/components/GreetingHeader.kt ----
mkdir -p "$B/presentation/dashboard/components"
cat > "$B/presentation/dashboard/components/GreetingHeader.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Dashboard greeting shown at the top of the Home screen.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    userName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$greeting,",
            style = LedgerTextStyles.Body,
            color = LedgerTheme.colors.secondaryLabel,
        )
        Text(
            text = userName,
            style = LedgerTextStyles.Headline,
            color = LedgerTheme.colors.label,
        )
    }
}
KEOF

# ---- presentation/dashboard/components/InsightSection.kt ----
mkdir -p "$B/presentation/dashboard/components"
cat > "$B/presentation/dashboard/components/InsightSection.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionTitle
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun InsightSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        LedgerSectionTitle(text = "Insights")

        LedgerSurface(level = LedgerSurfaceLevel.Level1) {
            state.insights.forEachIndexed { index, insight ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LedgerSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
                ) {
                    Text(
                        text = insight.title,
                        style = LedgerTextStyles.Label,
                        color = LedgerTheme.colors.label,
                    )
                    Text(
                        text = insight.subtitle,
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.secondaryLabel,
                    )
                }
                if (index != state.insights.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = LedgerSpacing.Medium))
                }
            }
        }
    }
}
KEOF

# ---- presentation/dashboard/components/MonthlySummaryCard.kt ----
mkdir -p "$B/presentation/dashboard/components"
cat > "$B/presentation/dashboard/components/MonthlySummaryCard.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.hero.LedgerHeroCard

/**
 * Dashboard adapter around the reusable LedgerHeroCard.
 */
@Composable
fun MonthlySummaryCard(
    month: String,
    totalSpent: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LedgerHeroCard(
        modifier = modifier,
        title = month,
        value = totalSpent,
        subtitle = "",
        progress = progress,
    )
}
KEOF

# ---- presentation/dashboard/components/QuickStatsSection.kt ----
mkdir -p "$B/presentation/dashboard/components"
cat > "$B/presentation/dashboard/components/QuickStatsSection.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun QuickStatsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatMetric("Expense", state.expense, LedgerTheme.colors.expense)
        StatMetric("Income", state.income, LedgerTheme.colors.income)
        StatMetric("Savings", state.savings, LedgerTheme.colors.label)
    }
}

@Composable
private fun StatMetric(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Spacer(Modifier.height(LedgerSpacing.XxSmall))
        Text(
            text = value,
            style = LedgerTextStyles.Section,
            color = color,
        )
    }
}
KEOF

# ---- presentation/dashboard/components/RecentTransactionsSection.kt ----
mkdir -p "$B/presentation/dashboard/components"
cat > "$B/presentation/dashboard/components/RecentTransactionsSection.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionTitle
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        LedgerSectionTitle(text = "Recent Activity")

        LedgerSurface(
            level = LedgerSurfaceLevel.Level1,
            contentPadding = PaddingValues(horizontal = LedgerSpacing.Medium),
        ) {
            state.recentTransactions.forEachIndexed { index, txn ->
                val sign = if (txn.isExpense) "-" else "+"
                LedgerTransactionRow(
                    title = txn.merchant,
                    subtitle = txn.category,
                    amount = "$sign${txn.amount}",
                    amountColor = if (txn.isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income,
                )
                if (index != state.recentTransactions.lastIndex) {
                    LedgerHairline()
                }
            }
        }
    }
}
KEOF

# ---- presentation/dashboard/preview/DashboardPreviewData.kt ----
mkdir -p "$B/presentation/dashboard/preview"
cat > "$B/presentation/dashboard/preview/DashboardPreviewData.kt" << 'KEOF'
package com.sherif.ledger.presentation.dashboard.preview

import com.sherif.ledger.presentation.dashboard.DashboardUiState
import com.sherif.ledger.presentation.dashboard.InsightUiModel
import com.sherif.ledger.presentation.dashboard.TransactionUiModel

object DashboardPreviewData {

    val state = DashboardUiState(
        greeting = "Good Evening",
        userName = "Sherif",
        currentMonth = "June Spending",
        totalSpent = "AED 2,840.25",
        budgetProgress = 0.62f,
        expense = "AED 1,950",
        income = "AED 5,200",
        savings = "AED 3,250",
        recentTransactions = listOf(
            TransactionUiModel("Amazon", "Shopping", "AED 52", isExpense = true),
            TransactionUiModel("Carrefour", "Groceries", "AED 126", isExpense = true),
            TransactionUiModel("Costa Coffee", "Coffee", "AED 19", isExpense = true),
        ),
        insights = listOf(
            InsightUiModel("Food spending", "12% lower than last week"),
            InsightUiModel("Subscriptions", "Netflix renews tomorrow"),
        ),
    )
}
KEOF

# ---- delete LedgerCard ----
rm -f "$B/core/designsystem/component/LedgerCard.kt"

echo "Done. 19 files written, LedgerCard deleted."
echo "Run: git add -A && git commit -m \"feat(ldl): LDL foundation and LedgerCollapsingHero\""
echo "Then: ./gradlew assembleDebug"

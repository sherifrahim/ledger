#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying LDL Phase 2 audit..."
cat > "$B/core/designsystem/theme/LedgerSpacing.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LDL spacing scale.
 *
 * Numeric names remain for compatibility. Semantic aliases describe purpose
 * so future screens read as intent rather than measurement.
 */
object LedgerSpacing {
    val XxSmall: Dp = 4.dp
    val XSmall: Dp = 8.dp
    val Small: Dp = 12.dp
    val Medium: Dp = 16.dp
    val Large: Dp = 20.dp
    val XLarge: Dp = 24.dp
    val XxLarge: Dp = 32.dp
    val XxxLarge: Dp = 40.dp
    val Huge: Dp = 48.dp
    val Massive: Dp = 64.dp

    /** Inline padding within a single line or between icon and text. */
    val Inline: Dp = XxSmall
    /** Padding within a content group (between label and value). */
    val Content: Dp = XSmall
    /** Gap between related items inside a section. */
    val Group: Dp = Medium
    /** Screen edge inset. */
    val Screen: Dp = Large
    /** Vertical gap between major screen sections. */
    val Section: Dp = 28.dp
    /** Bottom safe area padding. */
    val ScreenBottom: Dp = XxxLarge
}
EOF
cat > "$B/core/designsystem/theme/LedgerMotion.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * LDL motion vocabulary.
 *
 * Every animation in Ledger should reference these instead of local constants.
 * Springs produce natural settle for user-initiated motion. Tweens produce
 * predictable timing for system-initiated events.
 */
object LedgerMotion {
    // Duration constants (ms) for simple references.
    const val Fast = 120
    const val Medium = 220
    const val Slow = 360

    // Spring presets: damping + stiffness pairs.
    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f
    const val CardSpringDamping = 0.85f
    const val CardSpringStiffness = 400f
    const val ScreenSpringDamping = 0.92f
    const val ScreenSpringStiffness = 200f

    /** Hero snap and collapse settle. Calm, weighty. */
    fun heroSpring(): SpringSpec<Float> = spring(
        dampingRatio = HeroSpringDamping,
        stiffness = HeroSpringStiffness,
    )

    /** Card expand/collapse. Slightly bouncier. */
    fun cardSpring(): SpringSpec<Float> = spring(
        dampingRatio = CardSpringDamping,
        stiffness = CardSpringStiffness,
    )

    /** Full screen transitions. Very smooth, slow settle. */
    fun screenSpring(): SpringSpec<Float> = spring(
        dampingRatio = ScreenSpringDamping,
        stiffness = ScreenSpringStiffness,
    )

    // Tween presets for system-initiated animations.
    const val FastTweenMs = 150
    const val StandardTweenMs = 250
    const val SlowTweenMs = 400
}
EOF
cat > "$B/core/designsystem/tokens/LedgerOpacity.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.tokens

/**
 * LDL opacity tokens.
 *
 * Named opacities replace scattered magic alpha values. Each token describes
 * an intent so composables read as meaning rather than measurement.
 */
object LedgerOpacity {
    /** Non-interactive content that must remain visible. */
    const val Disabled = 0.38f
    /** Tinted fills behind avatars and swatches. */
    const val Fill = 0.12f
    /** Barely visible backgrounds (ghost buttons, subtle containers). */
    const val Subtle = 0.06f
    /** Quiet supporting text and icons (hero labels, metadata). */
    const val Muted = 0.22f
    /** Secondary content visible but not prominent. */
    const val Secondary = 0.45f
    /** Scrim or overlay behind modals. */
    const val Overlay = 0.60f
}
EOF
mkdir -p "$B/core/designsystem/tokens"
cat > "$B/core/designsystem/tokens/LedgerIconSize.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LDL icon size vocabulary.
 *
 * Components reference these instead of raw dp so icon density stays
 * consistent across screens.
 */
object LedgerIconSize {
    val Small: Dp = 16.dp
    val Medium: Dp = 20.dp
    val Large: Dp = 24.dp
    val Navigation: Dp = 24.dp
}
EOF
cat > "$B/core/designsystem/tokens/LedgerRadius.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * LDL corner radius vocabulary.
 *
 * Pre-built shapes for Compose modifiers plus raw Dp values for cases
 * where a shape object is not accepted (e.g. lerp interpolation in the
 * collapsing hero). This is the single authority for LDL radii.
 *
 * Note: [com.sherif.ledger.core.designsystem.theme.LedgerShapes] feeds
 * Material components via the Material Shapes API and carries its own
 * values. Those are toolkit plumbing, not LDL identity.
 */
object LedgerRadius {
    val SmallDp: Dp = 16.dp
    val MediumDp: Dp = 24.dp
    val LargeDp: Dp = 32.dp
    val XLargeDp: Dp = 40.dp
    val FullDp: Dp = 100.dp

    val Small = RoundedCornerShape(SmallDp)
    val Medium = RoundedCornerShape(MediumDp)
    val Large = RoundedCornerShape(LargeDp)
    val XLarge = RoundedCornerShape(XLargeDp)
    val Full = RoundedCornerShape(FullDp)
}
EOF
cat > "$B/core/designsystem/theme/LedgerTheme.kt" << 'EOF'
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
import com.sherif.ledger.core.designsystem.tokens.LedgerIconSize
import com.sherif.ledger.core.designsystem.tokens.LedgerOpacity
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Applies the Ledger Design Language.
 *
 * Material3 remains underneath as a toolkit. LDL components read
 * [LedgerTheme] for semantic decisions, not Material color roles.
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
 * Single semantic entry point for LDL.
 *
 * Only [colors] varies with light/dark (CompositionLocal). Everything
 * else is stable tokens exposed directly.
 */
object LedgerTheme {
    val colors: LedgerColors
        @Composable @ReadOnlyComposable get() = LocalLedgerColors.current
    val radius get() = LedgerRadius
    val border get() = LedgerBorder
    val opacity get() = LedgerOpacity
    val motion get() = LedgerMotion
    val elevation get() = LedgerElevation
    val iconSize get() = LedgerIconSize
}
EOF

cat > "$B/core/designsystem/component/LedgerButton.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

enum class LedgerButtonStyle { Primary, Secondary, Text }

@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: LedgerButtonStyle = LedgerButtonStyle.Primary,
) {
    val contentPadding = PaddingValues(horizontal = LedgerSpacing.XLarge, vertical = LedgerSpacing.Small)
    val shape = LedgerShapes.small
    when (style) {
        LedgerButtonStyle.Primary -> Button(onClick, modifier, enabled, shape = shape, contentPadding = contentPadding) { Text(text) }
        LedgerButtonStyle.Secondary -> OutlinedButton(onClick, modifier, enabled, shape = shape, contentPadding = contentPadding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerTheme.colors.tint)) { Text(text) }
        LedgerButtonStyle.Text -> TextButton(onClick, modifier, enabled, shape = shape, contentPadding = contentPadding) { Text(text) }
    }
}
EOF

cat > "$B/core/designsystem/component/LedgerTag.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LedgerTheme.colors.tint.copy(alpha = LedgerTheme.opacity.Fill),
    contentColor: Color = LedgerTheme.colors.tint,
) {
    Surface(modifier = modifier, shape = LedgerShapes.small, color = containerColor, contentColor = contentColor) {
        Text(text, Modifier.padding(PaddingValues(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.XxSmall)), style = LedgerTextStyles.Caption)
    }
}
EOF

cat > "$B/core/designsystem/component/LedgerEmptyState.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        if (illustration != null) illustration()
        Text(title, style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)
        if (message != null) Text(message, style = LedgerTextStyles.Body, color = LedgerTheme.colors.secondaryLabel)
        if (action != null) action()
    }
}
EOF

cat > "$B/core/designsystem/component/LedgerLoading.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerLoading(modifier: Modifier = Modifier, message: String? = null) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        CircularProgressIndicator(color = LedgerTheme.colors.tint)
        if (message != null) Text(message, style = LedgerTextStyles.Body, color = LedgerTheme.colors.secondaryLabel)
    }
}
EOF

cat > "$B/core/designsystem/component/LedgerSearchBar.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true,
    searchIcon: ImageVector = Icons.Filled.Search,
    clearIcon: ImageVector = Icons.Filled.Close,
) {
    val colors = LedgerTheme.colors
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        textStyle = LedgerTextStyles.Body,
        placeholder = { Text(placeholder, style = LedgerTextStyles.Body) },
        leadingIcon = { Icon(searchIcon, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(clearIcon, "Clear search") }
        },
        shape = LedgerShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.tint,
            unfocusedBorderColor = colors.separator,
            focusedContainerColor = colors.surfaceLevel1,
            unfocusedContainerColor = colors.surfaceLevel1,
        ),
    )
}
EOF

cat > "$B/core/designsystem/component/LedgerTopBar.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LedgerTheme.colors
    CenterAlignedTopAppBar(
        title = { Text(title, style = LedgerTextStyles.Section) },
        modifier = modifier, navigationIcon = navigationIcon, actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colors.surfaceLevel0,
            titleContentColor = colors.label,
            navigationIconContentColor = colors.label,
            actionIconContentColor = colors.label,
        ),
    )
}
EOF

echo "Done. 12 files written."
echo "Run: git add -A && git commit -m 'refactor(ldl): phase 2 audit - semantic tokens, icon sizes, component migration'"
echo "Then: ./gradlew assembleDebug"

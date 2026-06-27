#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying LDL v1.1 refinement..."

cat > "$B/core/designsystem/theme/LedgerColors.kt" << 'EOF'
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

enum class LedgerSurfaceLevel { Level0, Level1, Level2, Level3 }

/**
 * The LDL semantic color contract, read through [LedgerTheme.colors].
 *
 * Hero atmosphere tokens define the ambient glow behind the balance.
 * They are base hues; composables apply their own alpha per layer to
 * build depth without hardcoding raw color literals.
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
    val heroGlowPrimary: Color,
    val heroGlowSecondary: Color,
    val heroGlowWarm: Color,
) {
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
    heroGlowPrimary = Color(0xFFD4EDE7),
    heroGlowSecondary = Color(0xFFC8E6DD),
    heroGlowWarm = Color(0xFFF0EDE0),
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
    heroGlowPrimary = Color(0xFF122624),
    heroGlowSecondary = Color(0xFF0A2D2A),
    heroGlowWarm = Color(0xFF1C1C16),
)

val LocalLedgerColors = staticCompositionLocalOf { LedgerLightColors }
EOF

cat > "$B/presentation/dashboard/DashboardScreen.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.hero.LedgerCollapsingHero
import com.sherif.ledger.core.designsystem.component.hero.LedgerHeroDefaults
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.components.InsightSection
import com.sherif.ledger.presentation.dashboard.components.QuickStatsSection
import com.sherif.ledger.presentation.dashboard.components.RecentTransactionsSection
import com.sherif.ledger.presentation.dashboard.preview.DashboardPreviewData

private object HeroTransitions {
    const val TopBarExit = 0.25f
    const val TopBarTranslationPx = 60f
    const val BalanceLabelExit = 0.35f
    const val CurrencyExit = 0.4f
    const val ExpandedExit = 0.65f
    const val CompactEnter = 0.55f
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
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / maxOffsetPx).coerceIn(0f, 1f)
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

    val colors = LedgerTheme.colors

    Box(Modifier.fillMaxSize().background(colors.surfaceLevel0)) {

        // Aurora atmosphere: multiple diffuse layers, no visible geometry.
        Canvas(Modifier.fillMaxSize()) {
            // Broad teal wash across the upper third
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.heroGlowPrimary.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.02f),
                    radius = size.width * 1.3f,
                ),
            )
            // Secondary band, offset left and slightly lower
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.heroGlowSecondary.copy(alpha = 0.3f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.10f),
                    radius = size.width * 0.7f,
                ),
            )
            // Warm highlight, offset right, barely visible
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.heroGlowWarm.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.06f),
                    radius = size.width * 0.5f,
                ),
            )
            // Deep secondary bloom below the primary, very faint
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.heroGlowSecondary.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.6f, size.height * 0.18f),
                    radius = size.width * 0.55f,
                ),
            )
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero_spacer") { Spacer(Modifier.height(expandedHeight)) }
            item(key = "stats") { QuickStatsSection(state) }
            item(key = "transactions") { RecentTransactionsSection(state) }
            item(key = "insights") { InsightSection(state) }
        }

        LedgerCollapsingHero(
            collapseProgress = collapseProgress,
            background = SolidColor(Color.Transparent),
            expandedContent = { progress -> ExpandedHero(progress, state) },
            compactContent = { progress -> CompactHero(progress, state) },
        )
    }
}

@Composable
private fun ExpandedHero(progress: Float, state: DashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 40.dp)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar: notification + avatar, right-aligned. No greeting.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.TopBarExit).coerceIn(0f, 1f)
                    translationY = -(progress * HeroTransitions.TopBarTranslationPx)
                },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.userName.take(1),
                    style = LedgerTextStyles.Caption,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // The money. Centered. Nothing competes.
        Text(
            "Total balance",
            style = LedgerTextStyles.Caption,
            color = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.graphicsLayer {
                alpha = (1f - progress / HeroTransitions.BalanceLabelExit).coerceIn(0f, 1f)
            },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            state.balanceAmount,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 54.sp,
                lineHeight = 58.sp,
                letterSpacing = (-1.5).sp,
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.graphicsLayer {
                alpha = (1f - progress / HeroTransitions.CurrencyExit).coerceIn(0f, 1f)
            },
        ) {
            Text(
                state.balanceCurrency,
                style = LedgerTextStyles.Body,
                color = Color.White.copy(alpha = 0.25f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.weight(0.5f))
    }
}

@Composable
private fun CompactHero(progress: Float, state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(horizontal = 20.dp)
            .graphicsLayer {
                alpha = ((progress - HeroTransitions.CompactEnter) /
                    (1f - HeroTransitions.CompactEnter)).coerceIn(0f, 1f)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${state.balanceCurrency} ${state.balanceAmount}",
            style = LedgerTextStyles.Section,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}
EOF

cat > "$B/presentation/dashboard/components/QuickStatsSection.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatMetric("Income", state.income, LedgerTheme.colors.income)
        StatMetric("Expense", state.expense, LedgerTheme.colors.expense)
        StatMetric("Savings", state.savings, LedgerTheme.colors.secondaryLabel)
    }
}

@Composable
private fun StatMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Spacer(Modifier.height(4.dp))
        Text(value, style = LedgerTextStyles.Section, color = color)
    }
}
EOF

cat > "$B/presentation/dashboard/components/InsightSection.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
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
    if (state.insights.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
        Text("Insights", style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)

        LedgerSurface(
            level = LedgerSurfaceLevel.Level1,
            contentPadding = PaddingValues(0.dp),
        ) {
            state.insights.forEachIndexed { index, insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(insight.title, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                        Text(insight.subtitle, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (insight.indicator.isNotEmpty()) {
                        MiniSparkline()
                        Spacer(Modifier.width(8.dp))
                        Text(insight.indicator, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.income)
                    } else {
                        Icon(
                            Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = LedgerTheme.colors.tertiaryLabel,
                        )
                    }
                }
                if (index != state.insights.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun MiniSparkline(modifier: Modifier = Modifier) {
    val color = LedgerTheme.colors.income
    Canvas(modifier = modifier.width(44.dp).height(18.dp)) {
        val points = listOf(0.7f, 0.85f, 0.6f, 0.75f, 0.45f, 0.5f, 0.3f)
        val stepX = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height * (1f - v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}
EOF

echo "Done. 4 files written."
echo "Run: git add -A && git commit -m 'fix(home): v1.1 tokenize atmosphere, center balance, strip decoration'"
echo "Then: ./gradlew assembleDebug"

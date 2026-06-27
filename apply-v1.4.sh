#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying LDL v1.4: hero identity pass..."

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

val LedgerInk = Color(0xFF111111)
val LedgerGraphite = Color(0xFF525252)
val LedgerMist = Color(0xFFF5F5F5)
val LedgerPaper = Color(0xFFFFFFFF)
val LedgerLine = Color(0xFFE0E0E0)
val LedgerDarkInk = Color(0xFFE8E8E8)
val LedgerDarkGraphite = Color(0xFFA0A0A0)
val LedgerDarkSurface = Color(0xFF111111)
val LedgerDarkSurfaceHigh = Color(0xFF191919)
val LedgerDarkBackground = Color(0xFF0A0A0A)
val LedgerDarkLine = Color(0xFF222222)
val LedgerSuccess = Color(0xFF047857)
val LedgerWarning = Color(0xFFB7791F)
val LedgerError = Color(0xFFBA1A1A)

val LedgerLightColorScheme = lightColorScheme(
    primary = LedgerEmerald700, onPrimary = Color.White,
    primaryContainer = LedgerEmerald100, onPrimaryContainer = LedgerEmerald950,
    secondary = Color(0xFF555555), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0), onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = Color(0xFF505060), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDDDE8), onTertiaryContainer = Color(0xFF1A1A22),
    error = LedgerError, onError = Color.White,
    background = LedgerMist, onBackground = LedgerInk,
    surface = LedgerPaper, onSurface = LedgerInk,
    surfaceVariant = Color(0xFFECECEC), onSurfaceVariant = LedgerGraphite,
    outline = Color(0xFF787878), outlineVariant = LedgerLine,
    inverseSurface = LedgerInk, inverseOnSurface = LedgerMist,
    inversePrimary = LedgerEmerald300, scrim = Color.Black,
)

val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerEmerald300, onPrimary = LedgerEmerald950,
    primaryContainer = LedgerEmerald800, onPrimaryContainer = LedgerEmerald100,
    secondary = Color(0xFFBBBBBB), onSecondary = Color(0xFF2A2A2A),
    secondaryContainer = Color(0xFF3A3A3A), onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFB0B0C0), onTertiary = Color(0xFF1E1E28),
    tertiaryContainer = Color(0xFF363646), onTertiaryContainer = Color(0xFFDDDDE8),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    background = LedgerDarkBackground, onBackground = LedgerDarkInk,
    surface = LedgerDarkSurface, onSurface = LedgerDarkInk,
    surfaceVariant = LedgerDarkSurfaceHigh, onSurfaceVariant = LedgerDarkGraphite,
    outline = Color(0xFF888888), outlineVariant = LedgerDarkLine,
    inverseSurface = LedgerDarkInk, inverseOnSurface = LedgerDarkBackground,
    inversePrimary = LedgerEmerald700, scrim = Color.Black,
)

enum class LedgerSurfaceLevel { Level0, Level1, Level2, Level3 }

/**
 * LDL semantic color contract.
 *
 * Hero glow tokens define the volumetric light that illuminates the balance.
 * [heroGlowPrimary] is the dominant teal light source. [heroGlowSecondary]
 * adds depth where the light is brightest. [heroGlowCool] introduces
 * blue-grey temperature so the atmosphere reads as rich neutral, not
 * monochrome. [heroGlowWarm] provides a faint counter-temperature at the
 * shadow edge. Composables apply alpha per layer to build depth.
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
    val heroGlowCool: Color,
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
    surfaceLevel0 = Color(0xFFF3F3F3), surfaceLevel1 = Color(0xFFFFFFFF),
    surfaceLevel2 = Color(0xFFF7F7F7), surfaceLevel3 = Color(0xFFFFFFFF),
    label = Color(0xFF111111), secondaryLabel = Color(0xFF636363),
    tertiaryLabel = Color(0xFF8E8E8E), separator = Color(0xFFE0E0E0),
    tint = LedgerEmerald700, onTint = Color(0xFFFFFFFF),
    income = LedgerEmerald700, expense = Color(0xFFC0362C),
    pending = Color(0xFFB7791F), refund = Color(0xFF2563EB),
    warning = Color(0xFFB7791F), success = LedgerEmerald700,
    neutral = Color(0xFF737373),
    heroGlowPrimary = Color(0xFFB8E0D8), heroGlowSecondary = Color(0xFFA0D4CA),
    heroGlowCool = Color(0xFFBCC8D4), heroGlowWarm = Color(0xFFE8E0C8),
)

val LedgerDarkColors = LedgerColors(
    isDark = true,
    surfaceLevel0 = Color(0xFF0A0A0A), surfaceLevel1 = Color(0xFF111111),
    surfaceLevel2 = Color(0xFF191919), surfaceLevel3 = Color(0xFF212121),
    label = Color(0xFFE8E8E8), secondaryLabel = Color(0xFFA0A0A0),
    tertiaryLabel = Color(0xFF6E6E6E), separator = Color(0xFF222222),
    tint = LedgerEmerald300, onTint = LedgerEmerald950,
    income = LedgerEmerald300, expense = Color(0xFFF97066),
    pending = Color(0xFFF0B860), refund = Color(0xFF60A5FA),
    warning = Color(0xFFF0B860), success = LedgerEmerald300,
    neutral = Color(0xFF8A8A8A),
    heroGlowPrimary = Color(0xFF1B6B62),
    heroGlowSecondary = Color(0xFF0F7D74),
    heroGlowCool = Color(0xFF3A5068),
    heroGlowWarm = Color(0xFF4A4830),
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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

        // Volumetric light. The light enters from the upper-left, travels
        // diagonally across the hero, and dissipates toward the lower-right.
        // No layer should be individually perceivable. The eye should only
        // register atmosphere.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Directional wash: a wide linear gradient traveling upper-left
            //    to lower-right. This sets the overall light direction.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.heroGlowPrimary.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    start = Offset(w * 0.05f, 0f),
                    end = Offset(w * 0.9f, h * 0.35f),
                ),
                size = Size(w, h * 0.4f),
            )

            // 2. Cool temperature band: blue-grey streak running parallel to
            //    the main light but offset upward. Prevents monochrome.
            rotate(degrees = -18f, pivot = Offset(w * 0.4f, h * 0.06f)) {
                scale(scaleX = 3.5f, scaleY = 0.5f, pivot = Offset(w * 0.4f, h * 0.06f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.heroGlowCool.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(w * 0.4f, h * 0.06f),
                            radius = h * 0.10f,
                        ),
                    )
                }
            }

            // 3. Brightest point: a concentrated teal where the light is
            //    strongest, off-center toward upper-left.
            rotate(degrees = -25f, pivot = Offset(w * 0.35f, h * 0.10f)) {
                scale(scaleX = 2.5f, scaleY = 0.6f, pivot = Offset(w * 0.35f, h * 0.10f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.heroGlowSecondary.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(w * 0.35f, h * 0.10f),
                            radius = h * 0.09f,
                        ),
                    )
                }
            }

            // 4. Light spill: faint continuation of the main band toward
            //    center-right, creating the sense that light traveled through.
            rotate(degrees = -12f, pivot = Offset(w * 0.65f, h * 0.13f)) {
                scale(scaleX = 2.8f, scaleY = 0.4f, pivot = Offset(w * 0.65f, h * 0.13f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.heroGlowPrimary.copy(alpha = 0.09f), Color.Transparent),
                            center = Offset(w * 0.65f, h * 0.13f),
                            radius = h * 0.08f,
                        ),
                    )
                }
            }

            // 5. Warm shadow edge: very faint warm tone at the lower boundary
            //    where the light fades. Creates temperature contrast.
            rotate(degrees = 5f, pivot = Offset(w * 0.5f, h * 0.22f)) {
                scale(scaleX = 4f, scaleY = 0.3f, pivot = Offset(w * 0.5f, h * 0.22f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.heroGlowWarm.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(w * 0.5f, h * 0.22f),
                            radius = h * 0.07f,
                        ),
                    )
                }
            }

            // 6. Secondary cool wisp: a thin streak above the main band,
            //    adding layered depth to the atmosphere.
            rotate(degrees = -30f, pivot = Offset(w * 0.55f, h * 0.03f)) {
                scale(scaleX = 3f, scaleY = 0.3f, pivot = Offset(w * 0.55f, h * 0.03f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.heroGlowCool.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(w * 0.55f, h * 0.03f),
                            radius = h * 0.06f,
                        ),
                    )
                }
            }

            // 7. Deep ambient: extremely faint primary wash low in the hero
            //    zone so the transition to content is not an abrupt cut.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, colors.heroGlowPrimary.copy(alpha = 0.04f), Color.Transparent),
                    start = Offset(0f, h * 0.15f),
                    end = Offset(w, h * 0.30f),
                ),
                size = Size(w, h * 0.35f),
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
                tint = Color.White.copy(alpha = 0.30f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.userName.take(1),
                    style = LedgerTextStyles.Caption,
                    color = Color.White.copy(alpha = 0.35f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Total balance",
            style = LedgerTextStyles.Caption,
            color = Color.White.copy(alpha = 0.25f),
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
                color = Color.White.copy(alpha = 0.20f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.20f),
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

echo "Done. 2 files written."
echo "Run: git add -A && git commit -m 'feat(home): v1.4 directional volumetric light with temperature depth'"
echo "Then: ./gradlew assembleDebug"

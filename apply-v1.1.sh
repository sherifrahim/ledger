#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying LDL v1.1 refinement..."

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

    Box(Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0)) {

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF142A28).copy(alpha = 0.6f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.04f),
                    radius = size.width * 1.0f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0C2E2B).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * 0.10f),
                    radius = size.width * 0.6f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1E1E18).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.07f),
                    radius = size.width * 0.45f,
                ),
            )
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 40.dp,
            ),
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
            .padding(top = 12.dp, bottom = 36.dp)
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
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.userName.take(1),
                    style = LedgerTextStyles.Caption,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Total balance",
            style = LedgerTextStyles.Caption,
            color = Color.White.copy(alpha = 0.35f),
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
                color = Color.White.copy(alpha = 0.3f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
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

echo "Done. 3 files written."
echo "Run: git add -A && git commit -m 'fix(home): v1.1 center balance, remove decoration, atmospheric depth'"
echo "Then: ./gradlew assembleDebug"

package com.sherif.ledger.presentation.dashboard

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
    const val DampingRatio = 0.88f
    const val Stiffness = 300f
}

@Composable
fun DashboardScreen(
    onNavigateToTransactions: () -> Unit = {},
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

    val c = LedgerTheme.colors

    Box(Modifier.fillMaxSize().background(c.surfaceLevel0)) {

        // The light enters from the upper-left, outside the frame.
        // It travels diagonally across the hero, illuminating the
        // balance from above-left, then dissipates past the hero
        // boundary into the first content section so the page reads
        // as one continuous scene.
        //
        // No layer should be individually perceivable. The right side
        // of the hero sits in relative shadow. The left carries the
        // light. The balance is in the lit zone but is not the source.

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Entry wash: light entering the frame from upper-left.
            //    A diagonal linear gradient that establishes direction.
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        c.heroGlowPrimary.copy(alpha = 0.22f),
                        c.heroGlowPrimary.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w * 0.85f, h * 0.32f),
                ),
                size = Size(w, h * 0.38f),
            )

            // 2. Light core: the brightest concentration, upper-left,
            //    where the beam enters. Stretched into a diagonal band.
            rotate(-22f, Offset(w * 0.22f, h * 0.08f)) {
                scale(3.0f, 0.5f, Offset(w * 0.22f, h * 0.08f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.heroGlowSecondary.copy(alpha = 0.18f), Color.Transparent),
                            Offset(w * 0.22f, h * 0.08f), h * 0.09f,
                        ),
                    )
                }
            }

            // 3. Cool temperature: blue-grey wisp above the main beam,
            //    offset right, adding temperature variation.
            rotate(-28f, Offset(w * 0.48f, h * 0.04f)) {
                scale(3.2f, 0.35f, Offset(w * 0.48f, h * 0.04f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.heroGlowCool.copy(alpha = 0.10f), Color.Transparent),
                            Offset(w * 0.48f, h * 0.04f), h * 0.07f,
                        ),
                    )
                }
            }

            // 4. Mid-path: the light traveling through the scene, now
            //    weaker, approaching center. Illuminates the balance zone.
            rotate(-15f, Offset(w * 0.45f, h * 0.12f)) {
                scale(2.6f, 0.45f, Offset(w * 0.45f, h * 0.12f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.heroGlowPrimary.copy(alpha = 0.10f), Color.Transparent),
                            Offset(w * 0.45f, h * 0.12f), h * 0.08f,
                        ),
                    )
                }
            }

            // 5. Light spill: faint continuation past center toward
            //    right, the beam losing energy.
            rotate(-10f, Offset(w * 0.7f, h * 0.16f)) {
                scale(2.2f, 0.35f, Offset(w * 0.7f, h * 0.16f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.heroGlowPrimary.copy(alpha = 0.05f), Color.Transparent),
                            Offset(w * 0.7f, h * 0.16f), h * 0.06f,
                        ),
                    )
                }
            }

            // 6. Warm shadow: faint warm tone at the lower boundary
            //    where the light fades. Opposite temperature from cool.
            rotate(8f, Offset(w * 0.55f, h * 0.24f)) {
                scale(3.5f, 0.3f, Offset(w * 0.55f, h * 0.24f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.heroGlowWarm.copy(alpha = 0.05f), Color.Transparent),
                            Offset(w * 0.55f, h * 0.24f), h * 0.06f,
                        ),
                    )
                }
            }

            // 7. Surface continuity: the atmosphere dissolves into the
            //    content zone so the hero never feels like it "ends."
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        c.heroGlowPrimary.copy(alpha = 0.04f),
                        c.heroGlowCool.copy(alpha = 0.02f),
                        Color.Transparent,
                    ),
                    start = Offset(w * 0.1f, h * 0.20f),
                    end = Offset(w * 0.8f, h * 0.42f),
                ),
                topLeft = Offset(0f, h * 0.18f),
                size = Size(w, h * 0.25f),
            )

            // 8. Secondary cool wisp: thin streak at a steeper angle,
            //    adding layered depth near the top edge.
            rotate(-35f, Offset(w * 0.6f, h * 0.02f)) {
                scale(2.5f, 0.25f, Offset(w * 0.6f, h * 0.02f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.heroGlowCool.copy(alpha = 0.06f), Color.Transparent),
                            Offset(w * 0.6f, h * 0.02f), h * 0.05f,
                        ),
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "hero_spacer") { Spacer(Modifier.height(expandedHeight)) }
            item(key = "stats") { QuickStatsSection(state) }
            item(key = "transactions") { RecentTransactionsSection(state, onSeeAllClick = onNavigateToTransactions) }
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
                tint = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.userName.take(1),
                    style = LedgerTextStyles.Caption,
                    color = Color.White.copy(alpha = 0.25f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Total balance",
            style = LedgerTextStyles.Caption,
            color = Color.White.copy(alpha = 0.22f),
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
                color = Color.White.copy(alpha = 0.18f),
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.18f),
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

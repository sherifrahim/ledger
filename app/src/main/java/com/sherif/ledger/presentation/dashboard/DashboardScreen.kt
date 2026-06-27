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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Dashboard-specific hero transition thresholds and snap configuration.
 *
 * These control how individual content elements within the hero fade and
 * translate during collapse. They are intentionally separate from
 * [LedgerHeroDefaults] because they describe dashboard content
 * choreography, not the reusable container's structure.
 *
 * All values are hypotheses. Tune after device testing.
 */
private object HeroTransitions {
    /** Progress at which the greeting fully exits (fades + translates up). */
    const val GreetingExit = 0.3f
    /** Upward translation in pixels applied to the greeting at full collapse. */
    const val GreetingTranslationPx = 80f
    /** Progress at which the month label fully exits. */
    const val MonthExit = 0.5f
    /** Progress at which the expanded content as a whole fully exits. */
    const val ExpandedExit = 0.7f
    /** Progress at which the budget progress bar fully exits. */
    const val ProgressExit = 0.25f
    /** Progress at which the compact header begins entering. */
    const val CompactEnter = 0.6f
}

/**
 * Snap behavior configuration. Experimental: if snapping feels artificial
 * compared to free-scrolling iOS interactions, set [Enabled] to false or
 * remove the snap block entirely.
 */
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

// ---- Dashboard-specific hero content (not reusable) ----

@Composable
private fun ExpandedHeroContent(progress: Float, state: DashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = (1f - progress / HeroTransitions.GreetingExit).coerceIn(0f, 1f)
                translationY = -(progress * HeroTransitions.GreetingTranslationPx)
            },
        ) {
            Text(
                text = "${state.greeting},",
                style = LedgerTextStyles.Body,
                color = Color.White.copy(alpha = 0.72f),
            )
            Text(
                text = state.userName,
                style = LedgerTextStyles.Headline,
                color = Color.White,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = state.currentMonth,
                style = LedgerTextStyles.Label,
                color = Color.White.copy(alpha = 0.80f),
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.MonthExit).coerceIn(0f, 1f)
                },
            )
            Text(
                text = state.totalSpent,
                style = LedgerTextStyles.Amount,
                color = Color.White,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ProgressExit).coerceIn(0f, 1f)
            },
        ) {
            LinearProgressIndicator(
                progress = { state.budgetProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.20f),
            )
            Text(
                text = "${(state.budgetProgress * 100).toInt()}% of monthly budget",
                style = LedgerTextStyles.Caption,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun CompactHeroContent(progress: Float, state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            text = "${state.currentMonth} \u00B7 ${(state.budgetProgress * 100).toInt()}%",
            style = LedgerTextStyles.Label,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

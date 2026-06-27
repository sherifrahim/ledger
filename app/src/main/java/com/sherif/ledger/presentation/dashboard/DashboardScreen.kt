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

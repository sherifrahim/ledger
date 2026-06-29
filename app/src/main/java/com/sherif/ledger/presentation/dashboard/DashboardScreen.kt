package com.sherif.ledger.presentation.dashboard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereGlow
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.hero.LedgerCollapsingHero
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.component.hero.LedgerHeroDefaults
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.presentation.dashboard.components.InsightSection
import com.sherif.ledger.presentation.dashboard.components.QuickStatsSection
import com.sherif.ledger.presentation.dashboard.components.RecentTransactionsSection
import com.sherif.ledger.presentation.dashboard.preview.DashboardPreviewData

private object HeroTransitions {
    const val TopBarExit = 0.20f
    const val TopBarTranslationPx = 40f
    const val BalanceLabelExit = 0.30f
    const val AmountExit = 0.50f
    const val CurrencyExit = 0.55f
    const val ExpandedExit = 0.60f
    const val CompactEnter = 0.65f
}

private object HeroSnap {
    const val Enabled = true
    const val ZoneStart = 0.25f
    const val ZoneEnd = 0.75f
    const val DampingRatio = 0.92f
    const val Stiffness = 200f
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

        LedgerAtmosphereGlow(Modifier.fillMaxSize())

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = LedgerSpacing.Group,
                end = LedgerSpacing.Group,
                bottom = LedgerSpacing.ScreenBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
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
            .padding(horizontal = LedgerSpacing.Group)
            .padding(top = LedgerSpacing.Medium, bottom = LedgerSpacing.XxLarge)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.greeting,
                    style = LedgerTextStyles.Caption,
                    color = Color.White.copy(alpha = 0.35f),
                )
                Text(
                    text = state.userName,
                    style = LedgerTextStyles.Title,
                    color = Color.White.copy(alpha = 0.60f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier
                        .size(LedgerTheme.iconSize.Small)
                        .ledgerClickable { /* TODO */ },
                )
                Spacer(Modifier.width(LedgerSpacing.Medium))
                Box(
                    modifier = Modifier
                        .size(LedgerTheme.iconSize.Large)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .ledgerClickable { /* TODO */ },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.userName.take(1),
                        style = LedgerTextStyles.Caption,
                        color = Color.White.copy(alpha = 0.25f),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Account total",
                style = LedgerTextStyles.Caption,
                color = Color.White.copy(alpha = 0.40f),
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.BalanceLabelExit).coerceIn(0f, 1f)
                },
            )
            Spacer(Modifier.height(LedgerSpacing.XSmall))
            LedgerAmount(
                amount = "${state.balanceCurrency} ${state.balanceAmount}",
                style = LedgerAmountStyle.Display,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = (1f - progress / HeroTransitions.AmountExit).coerceIn(0f, 1f)
                        scaleX = 1f - (progress * 0.08f)
                        scaleY = 1f - (progress * 0.08f)
                        translationX = -(progress * 20f)
                    },
            )
            Spacer(Modifier.height(LedgerSpacing.XxSmall))
            Text(
                text = state.currentMonth,
                style = LedgerTextStyles.Mono.copy(fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.CurrencyExit).coerceIn(0f, 1f)
                },
            )
        }

        Spacer(Modifier.weight(0.4f))
    }
}

@Composable
private fun CompactHero(progress: Float, state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(horizontal = LedgerSpacing.Group)
            .graphicsLayer {
                alpha = ((progress - HeroTransitions.CompactEnter) /
                    (1f - HeroTransitions.CompactEnter)).coerceIn(0f, 1f)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "June Balance",
            style = LedgerTextStyles.Label,
            color = LedgerTheme.colors.secondaryLabel,
        )
        LedgerAmount(
            amount = "${state.balanceCurrency} ${state.balanceAmount}",
            style = LedgerAmountStyle.Regular,
            color = LedgerTheme.colors.label,
        )
    }
}

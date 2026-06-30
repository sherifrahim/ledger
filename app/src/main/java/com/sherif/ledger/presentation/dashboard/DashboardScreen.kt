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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereGlow
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.component.hero.LedgerCollapsingHero
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
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
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.Group)
            .padding(top = LedgerSpacing.Medium, bottom = LedgerSpacing.XxLarge)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Identity Frame
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Good morning, ${state.userName} 👋",
                style = LedgerTextStyles.Label,
                color = Color.White.copy(alpha = 0.60f),
            )
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = Color.White.copy(alpha = 0.40f),
                modifier = Modifier
                    .size(LedgerTheme.iconSize.Small)
                    .ledgerClickable { /* TODO */ },
            )
        }

        Spacer(Modifier.weight(1f))

        // Total Balance (Centered per Mockup)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL BALANCE",
                style = LedgerTextStyles.Caption.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
                color = Color.White.copy(alpha = 0.40f),
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.BalanceLabelExit).coerceIn(0f, 1f)
                    translationY = -(progress * 20f)
                },
            )
            Spacer(Modifier.height(LedgerSpacing.XxSmall))
            LedgerAmount(
                amount = state.balanceAmount,
                style = LedgerAmountStyle.Display,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.AmountExit).coerceIn(0f, 1f)
                    val scale = 1f - (progress * 0.12f)
                    scaleX = scale
                    scaleY = scale
                    translationY = -(progress * 40f)
                },
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
            // Currency Pill (Restored per Mockup)
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = (1f - progress / HeroTransitions.CurrencyExit).coerceIn(0f, 1f)
                        translationY = -(progress * 30f)
                    }
                    .ledgerSurface(
                        backgroundColor = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.White.copy(alpha = 0.15f),
                        shape = LedgerRadius.Full,
                    )
                    .ledgerClickable { /* TODO: Currency Picker */ }
                    .padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.XxSmall),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.balanceCurrency,
                        style = LedgerTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.width(LedgerSpacing.XxSmall))
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Large))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MetricItem(
                label = "INCOME",
                value = state.income,
                change = state.incomeChange,
                icon = Icons.AutoMirrored.Filled.ArrowForward, // Placeholder for Upward
                color = LedgerTheme.colors.income,
            )
            MetricItem(
                label = "EXPENSES",
                value = state.expense,
                change = state.expenseChange,
                icon = Icons.Filled.KeyboardArrowDown, // Placeholder for Downward
                color = LedgerTheme.colors.expense,
            )
            MetricItem(
                label = "SAVINGS",
                value = state.savings,
                change = state.savingsChange,
                icon = Icons.Filled.Star, // Placeholder for Safe
                color = Color.White.copy(alpha = 0.70f),
            )
        }

        Spacer(Modifier.weight(0.4f))
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    change: String,
    icon: ImageVector,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(LedgerTheme.iconSize.Medium)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = label,
            style = LedgerTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.35f),
        )
        Text(
            text = value,
            style = LedgerTextStyles.Label.copy(fontFamily = FontFamily.Monospace),
            color = color,
        )
        Text(
            text = change,
            style = LedgerTextStyles.Caption.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = color.copy(alpha = 0.8f),
        )
        Text(
            text = "vs last month",
            style = LedgerTextStyles.Caption.copy(fontSize = 8.sp),
            color = Color.White.copy(alpha = 0.2f),
        )
    }
}

@Composable
private fun CompactHero(progress: Float, state: DashboardUiState) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.Group)
            .graphicsLayer {
                alpha = ((progress - HeroTransitions.CompactEnter) /
                    (1f - HeroTransitions.CompactEnter)).coerceIn(0f, 1f)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Total balance",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )
            LedgerAmount(
                amount = "${state.balanceCurrency} ${state.balanceAmount}",
                style = LedgerAmountStyle.Regular,
                color = LedgerTheme.colors.label,
            )
        }
        
        // Compact secondary info
        Text(
            text = state.currentMonth,
            style = LedgerTextStyles.Mono.copy(fontSize = 11.sp),
            color = LedgerTheme.colors.tertiaryLabel,
        )
    }
}

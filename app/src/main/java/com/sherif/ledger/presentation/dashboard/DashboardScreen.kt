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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import com.sherif.ledger.presentation.dashboard.components.RecentTransactionsSection
import com.sherif.ledger.presentation.dashboard.preview.DashboardPreviewData

private object HeroTransitions {
    const val TopBarExit = 0.15f
    const val TopBarTranslationPx = 30f
    const val BalanceLabelExit = 0.25f
    const val AmountExit = 0.45f
    const val CurrencyExit = 0.50f
    const val ExpandedExit = 0.55f
    const val CompactEnter = 0.60f
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
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val expandedHeight = LedgerHeroDefaults.ExpandedHeight + statusBarPadding
    val collapsedHeight = LedgerHeroDefaults.CollapsedHeight + statusBarPadding

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
        // Global Atmosphere (Subtle wash) - Ensures bleed behind status bar
        LedgerAtmosphereGlow(Modifier.fillMaxSize())

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = LedgerSpacing.Group,
                end = LedgerSpacing.Group,
                bottom = LedgerSpacing.ScreenBottom + 100.dp, // Clear the floating dock
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
            expandedHeight = expandedHeight,
            collapsedHeight = collapsedHeight,
            background = SolidColor(Color.Transparent),
            contentBackground = {
                LedgerAtmosphereGlow(Modifier.fillMaxSize())
            },
            expandedContent = { progress -> ExpandedHero(progress, state) },
            compactContent = { progress -> CompactHero(progress, state) },
        )
    }
}

@Composable
private fun ExpandedHero(progress: Float, state: DashboardUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.Group)
            .padding(bottom = LedgerSpacing.XxLarge)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
    ) {
        // 1. The Financial Instrument Monolith (Centered in content area)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                text = "MONTHLY SPENDING",
                style = LedgerTextStyles.Caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                ),
                color = Color.White.copy(alpha = 0.35f),
            )
            
            LedgerAmount(
                amount = state.totalSpent.replace(state.balanceCurrency, "").trim(),
                style = LedgerAmountStyle.Display,
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    val scale = 1f - (progress * 0.10f)
                    scaleX = scale
                    scaleY = scale
                },
            )
            
            Text(
                text = state.balanceCurrency,
                style = LedgerTextStyles.Caption.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.45f),
            )
            
            Spacer(Modifier.height(LedgerSpacing.Small))
            
            // Interaction Pill
            Box(
                modifier = Modifier
                    .ledgerSurface(
                        backgroundColor = Color.White.copy(alpha = 0.05f),
                        borderColor = Color.White.copy(alpha = 0.10f),
                        shape = LedgerRadius.Full,
                    )
                    .ledgerClickable { /* TODO */ }
                    .padding(horizontal = LedgerSpacing.Medium, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.currentMonth.uppercase(),
                        style = LedgerTextStyles.Caption.copy(
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.width(LedgerSpacing.XxSmall))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }

        // 2. Summary Metrics (Anchored to bottom of hero area)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MetricItem(
                label = "INCOME",
                value = state.income,
                change = state.incomeChange,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                color = LedgerTheme.colors.income,
            )
            MetricItem(
                label = "EXPENSES",
                value = state.expense,
                change = state.expenseChange,
                icon = Icons.Filled.KeyboardArrowDown,
                color = LedgerTheme.colors.expense,
            )
            MetricItem(
                label = "SAVINGS",
                value = state.savings,
                change = state.savingsChange,
                icon = Icons.Filled.Star,
                color = Color.White.copy(alpha = 0.70f),
            )
        }
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(LedgerTheme.iconSize.Medium)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = label,
            style = LedgerTextStyles.Caption.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                lineHeight = 12.sp,
            ),
            color = Color.White.copy(alpha = 0.60f),
        )
        LedgerAmount(
            amount = value,
            style = LedgerAmountStyle.Small,
            color = color,
            textAlign = TextAlign.Center,
        )
        Text(
            text = change,
            style = LedgerTextStyles.Caption.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp,
            ),
            color = color.copy(alpha = 0.90f),
        )
        Text(
            text = "vs last month",
            style = LedgerTextStyles.Caption.copy(fontSize = 8.sp, lineHeight = 10.sp),
            color = Color.White.copy(alpha = 0.30f),
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
        
        Text(
            text = state.currentMonth,
            style = LedgerTextStyles.Mono.copy(fontSize = 11.sp),
            color = LedgerTheme.colors.tertiaryLabel,
        )
    }
}

#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying LDL Design Direction v2..."

mkdir -p "$B/presentation/dashboard"
cat > "$B/presentation/dashboard/DashboardUiState.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard

data class DashboardUiState(
    val greeting: String,
    val userName: String,
    val currentMonth: String,
    val totalSpent: String,
    val balanceAmount: String = "",
    val balanceCurrency: String = "AED",
    val budgetProgress: Float,
    val expense: String,
    val income: String,
    val savings: String,
    val recentTransactions: List<TransactionUiModel>,
    val insights: List<InsightUiModel>,
)

data class TransactionUiModel(
    val merchant: String,
    val category: String,
    val amount: String,
    val isExpense: Boolean = true,
    val date: String = "Today",
)

data class InsightUiModel(
    val title: String,
    val subtitle: String,
    val indicator: String = "",
)
EOF

mkdir -p "$B/presentation/dashboard/preview"
cat > "$B/presentation/dashboard/preview/DashboardPreviewData.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.preview

import com.sherif.ledger.presentation.dashboard.DashboardUiState
import com.sherif.ledger.presentation.dashboard.InsightUiModel
import com.sherif.ledger.presentation.dashboard.TransactionUiModel

object DashboardPreviewData {

    val state = DashboardUiState(
        greeting = "Good morning",
        userName = "Sherif",
        currentMonth = "June",
        totalSpent = "AED 2,840.25",
        balanceAmount = "2,840.25",
        balanceCurrency = "AED",
        budgetProgress = 0.62f,
        expense = "AED 1,950",
        income = "AED 5,200",
        savings = "AED 3,250",
        recentTransactions = listOf(
            TransactionUiModel("Amazon", "Shopping", "AED 52", isExpense = true, date = "Today"),
            TransactionUiModel("Carrefour", "Groceries", "AED 126", isExpense = true, date = "Today"),
            TransactionUiModel("Costa Coffee", "Coffee", "AED 19", isExpense = true, date = "Yesterday"),
            TransactionUiModel("Salary", "Income", "AED 5,200", isExpense = false, date = "May 31"),
        ),
        insights = listOf(
            InsightUiModel("Food spending", "12% lower than last week", "\u2193 12%"),
            InsightUiModel("Subscriptions", "Netflix renews tomorrow", ""),
        ),
    )
}
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
    const val GreetingExit = 0.3f
    const val GreetingTranslationPx = 80f
    const val BalanceLabelExit = 0.4f
    const val CurrencyExit = 0.45f
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

        // Atmospheric glow
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0D4F4F).copy(alpha = 0.4f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.06f),
                    radius = size.width * 1.1f,
                ),
            )
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = LedgerSpacing.Large,
                end = LedgerSpacing.Large,
                bottom = LedgerSpacing.XxLarge,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge),
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
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 32.dp)
            .graphicsLayer {
                alpha = (1f - progress / HeroTransitions.ExpandedExit).coerceIn(0f, 1f)
            },
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.GreetingExit).coerceIn(0f, 1f)
                    translationY = -(progress * HeroTransitions.GreetingTranslationPx)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${state.greeting},",
                    style = LedgerTextStyles.Caption,
                    color = Color.White.copy(alpha = 0.55f),
                )
                Text(
                    state.userName,
                    style = LedgerTextStyles.Title,
                    color = Color.White,
                )
            }
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    state.userName.take(1),
                    style = LedgerTextStyles.Label,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Balance hero
        Column {
            Text(
                "Total balance",
                style = LedgerTextStyles.Caption,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.BalanceLabelExit).coerceIn(0f, 1f)
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                state.balanceAmount,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    lineHeight = 56.sp,
                    letterSpacing = (-1.5).sp,
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    alpha = (1f - progress / HeroTransitions.CurrencyExit).coerceIn(0f, 1f)
                },
            ) {
                Text(
                    state.balanceCurrency,
                    style = LedgerTextStyles.Body,
                    color = Color.White.copy(alpha = 0.45f),
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
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
        Text(
            state.currentMonth,
            style = LedgerTextStyles.Caption,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}
EOF

mkdir -p "$B/presentation/dashboard/components"
cat > "$B/presentation/dashboard/components/QuickStatsSection.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatMetric("\u2193", "Income", state.income, LedgerTheme.colors.income)
        StatMetric("\u2191", "Expense", state.expense, LedgerTheme.colors.expense)
        StatMetric("\u25CF", "Savings", state.savings, LedgerTheme.colors.tint)
    }
}

@Composable
private fun StatMetric(symbol: String, label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, style = LedgerTextStyles.Label, color = color)
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Spacer(Modifier.height(LedgerSpacing.XxSmall))
        Text(value, style = LedgerTextStyles.Section, color = color)
    }
}
EOF

cat > "$B/presentation/dashboard/components/RecentTransactionsSection.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

private val CategoryColors = mapOf(
    "shopping" to Color(0xFFA855F7),
    "groceries" to Color(0xFF22C55E),
    "coffee" to Color(0xFF92400E),
    "food" to Color(0xFFF97316),
    "transport" to Color(0xFF3B82F6),
    "fuel" to Color(0xFFEF4444),
    "entertainment" to Color(0xFFEC4899),
    "income" to Color(0xFF047857),
)

@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent activity", style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)
            Text("See all", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tint)
        }

        LedgerSurface(
            level = LedgerSurfaceLevel.Level1,
            contentPadding = PaddingValues(0.dp),
        ) {
            state.recentTransactions.forEachIndexed { index, txn ->
                TransactionRow(txn)
                if (index != state.recentTransactions.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = 60.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(txn: com.sherif.ledger.presentation.dashboard.TransactionUiModel) {
    val catColor = CategoryColors[txn.category.lowercase()] ?: LedgerTheme.colors.tint
    val amountColor = if (txn.isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income
    val sign = if (txn.isExpense) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                txn.merchant.take(1).uppercase(),
                style = LedgerTextStyles.Label,
                color = catColor,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(txn.merchant, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
            Text(
                "${txn.category} \u00B7 ${txn.date}",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$sign${txn.amount}", style = LedgerTextStyles.Label, color = amountColor)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LedgerTheme.colors.tertiaryLabel,
            modifier = Modifier.size(18.dp),
        )
    }
}
EOF

cat > "$B/presentation/dashboard/components/InsightSection.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    if (insight.indicator.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            insight.indicator,
                            style = LedgerTextStyles.Label,
                            color = LedgerTheme.colors.income,
                        )
                    } else {
                        Icon(
                            Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = LedgerTheme.colors.tertiaryLabel,
                            modifier = Modifier.padding(start = 8.dp),
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
EOF

echo "Done. 6 files written."
echo "Run: git add -A && git commit -m 'feat(home): LDL v2 dark atmospheric composition'"
echo "Then: ./gradlew assembleDebug"

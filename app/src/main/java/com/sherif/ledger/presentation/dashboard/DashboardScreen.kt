package com.sherif.ledger.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.settings.presentation.viewmodel.UserProfileViewModel
import java.util.Calendar

/**
 * Dashboard — the flagship surface, wired to live data.
 *
 * Everything shown here is backed by the real analytics/transaction pipeline
 * ([DashboardViewModel]): the balance and its month-over-month change, this
 * month's spend, backend-derived intelligence, and recent captured activity.
 * Sections whose engines don't exist yet (Safe-to-Spend forecast, Story
 * generation, recurring/upcoming detection) are intentionally absent rather than
 * faked — when the device has no captured activity yet, an honest empty state
 * explains what will appear. No value on this screen is fabricated.
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMerchantClick: (String) -> Unit = {},
) {
    val hasActivity = state.recentActivity.isNotEmpty()
    val hasInsights = state.intelligenceSummary.isNotEmpty()

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding,
                end = LedgerSpacing.ScreenPadding,
                bottom = LedgerSpacing.ScreenBottom + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item { DashboardReveal(index = 0) { GreetingHeader() } }

            item {
                DashboardReveal(index = 1) {
                    BalanceHero(
                        balance = state.totalBalance,
                        isNegative = state.isNegativeBalance,
                        change = state.balanceChangePercentage,
                        monthlySpend = state.monthlyExpenses,
                    )
                }
            }

            if (hasInsights) {
                item { DashboardReveal(index = 2) { InsightsSection(state.intelligenceSummary, onNavigateToInsights) } }
            }

            if (hasActivity) {
                item {
                    DashboardReveal(index = 3) {
                        SectionLabel("RECENT ACTIVITY", trailing = "See all", onTrailing = onNavigateToTransactions)
                    }
                }
                state.recentActivity.forEach { group ->
                    item {
                        Text(
                            group.title,
                            style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                            color = LedgerTheme.colors.textTertiary,
                            modifier = Modifier.padding(top = LedgerSpacing.Tiny),
                        )
                    }
                    items(group.items, key = { it.id }) { item ->
                        LedgerTransactionRow(
                            title = item.merchantName,
                            amount = item.amount,
                            explanation = item.explanation.ifEmpty { item.category },
                            isExpense = item.isExpense,
                            time = item.time,
                            onClick = { onMerchantClick(item.merchantName) },
                        )
                        LedgerDivider(alpha = 0.05f)
                    }
                }
            }

            if (!hasActivity && !hasInsights) {
                item {
                    DashboardReveal(index = 2) {
                        LedgerEmptyState(
                            title = "Watching for your activity",
                            subtitle = "Ledger reads your bank SMS and notifications and builds your " +
                                "dashboard automatically. As transactions arrive, your balance, " +
                                "insights and recent activity appear here — nothing to enter by hand.",
                            icon = Icons.Outlined.AutoAwesome,
                            modifier = Modifier.padding(top = LedgerSpacing.XLarge),
                        )
                    }
                }
            }
        }
    }
}

/**
 * First-paint reveal for a top-level dashboard section: a calm fade + slight
 * upward settle, staggered by [index] so the surface assembles top-to-bottom
 * rather than snapping in all at once. Plays once on entry (never on scroll
 * churn — the flag is remembered), and uses the canonical [LedgerAnimations]
 * vocabulary so motion stays tunable in one place. Deliberately restrained:
 * no bounce, no overshoot — professional, not flashy.
 */
@Composable
private fun DashboardReveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = LedgerAnimations.listEnter(delayMs = LedgerAnimations.staggerDelay(index)),
    ) {
        content()
    }
}

@Composable
private fun GreetingHeader(viewModel: UserProfileViewModel = hiltViewModel()) {
    val profile by viewModel.uiState.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(LedgerTheme.colors.surfaceInset),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.initials, style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(LedgerSpacing.Small))
        Column(Modifier.weight(1f)) {
            Text(
                "${greeting()}, ${profile.name.substringBefore(' ').ifBlank { "there" }}",
                style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.textPrimary,
            )
            Text("Here's your financial overview", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary)
        }
        // (No notification bell — there is no notifications surface yet; a dead
        // affordance is worse than none. Reinstated when a real inbox exists.)
    }
}

@Composable
private fun BalanceHero(balance: String, isNegative: Boolean, change: String?, monthlySpend: String) {
    var hidden by remember { mutableStateOf(false) }
    LedgerCard(elevation = LedgerCardDefaults.Elevation) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Total Balance",
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (hidden) "Show amount" else "Hide amount",
                tint = LedgerTheme.colors.textTertiary,
                modifier = Modifier.size(20.dp).clip(CircleShape).ledgerClickable { hidden = !hidden },
            )
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerAutoSizeText(
            text = if (hidden) "••••••" else (if (isNegative) "-$balance" else balance),
            style = LedgerTextStyles.Hero,
            color = if (isNegative) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (change != null || monthlySpend.isNotBlank()) {
            Spacer(Modifier.height(LedgerSpacing.Small))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (change != null) {
                    val down = change.startsWith("-")
                    val tint = if (down) LedgerTheme.colors.negative else LedgerTheme.colors.positive
                    Text(
                        text = "$change vs last month",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.SemiBold),
                        color = tint,
                        modifier = Modifier.clip(LedgerRadius.Full).background(tint.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.width(LedgerSpacing.Small))
                }
                Text("Spent $monthlySpend this month", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
            }
        }
    }
}

@Composable
private fun InsightsSection(items: List<String>, onOpen: () -> Unit) {
    Column {
        SectionLabel("INSIGHTS", trailing = "See all", onTrailing = onOpen)
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
            items.forEachIndexed { i, fact ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(LedgerSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LedgerTheme.colors.system))
                    Spacer(Modifier.width(LedgerSpacing.Medium))
                    Text(fact, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
                }
                if (i < items.lastIndex) LedgerDivider(alpha = 0.05f)
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, trailing: String? = null, onTrailing: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = LedgerSpacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = LedgerTheme.colors.textTertiary,
        )
        if (trailing != null) {
            Text(
                trailing,
                style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.system,
                modifier = if (onTrailing != null) Modifier.ledgerClickable(onClick = onTrailing) else Modifier,
            )
        }
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good Morning"
        h < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

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
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith

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
    val hasUpcoming = state.upcoming.isNotEmpty()

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding,
                end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            // Small, not Large: this arrangement applies between EVERY item,
            // including each individual transaction row, so a section-sized gap
            // here doubled the space around rows that already carry their own
            // padding. Section headings add their own breathing room instead.
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
        ) {
            item { DashboardReveal(index = 0) { GreetingHeader() } }

            item {
                DashboardReveal(index = 1) {
                    BalanceHero(
                        balance = state.totalBalance,
                        isNegative = state.isNegativeBalance,
                        change = state.balanceChangePercentage,
                        monthlySpend = state.monthlyExpenses,
                        unattributedCount = state.unattributedCount,
                    )
                }
            }

            if (hasUpcoming) {
                item {
                    DashboardReveal(index = 2) {
                        UpcomingSection(state.upcoming)
                    }
                }
            }

            if (hasInsights) {
                item { DashboardReveal(index = 3) { InsightsSection(state.intelligenceSummary, onNavigateToInsights) } }
            }

            if (hasActivity) {
                item {
                    DashboardReveal(index = 4) {
                        SectionLabel("RECENT ACTIVITY", trailing = "See all", onTrailing = onNavigateToTransactions)
                    }
                }
                state.recentActivity.forEach { group ->
                    item {
                        Text(
                            group.title,
                            style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                            color = LedgerTheme.colors.textTertiary,
                            modifier = Modifier.padding(top = LedgerSpacing.Small),
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
        LedgerAvatar(initials = profile.initials, size = 44.dp)
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
private fun BalanceHero(balance: String, isNegative: Boolean, change: String?, monthlySpend: String, unattributedCount: Int = 0) {
    var hidden by remember { mutableStateOf(false) }
    LedgerHeroCard {
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
        // Hiding and revealing the balance is a deliberate, privacy-motivated act —
        // often performed with someone next to you — so it should read as the figure
        // being covered and uncovered, not as one string being swapped for another
        // between frames. The reveal lifts in; the mask drops down over it.
        AnimatedContent(
            targetState = hidden,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(LedgerAnimations.itemPlacement()) { -it / 4 } + fadeIn(LedgerAnimations.itemAppear()))
                        .togetherWith(fadeOut(LedgerAnimations.itemDisappear()))
                } else {
                    (slideInVertically(LedgerAnimations.itemPlacement()) { it / 4 } + fadeIn(LedgerAnimations.itemAppear()))
                        .togetherWith(fadeOut(LedgerAnimations.itemDisappear()))
                }
            },
            label = "balanceReveal",
        ) { isHidden ->
            LedgerAutoSizeText(
                text = if (isHidden) "••••••" else (if (isNegative) "−$balance" else balance),
                style = LedgerTextStyles.Hero,
                color = if (isNegative) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (change != null || monthlySpend.isNotBlank()) {
            Spacer(Modifier.height(LedgerSpacing.Small))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (change != null) {
                    val down = change.startsWith("-")
                    val tint = if (down) LedgerTheme.colors.negative else LedgerTheme.colors.positive
                    Text(
                        // "spend" named explicitly — this badge sits directly under the
                        // BALANCE figure, and without the word it reads as describing
                        // the balance itself, not this month's spend.
                        text = "$change spend vs last month",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.SemiBold),
                        color = tint,
                        modifier = Modifier.clip(LedgerRadius.Full).background(tint.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    Spacer(Modifier.width(LedgerSpacing.Small))
                }
                // One line. "this month" is already implied by the section it sits in, and
                // spelling it out wrapped the line on a 360dp canvas.
                Text(
                    "Spent $monthlySpend",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.textTertiary,
                    maxLines = 1,
                )
            }
        }
        if (unattributedCount > 0) {
            Spacer(Modifier.height(LedgerSpacing.Tiny))
            // Design review finding F1 (2026-08-06): a capture that can't be linked
            // to a real account is invisible to the balance above but still shows up
            // in Recent Activity below — without this line the two numbers just
            // silently disagree, which reads as broken rather than as a real, honest
            // state. Never a tap target to a screen that doesn't exist yet (RC7
            // Candidate Accounts are Developer-Console-only today) — just the truth.
            Text(
                if (unattributedCount == 1) {
                    "1 transaction below isn't linked to an account yet, so it isn't counted here"
                } else {
                    "$unattributedCount transactions below aren't linked to an account yet, so they aren't counted here"
                },
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textTertiary,
            )
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

/**
 * What Ledger expects to be charged next.
 *
 * Every row here is a projection, so each one carries the cadence it was derived
 * from ("Monthly") and the engine's own confidence rather than presenting a
 * forecast as a fact. The section simply does not appear until there is enough
 * history for the engine to be sure — an empty "Upcoming" heading would imply
 * Ledger had looked and found nothing, when the truth is it cannot know yet.
 */
@Composable
private fun UpcomingSection(items: List<UpcomingUiModel>) {
    val colors = LedgerTheme.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel("UPCOMING")
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerCard {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = LedgerSpacing.Small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerBrandIcon(name = item.label, size = 36.dp)
                    Spacer(Modifier.width(LedgerSpacing.Medium))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Text(
                            "${item.cadence} · ${item.dueLabel}",
                            style = LedgerTextStyles.Label,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(LedgerSpacing.Small))
                    LedgerAmount(
                        amount = item.amount,
                        style = LedgerAmountStyle.Regular,
                        color = colors.textPrimary,
                    )
                }
                if (index != items.lastIndex) LedgerDivider(alpha = 0.05f)
            }
        }
    }
}

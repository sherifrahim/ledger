package com.sherif.ledger.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.settings.presentation.viewmodel.UserProfileViewModel
import java.util.Calendar

/**
 * Dashboard — the flagship surface (Milestone 1.5).
 *
 * Layout follows the canonical Dashboard order (spec D4): a greeting, then the
 * Safe-to-Spend hero, the Financial Story, an Upcoming timeline, Insights, the
 * Review queue, Accounts and Recent activity. This milestone establishes the
 * *visual language*; the sections below the balance are not yet wired to their
 * engines (Story generation, forecast, recommendation are explicitly out of
 * scope), so where a section has no backend it renders realistic placeholder
 * data — DEBUG-only, so a release build never fabricates financial figures.
 */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onMerchantClick: (String) -> Unit = {},
) {
    // Design-sprint showcase data. Gated to debug so production stays honest and
    // shows real (currently empty) state instead of sample figures.
    val showcase = com.sherif.ledger.BuildConfig.DEBUG

    Scaffold(containerColor = LedgerTheme.colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding,
                end = LedgerSpacing.ScreenPadding,
                top = 0.dp,
                bottom = LedgerSpacing.ScreenBottom + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item { GreetingHeader() }

            if (showcase) {
                item { SafeToSpendCard(DashboardShowcase.safeToSpend, DashboardShowcase.safeToSpendPeriod, DashboardShowcase.safeToSpendProgress) }
                item { FinancialStoryCard(DashboardShowcase.story) }
                item { UpcomingSection(DashboardShowcase.upcoming) }
                item { InsightsSection(DashboardShowcase.insights, onNavigateToInsights) }
                item { ReviewQueueCard(DashboardShowcase.reviewCount) }
                item { AccountsStrip(DashboardShowcase.accounts) }
                item { SectionLabel("RECENT ACTIVITY", trailing = "See all", onTrailing = onNavigateToTransactions) }
                items(DashboardShowcase.recent.size) { i ->
                    val t = DashboardShowcase.recent[i]
                    LedgerTransactionRow(
                        title = t.merchant,
                        amount = t.amount,
                        explanation = t.category,
                        isExpense = t.isExpense,
                        time = t.time,
                        onClick = { onMerchantClick(t.merchant) },
                    )
                    if (i < DashboardShowcase.recent.lastIndex) LedgerDivider(alpha = 0.05f)
                }
            } else {
                // Honest production path until the flagship sections are wired (M2+).
                item { SafeToSpendCard(state.totalBalance, "Financial state", 0f, mask = false) }
                item {
                    LedgerEmptyState(
                        title = "Your dashboard is warming up",
                        subtitle = "As Ledger captures your bank messages, your Safe to Spend, story and timeline appear here automatically.",
                        icon = Icons.Filled.Notifications,
                    )
                }
            }
        }
    }
}

@Composable
private fun GreetingHeader(viewModel: UserProfileViewModel = hiltViewModel()) {
    val profile by viewModel.uiState.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LedgerTheme.colors.surfaceInset),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.initials, style = LedgerTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(LedgerSpacing.Small))
        Column(Modifier.weight(1f)) {
            Text(
                text = "${greeting()}, ${profile.name.substringBefore(' ').ifBlank { "there" }}",
                style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.textPrimary,
            )
            Text(
                text = "Here's your financial overview",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textSecondary,
            )
        }
        LedgerIconButton(icon = Icons.Default.Notifications, onClick = { }, tint = LedgerTheme.colors.textPrimary)
    }
}

@Composable
private fun SafeToSpendCard(amount: String, period: String, progress: Float, mask: Boolean = true) {
    var hidden by remember { mutableStateOf(false) }
    LedgerCard(elevation = LedgerCardDefaults.Elevation) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Safe to Spend",
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                color = LedgerTheme.colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            if (mask) {
                Icon(
                    imageVector = if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (hidden) "Show amount" else "Hide amount",
                    tint = LedgerTheme.colors.textTertiary,
                    modifier = Modifier.size(20.dp).clip(CircleShape).ledgerClickable { hidden = !hidden },
                )
            }
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = if (hidden) "••••••" else amount,
            style = LedgerTextStyles.Hero,
            color = LedgerTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        Text(period, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
        if (progress > 0f) {
            Spacer(Modifier.height(LedgerSpacing.Medium))
            ProgressTrack(progress)
        }
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(LedgerRadius.Full)
            .background(LedgerTheme.colors.accent.copy(alpha = 0.15f)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(LedgerRadius.Full)
                .background(LedgerTheme.colors.accent),
        )
    }
}

@Composable
private fun FinancialStoryCard(text: String) {
    LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
        Text(
            text = "Financial Story",
            style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
            color = LedgerTheme.colors.textSecondary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = text,
            style = LedgerTextStyles.BodyMedium.copy(lineHeight = 24.sp),
            color = LedgerTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Read Story", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.system)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = LedgerTheme.colors.system, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun UpcomingSection(items: List<ShowcaseUpcoming>) {
    Column {
        SectionLabel("UPCOMING", trailing = "View all", onTrailing = {})
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
            items.forEachIndexed { i, it ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(LedgerSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerBrandIcon(name = it.name, size = 36.dp)
                    Spacer(Modifier.width(LedgerSpacing.Medium))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LedgerTheme.colors.textPrimary)
                        Text(it.due, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                    }
                    Text(it.amount, style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LedgerTheme.colors.textPrimary)
                }
                if (i < items.lastIndex) LedgerDivider(alpha = 0.05f)
            }
        }
    }
}

@Composable
private fun InsightsSection(items: List<ShowcaseInsight>, onOpen: () -> Unit) {
    Column {
        SectionLabel("INSIGHTS", trailing = "See all", onTrailing = onOpen)
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
            items.forEachIndexed { i, it ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(LedgerSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (it.positive) LedgerTheme.colors.positive else LedgerTheme.colors.attention))
                    Spacer(Modifier.width(LedgerSpacing.Medium))
                    Text(it.title, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                    Text(
                        it.value,
                        style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                        color = if (it.positive) LedgerTheme.colors.positive else LedgerTheme.colors.textSecondary,
                    )
                }
                if (i < items.lastIndex) LedgerDivider(alpha = 0.05f)
            }
        }
    }
}

@Composable
private fun ReviewQueueCard(count: Int) {
    LedgerCard(elevation = LedgerCardDefaults.ElevationLow, onClick = { }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Review Queue", style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
                Text("$count items need your attention", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary)
            }
            Box(
                modifier = Modifier.clip(CircleShape).background(LedgerTheme.colors.attention.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("$count", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.attention)
            }
        }
    }
}

@Composable
private fun AccountsStrip(items: List<ShowcaseAccount>) {
    Column {
        SectionLabel("ACCOUNTS", trailing = "Manage", onTrailing = {})
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
            items.forEachIndexed { i, it ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(LedgerSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LedgerBrandIcon(name = it.name, size = 36.dp)
                    Spacer(Modifier.width(LedgerSpacing.Medium))
                    Column(Modifier.weight(1f)) {
                        Text(it.name, style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold), color = LedgerTheme.colors.textPrimary)
                        Text(it.type, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                    }
                    Text(
                        it.balance,
                        style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (it.balance.startsWith("-")) LedgerTheme.colors.negative else LedgerTheme.colors.textPrimary,
                    )
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
            text = title,
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = LedgerTheme.colors.textTertiary,
        )
        if (trailing != null) {
            Text(
                text = trailing,
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

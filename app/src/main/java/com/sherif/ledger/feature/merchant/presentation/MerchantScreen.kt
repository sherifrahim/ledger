package com.sherif.ledger.feature.merchant.presentation

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerCardDefaults
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Merchant relationship page (P2), wired to live data.
 *
 * Ledger treats a merchant as a relationship: how long you've transacted, how
 * much, how often, and what it means — all computed from the user's real
 * transactions ([com.sherif.ledger.feature.merchant.presentation.viewmodel.MerchantViewModel]).
 * No fabricated ratings or figures.
 */
@Composable
fun MerchantScreen(
    state: MerchantUiState,
    onBackClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceBase),
        contentPadding = PaddingValues(
            start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
            bottom = LedgerSpacing.ScreenBottom + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(vertical = LedgerSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LedgerIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
                Text(
                    state.name,
                    style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold),
                    color = LedgerTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f).padding(start = LedgerSpacing.Small),
                )
                Spacer(Modifier.width(44.dp))
            }
        }

        if (state.txCount == 0) {
            item {
                LedgerEmptyState(
                    title = state.name,
                    subtitle = if (state.loaded)
                        "No transactions with this merchant yet. Once Ledger captures one, its " +
                            "relationship — spend, frequency and categories — appears here."
                    else "Loading…",
                    modifier = Modifier.padding(top = LedgerSpacing.XLarge),
                )
            }
            return@LazyColumn
        }

        item { RelationshipHeader(state) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                StatCard("Total Spent", "${state.currency} ${state.totalSpent}", Modifier.weight(1f))
                StatCard("Transactions", state.txCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                StatCard("Avg. Monthly", "${state.currency} ${state.avgMonthly}", Modifier.weight(1f))
                StatCard("Largest", "${state.currency} ${state.largest}", Modifier.weight(1f))
            }
        }

        if (state.insights.isNotEmpty()) {
            item {
                Section("Insights") {
                    LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large) {
                        state.insights.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(LedgerTheme.colors.positive))
                                Spacer(Modifier.width(LedgerSpacing.Small))
                                Text(s, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }

        if (state.categories.isNotEmpty()) {
            item {
                Section("Top categories") {
                    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                        state.categories.forEach { CategoryBar(it.label, it.fraction) }
                    }
                }
            }
        }

        if (state.related.isNotEmpty()) {
            item {
                Section("Related merchants") {
                    Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Large)) {
                        state.related.forEach { name ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LedgerBrandIcon(name = name, size = 52.dp)
                                Spacer(Modifier.height(LedgerSpacing.Tiny))
                                Text(name, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipHeader(state: MerchantUiState) {
    LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LedgerBrandIcon(name = state.name, size = 64.dp)
            Spacer(Modifier.width(LedgerSpacing.Medium))
            Column {
                Text("Relationship", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                Text(state.name, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
                if (state.since.isNotBlank()) {
                    Text(state.since, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    LedgerSurface(modifier = modifier, level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        Text(value, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
    }
}

@Composable
private fun CategoryBar(label: String, fraction: Float) {
    val anim by animateFloatAsState(targetValue = fraction, label = "cat")
    Column {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
            Text("${(fraction * 100).toInt()}%", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(LedgerRadius.Full).background(LedgerTheme.colors.surfaceInset)) {
            Box(Modifier.fillMaxWidth(anim).height(8.dp).clip(LedgerRadius.Full).background(LedgerTheme.colors.accent))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = LedgerTheme.colors.textTertiary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        content()
    }
}

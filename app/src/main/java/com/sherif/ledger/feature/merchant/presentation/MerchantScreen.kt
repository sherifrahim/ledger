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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
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
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Merchant relationship page (Milestone 1.5).
 *
 * Ledger treats a merchant as a *relationship*, not a string: how long you've
 * transacted, how much, how often, and what it means. This milestone designs the
 * page; the figures are placeholder until the Merchant Intelligence / Relationship
 * engines feed it in a later milestone. [merchantName] lets the caller title it.
 */
@Composable
fun MerchantScreen(
    merchantName: String = "Amazon.ae",
    onBackClick: () -> Unit = {},
) {
    val d = MerchantShowcase
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
                Text(merchantName, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(44.dp))
            }
        }

        item { RelationshipHeader(merchantName, d.rating, d.since) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                StatCard("Total Spent", d.totalSpent, Modifier.weight(1f))
                StatCard("Transactions", d.txCount, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                StatCard("Avg. Monthly", d.avgMonthly, Modifier.weight(1f))
                StatCard("Largest Purchase", d.largest, Modifier.weight(1f))
            }
        }

        item {
            Section("Insights") {
                LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large) {
                    d.insights.forEachIndexed { i, s ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                            Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(LedgerTheme.colors.positive))
                            Spacer(Modifier.width(LedgerSpacing.Small))
                            Text(s, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary)
                        }
                    }
                }
            }
        }

        item {
            Section("Top categories") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
                    d.categories.forEach { CategoryBar(it.first, it.second) }
                }
            }
        }

        item {
            Section("Related merchants") {
                Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Large)) {
                    d.related.forEach { name ->
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

@Composable
private fun RelationshipHeader(name: String, rating: Float, since: String) {
    LedgerCard(elevation = LedgerCardDefaults.ElevationLow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LedgerBrandIcon(name = name, size = 64.dp)
            Spacer(Modifier.width(LedgerSpacing.Medium))
            Column {
                Text("Relationship", style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textSecondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..5).forEach { i ->
                        val icon = when {
                            rating >= i -> Icons.Filled.Star
                            rating >= i - 0.5f -> Icons.Filled.StarHalf
                            else -> Icons.Outlined.StarOutline
                        }
                        Icon(icon, null, tint = LedgerTheme.colors.attention, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(since, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
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

private object MerchantShowcase {
    const val rating = 4.5f
    const val since = "Since 2021"
    const val totalSpent = "AED 14,280"
    const val txCount = "137"
    const val avgMonthly = "AED 580"
    const val largest = "AED 2,100"
    val insights = listOf(
        "Most purchases are electronics",
        "Spending increases every November",
        "Prime membership detected",
    )
    val categories = listOf("Shopping" to 0.68f, "Electronics" to 0.24f, "Books" to 0.08f)
    val related = listOf("Amazon Prime", "Noon", "Apple")
}

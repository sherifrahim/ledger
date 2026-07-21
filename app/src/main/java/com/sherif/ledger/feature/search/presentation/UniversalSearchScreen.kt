package com.sherif.ledger.feature.search.presentation

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Universal Search (Milestone 1.5).
 *
 * A calm, Spotlight-like entry point that searches *concepts* — merchants,
 * transactions, accounts, categories, story, forecast — not screens. This
 * milestone builds the resting experience (field, recent searches, quick access,
 * suggestions); the actual query engine is a later milestone, so the content
 * here is static scaffolding rather than live results.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UniversalSearchScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().background(LedgerTheme.colors.surfaceBase),
        contentPadding = PaddingValues(
            start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
            top = LedgerSpacing.Small, bottom = LedgerSpacing.ScreenBottom + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = LedgerSpacing.Small)) {
                Text("Search", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
                Spacer(Modifier.height(LedgerSpacing.Medium))
                SearchField()
            }
        }

        item {
            Section("Recent") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                    listOf("electricity bill", "amazon", "salary", "netflix").forEach { Chip(it) }
                }
            }
        }

        item {
            Section("Quick access") {
                LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
                    QuickAccess.entries.forEachIndexed { i, q ->
                        QuickRow(q.icon, q.label)
                        if (i < QuickAccess.entries.lastIndex) LedgerDivider(alpha = 0.05f)
                    }
                }
            }
        }

        item {
            Section("Try searching") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                    listOf("coffee last week", "subscriptions this month", "transport in June", "dewa bill").forEach { Suggestion(it) }
                }
            }
        }
    }
}

@Composable
private fun SearchField() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LedgerRadius.Full)
            .background(LedgerTheme.colors.surfaceInset)
            .padding(horizontal = LedgerSpacing.Medium, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = LedgerTheme.colors.textTertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text("Search anything…", style = LedgerTextStyles.BodyLarge, color = LedgerTheme.colors.textTertiary, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.Mic, null, tint = LedgerTheme.colors.textTertiary, modifier = Modifier.size(20.dp))
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

@Composable
private fun Chip(label: String) {
    Text(
        label,
        style = LedgerTextStyles.Label,
        color = LedgerTheme.colors.textSecondary,
        modifier = Modifier
            .clip(LedgerRadius.Full)
            .background(LedgerTheme.colors.surfaceInset)
            .ledgerClickable {}
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Tiny),
    )
}

@Composable
private fun QuickRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().ledgerClickable {}.padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(LedgerRadius.Small).background(LedgerTheme.colors.system.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = LedgerTheme.colors.system, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(LedgerSpacing.Medium))
        Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Search, null, tint = LedgerTheme.colors.textTertiary.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Suggestion(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LedgerRadius.Medium)
            .background(LedgerTheme.colors.surfaceInset.copy(alpha = 0.6f))
            .ledgerClickable {}
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = LedgerTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
    }
}

private enum class QuickAccess(val label: String, val icon: ImageVector) {
    Merchants("Merchants", Icons.Filled.Store),
    Transactions("Transactions", Icons.AutoMirrored.Filled.ReceiptLong),
    Accounts("Accounts", Icons.Filled.AccountBalanceWallet),
    Categories("Categories", Icons.Filled.Category),
    Story("Financial Story", Icons.AutoMirrored.Filled.MenuBook),
    Forecast("Forecast", Icons.Filled.ShowChart),
}

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerSearchBar
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.search.presentation.viewmodel.SearchViewModel
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import androidx.compose.material.icons.filled.Hub

/**
 * Universal Search (Product Hardening, PART 1) — real, not a resting mock.
 *
 * The field searches the user's actual captured transactions (merchant + amount)
 * via [SearchViewModel]; results are real rows. With an empty query it shows
 * quick access to real destinations only. There is no fabricated recent-search
 * list or suggestion data.
 */
@Composable
fun UniversalSearchScreen(
    onOpenTransactions: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenStory: () -> Unit = {},
    onOpenStoryGraph: () -> Unit = {},
    onResultClick: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val quickAccess = listOf(
        QuickAccess("Transactions", Icons.AutoMirrored.Filled.ReceiptLong, onOpenTransactions),
        QuickAccess("Accounts", Icons.Filled.AccountBalanceWallet, onOpenAccounts),
        QuickAccess("Insights", Icons.Filled.PieChart, onOpenInsights),
        QuickAccess("Financial Story", Icons.AutoMirrored.Filled.MenuBook, onOpenStory),
        QuickAccess("Story Graph", Icons.Filled.Hub, onOpenStoryGraph),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceBase),
        contentPadding = PaddingValues(
            start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
            top = LedgerSpacing.Small, bottom = ledgerScreenBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
    ) {
        item {
            Column(Modifier.statusBarsPadding().padding(top = LedgerSpacing.Small)) {
                Text("Search", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Box(
                    Modifier.clip(LedgerRadius.Full).background(LedgerTheme.colors.surfaceInset)
                        .padding(horizontal = LedgerSpacing.Medium, vertical = 14.dp),
                ) {
                    LedgerSearchBar(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        placeholder = "Search transactions and merchants",
                    )
                }
            }
        }

        when {
            state.query.isBlank() -> {
                item {
                    Section("Quick access") {
                        LedgerSurface(level = LedgerSurfaceLevel.Inset, shape = LedgerRadius.Large, contentPadding = PaddingValues(0.dp)) {
                            quickAccess.forEachIndexed { i, q ->
                                QuickRow(q.icon, q.label, q.onClick)
                                if (i < quickAccess.lastIndex) LedgerDivider(alpha = 0.05f)
                            }
                        }
                    }
                }
            }
            state.results.isEmpty() -> {
                item {
                    LedgerEmptyState(
                        title = "No matches",
                        subtitle = "Nothing found for “${state.query}”. Try a merchant name or an amount.",
                        icon = Icons.Outlined.SearchOff,
                        modifier = Modifier.padding(top = LedgerSpacing.Large),
                    )
                }
            }
            else -> {
                item {
                    Text(
                        "${state.results.size} result${if (state.results.size == 1) "" else "s"}",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = LedgerTheme.colors.textTertiary,
                    )
                }
                items(state.results, key = { it.id }) { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().ledgerClickable { onResultClick(r.id) }.padding(vertical = LedgerSpacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
                    ) {
                        LedgerBrandIcon(name = r.merchant, size = 40.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.merchant,
                                style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = LedgerTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            if (r.category.isNotBlank()) {
                                Text(r.category, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                        Spacer(Modifier.width(LedgerSpacing.Small))
                        Column(horizontalAlignment = Alignment.End) {
                            LedgerAmount(
                                amount = (if (r.isExpense) "-" else "+") + r.amount,
                                style = LedgerAmountStyle.Regular,
                                color = if (r.isExpense) LedgerTheme.colors.textPrimary else LedgerTheme.colors.positive,
                            )
                            Text(r.time, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)
                        }
                    }
                    LedgerDivider(alpha = 0.05f)
                }
            }
        }
    }
}

private data class QuickAccess(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun QuickRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().ledgerClickable(onClick = onClick).padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(LedgerRadius.Small).background(LedgerTheme.colors.system.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = LedgerTheme.colors.system, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(LedgerSpacing.Medium))
        Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
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

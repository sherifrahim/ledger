#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 3 — Review Inbox..."
mkdir -p "$B/feature/review/presentation/components"
mkdir -p "$B/feature/review/presentation/preview"

# ---- Extract MerchantHeader to shared LDL (2 consumers: Details + Review) ----
cat > "$B/core/designsystem/component/MerchantHeader.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Merchant identity display: centered avatar, name, and category.
 *
 * Consumers: Transaction Details, Review Inbox. Future: Merchant Profile.
 */
@Composable
fun MerchantHeader(
    name: String,
    category: String,
    accentHue: Long,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 56.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
    ) {
        LedgerAvatar(name = name, color = Color(accentHue), modifier = Modifier.size(avatarSize))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = LedgerTextStyles.Title, color = LedgerTheme.colors.label)
            Spacer(Modifier.height(LedgerSpacing.XxSmall))
            Text(category, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
    }
}
EOF

# ---- Update Transaction Details import ----
cat > "$B/feature/transactions/presentation/detail/components/MerchantHeader.kt" << 'EOF'
@file:Suppress("unused")
package com.sherif.ledger.feature.transactions.presentation.detail.components

// MerchantHeader has been promoted to com.sherif.ledger.core.designsystem.component.MerchantHeader
// This file exists only to prevent import breakage during migration.
// TODO: Remove once all consumers import from the shared location.
EOF

# ---- ReviewInboxUiState.kt ----
cat > "$B/feature/review/presentation/ReviewInboxUiState.kt" << 'EOF'
package com.sherif.ledger.feature.review.presentation

/**
 * Presentation state for the Review Inbox.
 */
data class ReviewInboxUiState(
    val items: List<ReviewItemUi>,
    val selectedFilter: ReviewFilter = ReviewFilter.All,
    val pendingCount: Int = 0,
    val confirmedTodayCount: Int = 0,
    val ignoredTodayCount: Int = 0,
)

enum class ReviewFilter(val label: String) {
    All("All"),
    LowConfidence("Low"),
    MediumConfidence("Medium"),
    HighConfidence("High"),
}

data class ReviewItemUi(
    val id: String,
    val merchant: String,
    val merchantCategory: String,
    val merchantAccentHue: Long,
    val amount: String,
    val isIncome: Boolean = false,
    val suggestedCategory: String,
    val suggestedAccount: String,
    val confidence: Int,
    val reason: String,
    val timestamp: String,
)
EOF

# ---- ReviewInboxPreviewData.kt ----
cat > "$B/feature/review/presentation/preview/ReviewInboxPreviewData.kt" << 'EOF'
package com.sherif.ledger.feature.review.presentation.preview

import com.sherif.ledger.core.preview.PreviewAccounts
import com.sherif.ledger.core.preview.PreviewMerchants
import com.sherif.ledger.feature.review.presentation.ReviewFilter
import com.sherif.ledger.feature.review.presentation.ReviewInboxUiState
import com.sherif.ledger.feature.review.presentation.ReviewItemUi

object ReviewInboxPreviewData {

    val state = ReviewInboxUiState(
        pendingCount = 5,
        confirmedTodayCount = 3,
        ignoredTodayCount = 1,
        items = listOf(
            ReviewItemUi(
                id = "r1", merchant = PreviewMerchants.noon.name,
                merchantCategory = PreviewMerchants.noon.category,
                merchantAccentHue = PreviewMerchants.noon.accentHue,
                amount = "245", suggestedCategory = "Shopping",
                suggestedAccount = "${PreviewAccounts.adcb.name} ${PreviewAccounts.adcb.accountNumber}",
                confidence = 72, reason = "Low confidence parse",
                timestamp = "Yesterday, 1:20 PM",
            ),
            ReviewItemUi(
                id = "r2", merchant = "Unknown Merchant",
                merchantCategory = "Unknown",
                merchantAccentHue = 0xFF6B7280,
                amount = "89.50", suggestedCategory = "Others",
                suggestedAccount = "${PreviewAccounts.adcb.name} ${PreviewAccounts.adcb.accountNumber}",
                confidence = 34, reason = "Merchant not recognized",
                timestamp = "Yesterday, 10:15 AM",
            ),
            ReviewItemUi(
                id = "r3", merchant = PreviewMerchants.careem.name,
                merchantCategory = PreviewMerchants.careem.category,
                merchantAccentHue = PreviewMerchants.careem.accentHue,
                amount = "28", suggestedCategory = "Transport",
                suggestedAccount = "${PreviewAccounts.wio.name} ${PreviewAccounts.wio.accountNumber}",
                confidence = 91, reason = "Amount differs from usual",
                timestamp = "Mon, 6:30 PM",
            ),
            ReviewItemUi(
                id = "r4", merchant = PreviewMerchants.lulu.name,
                merchantCategory = PreviewMerchants.lulu.category,
                merchantAccentHue = PreviewMerchants.lulu.accentHue,
                amount = "312", suggestedCategory = "Groceries",
                suggestedAccount = "${PreviewAccounts.adcb.name} ${PreviewAccounts.adcb.accountNumber}",
                confidence = 85, reason = "New merchant variant",
                timestamp = "Mon, 2:45 PM",
            ),
            ReviewItemUi(
                id = "r5", merchant = PreviewMerchants.etisalat.name,
                merchantCategory = PreviewMerchants.etisalat.category,
                merchantAccentHue = PreviewMerchants.etisalat.accentHue,
                amount = "349", suggestedCategory = "Bills",
                suggestedAccount = "${PreviewAccounts.fab.name} ${PreviewAccounts.fab.accountNumber}",
                confidence = 58, reason = "Duplicate transaction detected",
                timestamp = "Sun, 12:00 AM",
            ),
        ),
    )

    val emptyState = ReviewInboxUiState(items = emptyList())
}
EOF

# ---- ReviewCard.kt ----
cat > "$B/feature/review/presentation/components/ReviewCard.kt" << 'EOF'
package com.sherif.ledger.feature.review.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.review.presentation.ReviewItemUi

/**
 * Review card for one transaction requiring confirmation.
 *
 * Stateless. Actions are callbacks, no internal state.
 * The card tells a story: who, how much, what the parser thinks,
 * why it needs review, and what the user can do about it.
 */
@Composable
fun ReviewCard(
    item: ReviewItemUi,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountColor = if (item.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (item.isIncome) "+" else "-"
    val confidenceColor = when {
        item.confidence >= 80 -> LedgerTheme.colors.income
        item.confidence >= 50 -> LedgerTheme.colors.pending
        else -> LedgerTheme.colors.expense
    }

    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Group),
    ) {
        // Merchant + amount
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerAvatar(
                name = item.merchant,
                color = Color(item.merchantAccentHue),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(item.merchant, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(item.timestamp, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            Text(
                "${sign}AED ${item.amount}",
                style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = amountColor,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerHairline()
        Spacer(Modifier.height(LedgerSpacing.Small))

        // Parsed fields
        DetailRow("Category", item.suggestedCategory)
        DetailRow("Account", item.suggestedAccount)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Confidence", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text("${item.confidence}%", style = LedgerTextStyles.Label, color = confidenceColor)
        }

        Spacer(Modifier.height(LedgerSpacing.Content))

        // Reason
        Text(
            "\u26A0 ${item.reason}",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.pending,
        )

        Spacer(Modifier.height(LedgerSpacing.Group))

        // Actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
            LedgerButton("Ignore", onClick = onIgnore, style = LedgerButtonStyle.Text, modifier = Modifier.weight(1f))
            LedgerButton("Edit", onClick = onEdit, style = LedgerButtonStyle.Secondary, modifier = Modifier.weight(1f))
            LedgerButton("Confirm", onClick = onConfirm, style = LedgerButtonStyle.Primary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = LedgerSpacing.Inline),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Text(value, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
    }
}
EOF

# ---- ReviewInboxScreen.kt ----
cat > "$B/feature/review/presentation/ReviewInboxScreen.kt" << 'EOF'
package com.sherif.ledger.feature.review.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.review.presentation.components.ReviewCard
import com.sherif.ledger.feature.review.presentation.preview.ReviewInboxPreviewData

@Composable
fun ReviewInboxScreen(
    state: ReviewInboxUiState = ReviewInboxPreviewData.state,
) {
    var selectedFilter by remember { mutableStateOf(state.selectedFilter) }
    val filtered = when (selectedFilter) {
        ReviewFilter.All -> state.items
        ReviewFilter.LowConfidence -> state.items.filter { it.confidence < 50 }
        ReviewFilter.MediumConfidence -> state.items.filter { it.confidence in 50..79 }
        ReviewFilter.HighConfidence -> state.items.filter { it.confidence >= 80 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.XLarge, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Group),
    ) {
        item("header") {
            Text("Review", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.label)
            Spacer(Modifier.height(LedgerSpacing.Content))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge)) {
                SummaryCount("Pending", state.pendingCount, LedgerTheme.colors.pending)
                SummaryCount("Confirmed", state.confirmedTodayCount, LedgerTheme.colors.income)
                SummaryCount("Ignored", state.ignoredTodayCount, LedgerTheme.colors.tertiaryLabel)
            }
        }

        item("filters") {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
            ) {
                ReviewFilter.entries.forEach { filter ->
                    FilterChip(
                        label = filter.label,
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item("empty") {
                Spacer(Modifier.height(LedgerSpacing.XxLarge))
                LedgerEmptyState(
                    title = "All clear",
                    message = "No transactions need review right now.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                ReviewCard(
                    item = item,
                    onConfirm = { /* preview only */ },
                    onEdit = { /* preview only */ },
                    onIgnore = { /* preview only */ },
                )
            }
        }
    }
}

@Composable
private fun SummaryCount(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text("$count", style = LedgerTextStyles.Title, color = color)
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LedgerTheme.colors.tint else LedgerTheme.colors.surfaceLevel1
    val fg = if (selected) LedgerTheme.colors.onTint else LedgerTheme.colors.secondaryLabel

    LedgerSurface(
        modifier = Modifier
            .clip(LedgerShapes.small)
            .clickable(onClick = onClick),
        level = if (selected) LedgerSurfaceLevel.Level2 else LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Inline),
    ) {
        Text(label, style = LedgerTextStyles.Caption, color = fg)
    }
}
EOF

# ---- MainActivity.kt ----
cat > "$B/MainActivity.kt" << 'EOF'
package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.review.presentation.ReviewInboxScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import dagger.hilt.android.AndroidEntryPoint

enum class DevScreen { Dashboard, Accounts, Transactions, TransactionDetails, ReviewInbox }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                when (DEV_ACTIVE_SCREEN) {
                    DevScreen.Dashboard -> LedgerNavHost()
                    DevScreen.Accounts -> AccountsScreen()
                    DevScreen.Transactions -> TransactionsScreen()
                    DevScreen.TransactionDetails -> TransactionDetailsScreen()
                    DevScreen.ReviewInbox -> ReviewInboxScreen()
                }
            }
        }
    }

    companion object {
        val DEV_ACTIVE_SCREEN = DevScreen.Dashboard
    }
}
EOF

# ---- Update TransactionDetailsScreen to use shared MerchantHeader ----
if grep -q "detail.components.MerchantHeader" "$B/feature/transactions/presentation/detail/TransactionDetailsScreen.kt" 2>/dev/null; then
    sed -i 's/com.sherif.ledger.feature.transactions.presentation.detail.components.MerchantHeader/com.sherif.ledger.core.designsystem.component.MerchantHeader/' \
        "$B/feature/transactions/presentation/detail/TransactionDetailsScreen.kt"
    echo "Updated TransactionDetailsScreen import."
fi

echo "Done. 7 files written."
echo "Run: git add -A && git commit -m 'feat(review): sprint 3 review inbox with filter chips and canonical preview'"
echo "Then: ./gradlew assembleDebug"

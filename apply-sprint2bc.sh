#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 2B refinements + Sprint 2C..."

# ---- TransactionRow.kt (refined) ----
cat > "$B/feature/transactions/presentation/components/TransactionRow.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.MerchantCategory
import com.sherif.ledger.feature.transactions.presentation.TransactionState
import com.sherif.ledger.feature.transactions.presentation.TransactionUi

/**
 * Stateless transaction row for the financial timeline.
 *
 * Responsibilities:
 * - Render one transaction with visual hierarchy: amount > merchant > metadata.
 * - Communicate direction, pending state, and recurrence through text and color.
 * - Meet the 56dp accessibility touch target.
 *
 * Non-responsibilities:
 * - No business logic, formatting, or data transformation.
 * - No expansion, selection, or gesture handling beyond tap.
 * - No currency formatting (deferred until currency domain exists).
 *
 * Future consumers:
 * - TransactionsScreen (Sprint 2C, immediate)
 * - Transaction detail sheet (future)
 * - Dashboard recent activity (potential replacement for LedgerTransactionRow)
 * - Review Inbox (pending transaction confirmation)
 */
@Composable
fun TransactionRow(
    transaction: TransactionUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val catColor = transaction.category.toColor()
    // TODO: Transaction direction is currently inferred from MerchantCategory.Salary.
    // This is temporary. Direction will become a dedicated domain concept once
    // multiple income sources exist (salary, refunds, cashback, interest, dividends).
    val isIncome = transaction.category == MerchantCategory.Salary
    val amountColor = if (isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (isIncome) "+" else "-"
    val dimmed = transaction.state == TransactionState.Pending

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerAvatar(
            name = transaction.merchant,
            color = catColor,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(LedgerSpacing.Small))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
        ) {
            Text(
                transaction.merchant,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label.let { if (dimmed) it.copy(alpha = LedgerTheme.opacity.Secondary) else it },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${transaction.subtitle} \u00B7 ${transaction.category.displayName()}",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(LedgerSpacing.Content))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${sign}AED ${transaction.amount.stripTrailingZeros().toPlainString()}",
                style = LedgerTextStyles.Label,
                color = amountColor.let { if (dimmed) it.copy(alpha = LedgerTheme.opacity.Secondary) else it },
            )
            when (transaction.state) {
                TransactionState.Pending -> Text(
                    "\u25CF Pending",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.pending,
                )
                TransactionState.Recurring -> Text(
                    "\u21BA Monthly",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
                TransactionState.Posted -> {}
            }
        }
    }
}

// TODO: Category colors are local to the transactions feature. These will
// migrate into semantic LDL merchant palette tokens once multiple screens
// consume them.
private fun MerchantCategory.toColor(): Color = when (this) {
    MerchantCategory.Grocery -> Color(0xFF22C55E)
    MerchantCategory.Shopping -> Color(0xFFA855F7)
    MerchantCategory.Coffee -> Color(0xFF92400E)
    MerchantCategory.Fuel -> Color(0xFFEF4444)
    MerchantCategory.Salary -> Color(0xFF047857)
    MerchantCategory.Bills -> Color(0xFFEAB308)
    MerchantCategory.Transport -> Color(0xFF3B82F6)
    MerchantCategory.Entertainment -> Color(0xFFEC4899)
    MerchantCategory.Electronics -> Color(0xFF6B7280)
    MerchantCategory.Food -> Color(0xFFF97316)
    MerchantCategory.Healthcare -> Color(0xFF14B8A6)
    MerchantCategory.Travel -> Color(0xFF0EA5E9)
    MerchantCategory.Education -> Color(0xFF8B5CF6)
}

private fun MerchantCategory.displayName(): String = when (this) {
    MerchantCategory.Grocery -> "Groceries"
    MerchantCategory.Shopping -> "Shopping"
    MerchantCategory.Coffee -> "Coffee"
    MerchantCategory.Fuel -> "Fuel"
    MerchantCategory.Salary -> "Income"
    MerchantCategory.Bills -> "Bills"
    MerchantCategory.Transport -> "Transport"
    MerchantCategory.Entertainment -> "Entertainment"
    MerchantCategory.Electronics -> "Electronics"
    MerchantCategory.Food -> "Food"
    MerchantCategory.Healthcare -> "Healthcare"
    MerchantCategory.Travel -> "Travel"
    MerchantCategory.Education -> "Education"
}
EOF

# ---- TimelineSection.kt (fillMaxWidth) ----
cat > "$B/feature/transactions/presentation/components/TimelineSection.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.feature.transactions.presentation.TransactionGroupUi

/**
 * One complete day in the financial timeline.
 *
 * Composes [TimelineDateHeader], [DaySummary], and a grouped
 * [LedgerSurface] of [TransactionRow] entries separated by
 * [LedgerHairline]. This is the repeating unit that
 * TransactionsScreen renders per date group.
 */
@Composable
fun TimelineSection(
    group: TransactionGroupUi,
    modifier: Modifier = Modifier,
    onTransactionClick: ((String) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
    ) {
        TimelineDateHeader(title = group.title)
        DaySummary(summary = group.summary)

        LedgerSurface(
            level = LedgerSurfaceLevel.Level1,
            contentPadding = PaddingValues(0.dp),
        ) {
            group.transactions.forEachIndexed { index, txn ->
                TransactionRow(
                    transaction = txn,
                    onClick = onTransactionClick?.let { { it(txn.id) } },
                )
                if (index != group.transactions.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = 68.dp))
                }
            }
        }
    }
}
EOF

# ---- TransactionsScreen.kt (Sprint 2C) ----
cat > "$B/feature/transactions/presentation/TransactionsScreen.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerSearchBar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.components.TimelineSection
import com.sherif.ledger.feature.transactions.presentation.preview.TransactionsPreviewData

@Composable
fun TransactionsScreen(
    state: TransactionsUiState = TransactionsPreviewData.state,
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            top = LedgerSpacing.XLarge,
            bottom = LedgerSpacing.ScreenBottom,
        ),
    ) {
        item(key = "title") {
            Text(
                "Transactions",
                style = LedgerTextStyles.Headline,
                color = LedgerTheme.colors.label,
                modifier = Modifier.padding(horizontal = LedgerSpacing.Screen),
            )
            Spacer(Modifier.height(LedgerSpacing.XxLarge))
        }

        item(key = "search") {
            LedgerSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search transactions",
                modifier = Modifier.padding(horizontal = LedgerSpacing.Screen),
            )
            Spacer(Modifier.height(LedgerSpacing.Section))
        }

        items(state.groups, key = { it.id }) { group ->
            TimelineSection(group = group)
            Spacer(Modifier.height(LedgerSpacing.Section))
        }
    }
}
EOF

# ---- MainActivity.kt (DevScreen + Transactions) ----
cat > "$B/MainActivity.kt" << 'EOF'
package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import dagger.hilt.android.AndroidEntryPoint

enum class DevScreen { Dashboard, Accounts, Transactions }

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
                }
            }
        }
    }

    companion object {
        val DEV_ACTIVE_SCREEN = DevScreen.Dashboard
    }
}
EOF

echo "Done. 4 files written."
echo "Run: git add -A && git commit -m 'feat(transactions): sprint 2B refinements + 2C transactions screen'"
echo "Then: ./gradlew assembleDebug"

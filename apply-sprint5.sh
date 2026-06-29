#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 5 — Polish and interactivity..."

# ---- 1. Dashboard navigation callbacks ----
# We only touch the NavHost to pass lambdas, and RecentTransactionsSection to accept them.
# DashboardScreen itself doesn't need modification — the lambdas flow through its children.

# Make RecentTransactionsSection interactive
cat > "$B/presentation/dashboard/components/RecentTransactionsSection.kt" << 'EOF'
package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent activity", style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)
            Text(
                "See all",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tint,
                modifier = if (onSeeAllClick != null) Modifier.clickable(onClick = onSeeAllClick) else Modifier,
            )
        }

        LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(horizontal = LedgerSpacing.Medium)) {
            state.recentTransactions.forEachIndexed { index, txn ->
                val sign = if (txn.isExpense) "-" else "+"
                LedgerTransactionRow(
                    title = txn.merchant,
                    subtitle = txn.category,
                    amount = "$sign${txn.amount}",
                    amountColor = if (txn.isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income,
                )
                if (index != state.recentTransactions.lastIndex) {
                    LedgerHairline()
                }
            }
        }
    }
}
EOF

# ---- 2. Update NavHost: pass callbacks to Dashboard sections ----
cat > "$B/presentation/navigation/LedgerNavHost.kt" << 'EOF'
package com.sherif.ledger.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sherif.ledger.core.designsystem.theme.LedgerMotion
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.review.presentation.ReviewInboxScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.presentation.dashboard.DashboardScreen

private val enterTransition = slideInHorizontally(tween(LedgerMotion.StandardTweenMs)) { it / 5 } + fadeIn(tween(LedgerMotion.StandardTweenMs))
private val exitTransition = slideOutHorizontally(tween(LedgerMotion.StandardTweenMs)) { -it / 5 } + fadeOut(tween(LedgerMotion.FastTweenMs))
private val popEnterTransition = slideInHorizontally(tween(LedgerMotion.StandardTweenMs)) { -it / 5 } + fadeIn(tween(LedgerMotion.StandardTweenMs))
private val popExitTransition = slideOutHorizontally(tween(LedgerMotion.StandardTweenMs)) { it / 5 } + fadeOut(tween(LedgerMotion.FastTweenMs))

@Composable
fun LedgerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LedgerRoute.Home.route,
        modifier = modifier,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
    ) {
        composable(LedgerRoute.Home.route) {
            DashboardScreen(
                onNavigateToTransactions = {
                    navController.navigate(LedgerRoute.Transactions.route) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(LedgerRoute.Accounts.route) {
            AccountsScreen()
        }

        composable(LedgerRoute.Transactions.route) {
            TransactionsScreen(
                onTransactionClick = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id))
                },
            )
        }

        composable(LedgerRoute.ReviewInbox.route) {
            ReviewInboxScreen(
                onReviewItemClick = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id))
                },
            )
        }

        composable(
            route = LedgerRoute.TransactionDetails.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) {
            TransactionDetailsScreen()
        }
    }
}
EOF

# ---- 3. DashboardScreen: add onNavigateToTransactions callback ----
# Read current file and check if it already has the param
if ! grep -q "onNavigateToTransactions" "$B/presentation/dashboard/DashboardScreen.kt" 2>/dev/null; then
    # Add the parameter to DashboardScreen function signature
    sed -i 's/fun DashboardScreen(/fun DashboardScreen(\n    onNavigateToTransactions: () -> Unit = {},/' \
        "$B/presentation/dashboard/DashboardScreen.kt"
    
    # Pass it to RecentTransactionsSection
    sed -i 's/RecentTransactionsSection(state)/RecentTransactionsSection(state, onSeeAllClick = onNavigateToTransactions)/' \
        "$B/presentation/dashboard/DashboardScreen.kt"
    
    echo "Patched DashboardScreen with navigation callback."
fi

# ---- 4. Cleanup orphaned files ----
echo "Removing orphaned files..."
rm -f "$B/ui/LedgerRoot.kt"
rm -f "$B/ui/navigation/LedgerNavHost.kt"
rm -f "$B/ui/navigation/Destinations.kt"
rm -f "$B/presentation/theme/Color.kt"
rm -f "$B/presentation/theme/Type.kt"
rm -f "$B/core/designsystem/component/action/LedgerQuickAction.kt"
rm -f "$B/core/designsystem/component/action/LedgerQuickActionsRow.kt"
rm -f "$B/presentation/dashboard/DashboardViewModel.kt"
rm -f "$B/presentation/dashboard/components/GreetingHeader.kt"
rm -f "$B/presentation/dashboard/components/MonthlySummaryCard.kt"
rm -f "$B/feature/transactions/presentation/detail/components/MerchantHeader.kt"

# Remove empty directories
rmdir "$B/ui/navigation" 2>/dev/null || true
rmdir "$B/ui" 2>/dev/null || true
rmdir "$B/core/designsystem/component/action" 2>/dev/null || true

# Remove .gitkeep files from populated feature directories
find "$B/feature" -name ".gitkeep" -delete 2>/dev/null || true

echo "Done. Sprint 5 complete."
echo "Run: git add -A && git commit -m 'feat(polish): sprint 5 dashboard interactivity, orphan cleanup, navigation wiring'"
echo "Then: ./gradlew assembleDebug"

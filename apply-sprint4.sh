#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 4 — Real Navigation..."

# ---- LedgerRoute.kt ----
cat > "$B/presentation/navigation/LedgerRoute.kt" << 'EOF'
package com.sherif.ledger.presentation.navigation

/**
 * All navigation destinations in Ledger.
 */
sealed class LedgerRoute(val route: String) {
    data object Home : LedgerRoute("home")
    data object Accounts : LedgerRoute("accounts")
    data object Transactions : LedgerRoute("transactions")
    data object ReviewInbox : LedgerRoute("review")
    data object TransactionDetails : LedgerRoute("transaction/{transactionId}") {
        fun create(transactionId: String): String = "transaction/$transactionId"
    }
}
EOF

# ---- LedgerBottomBar.kt ----
cat > "$B/presentation/navigation/LedgerBottomBar.kt" << 'EOF'
package com.sherif.ledger.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

private enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    Home(LedgerRoute.Home.route, "Home", Icons.Rounded.Home),
    Accounts(LedgerRoute.Accounts.route, "Accounts", Icons.Rounded.AccountBalance),
    Activity(LedgerRoute.Transactions.route, "Activity", Icons.Rounded.ReceiptLong),
    Review(LedgerRoute.ReviewInbox.route, "Review", Icons.Rounded.FactCheck),
}

@Composable
fun LedgerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(
        containerColor = LedgerTheme.colors.surfaceLevel1,
        contentColor = LedgerTheme.colors.label,
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = LedgerTextStyles.Caption) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LedgerTheme.colors.tint,
                    selectedTextColor = LedgerTheme.colors.tint,
                    unselectedIconColor = LedgerTheme.colors.tertiaryLabel,
                    unselectedTextColor = LedgerTheme.colors.tertiaryLabel,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
EOF

# ---- LedgerNavHost.kt ----
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
            DashboardScreen()
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
            // TODO: Look up transaction by ID when ViewModel exists. Preview data for now.
            TransactionDetailsScreen()
        }
    }
}
EOF

# ---- TransactionsScreen.kt (add onTransactionClick) ----
cat > "$B/feature/transactions/presentation/TransactionsScreen.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
    onTransactionClick: ((String) -> Unit)? = null,
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(top = LedgerSpacing.XLarge, bottom = LedgerSpacing.ScreenBottom),
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
            TimelineSection(group = group, onTransactionClick = onTransactionClick)
            Spacer(Modifier.height(LedgerSpacing.Section))
        }
    }
}
EOF

# ---- ReviewInboxScreen.kt (add onReviewItemClick) ----
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onReviewItemClick: ((String) -> Unit)? = null,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
                ReviewFilter.entries.forEach { filter ->
                    FilterChip(label = filter.label, selected = selectedFilter == filter, onClick = { selectedFilter = filter })
                }
            }
        }

        if (filtered.isEmpty()) {
            item("empty") {
                Spacer(Modifier.height(LedgerSpacing.XxLarge))
                LedgerEmptyState(title = "All clear", message = "No transactions need review right now.")
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                ReviewCard(
                    item = item,
                    onConfirm = {},
                    onEdit = {},
                    onIgnore = {},
                    onClick = { onReviewItemClick?.invoke(item.id) },
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
    val fg = if (selected) LedgerTheme.colors.onTint else LedgerTheme.colors.secondaryLabel
    LedgerSurface(
        modifier = Modifier.clip(LedgerShapes.small).clickable(onClick = onClick),
        level = if (selected) LedgerSurfaceLevel.Level2 else LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Inline),
    ) {
        Text(label, style = LedgerTextStyles.Caption, color = fg)
    }
}
EOF

# ---- ReviewCard.kt (add onClick) ----
cat > "$B/feature/review/presentation/components/ReviewCard.kt" << 'EOF'
package com.sherif.ledger.feature.review.presentation.components

import androidx.compose.foundation.clickable
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

@Composable
fun ReviewCard(
    item: ReviewItemUi,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val amountColor = if (item.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (item.isIncome) "+" else "-"
    val confidenceColor = when {
        item.confidence >= 80 -> LedgerTheme.colors.income
        item.confidence >= 50 -> LedgerTheme.colors.pending
        else -> LedgerTheme.colors.expense
    }

    LedgerSurface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Group),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerAvatar(name = item.merchant, color = Color(item.merchantAccentHue), modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(item.merchant, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(item.timestamp, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            Text("${sign}AED ${item.amount}", style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp), color = amountColor)
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerHairline()
        Spacer(Modifier.height(LedgerSpacing.Small))
        DetailRow("Category", item.suggestedCategory)
        DetailRow("Account", item.suggestedAccount)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Confidence", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text("${item.confidence}%", style = LedgerTextStyles.Label, color = confidenceColor)
        }
        Spacer(Modifier.height(LedgerSpacing.Content))
        Text("\u26A0 ${item.reason}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.pending)
        Spacer(Modifier.height(LedgerSpacing.Group))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
            LedgerButton("Ignore", onClick = onIgnore, style = LedgerButtonStyle.Text, modifier = Modifier.weight(1f))
            LedgerButton("Edit", onClick = onEdit, style = LedgerButtonStyle.Secondary, modifier = Modifier.weight(1f))
            LedgerButton("Confirm", onClick = onConfirm, style = LedgerButtonStyle.Primary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = LedgerSpacing.Inline), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Text(value, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.navigation.LedgerBottomBar
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import com.sherif.ledger.presentation.navigation.LedgerRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                LedgerApp()
            }
        }
    }
}

@Composable
private fun LedgerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = setOf(
        LedgerRoute.Home.route,
        LedgerRoute.Accounts.route,
        LedgerRoute.Transactions.route,
        LedgerRoute.ReviewInbox.route,
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in tabRoutes) {
                LedgerBottomBar(navController)
            }
        },
    ) { padding ->
        LedgerNavHost(navController, Modifier.padding(padding))
    }
}
EOF

echo "Done. 7 files written."
echo "Run: git add -A && git commit -m 'feat(nav): sprint 4 production navigation with transitions and bottom bar'"
echo "Then: ./gradlew assembleDebug"

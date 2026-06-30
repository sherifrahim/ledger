package com.sherif.ledger.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.analytics.presentation.InsightsScreen
import com.sherif.ledger.feature.review.presentation.ReviewInboxScreen
import com.sherif.ledger.feature.settings.presentation.ProfileScreen
import com.sherif.ledger.feature.settings.presentation.SettingsScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.presentation.dashboard.DashboardScreen
import com.sherif.ledger.presentation.dashboard.SearchFilterScreen

@Composable
fun LedgerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LedgerRoute.Home.route,
        modifier = modifier,
        enterTransition = { LedgerAnimations.screenEnter },
        exitTransition = { LedgerAnimations.screenExit },
        popEnterTransition = { LedgerAnimations.screenPopEnter },
        popExitTransition = { LedgerAnimations.screenPopExit },
    ) {
        composable(LedgerRoute.Home.route) {
            DashboardScreen(
                onNavigateToTransactions = {
                    navController.navigate(LedgerRoute.Transactions.route) { launchSingleTop = true }
                },
            )
        }

        composable(LedgerRoute.Accounts.route) {
            AccountsScreen()
        }

        composable(LedgerRoute.Insights.route) {
            InsightsScreen()
        }

        composable(LedgerRoute.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(LedgerRoute.Settings.route) }
            )
        }

        composable(LedgerRoute.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(LedgerRoute.Transactions.route) {
            TransactionsScreen(
                onTransactionClick = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id))
                },
                onSearchClick = {
                    navController.navigate(LedgerRoute.SearchFilter.route)
                }
            )
        }

        composable(LedgerRoute.SearchFilter.route) {
            SearchFilterScreen(
                onBackClick = { navController.popBackStack() }
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
            TransactionDetailsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

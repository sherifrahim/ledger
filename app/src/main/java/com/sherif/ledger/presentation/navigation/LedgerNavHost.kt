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

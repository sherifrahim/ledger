package com.sherif.ledger.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.accounts.presentation.viewmodel.AccountsViewModel
import com.sherif.ledger.feature.analytics.presentation.InsightsScreen
import com.sherif.ledger.feature.analytics.presentation.viewmodel.InsightsViewModel
import com.sherif.ledger.feature.onboarding.presentation.ProfileSetupScreen
import com.sherif.ledger.feature.onboarding.presentation.SmsOnboardingScreen
import com.sherif.ledger.feature.review.presentation.ReviewInboxScreen
import com.sherif.ledger.feature.settings.presentation.AdjustBalanceScreen
import com.sherif.ledger.feature.settings.presentation.ProfileScreen
import com.sherif.ledger.feature.settings.presentation.SettingsScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.viewmodel.TransactionsViewModel
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.viewmodel.TransactionDetailsViewModel
import com.sherif.ledger.presentation.dashboard.DashboardScreen
import com.sherif.ledger.presentation.dashboard.SearchFilterScreen
import com.sherif.ledger.presentation.dashboard.viewmodel.DashboardViewModel

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
            val viewModel: DashboardViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.core.common.logging.LedgerLogger.d("NavHost: COLLECTED DashboardState=$state")
            
            DashboardScreen(
                state = state,
                onNavigateToTransactions = {
                    navController.navigate(LedgerRoute.Transactions.route) { launchSingleTop = true }
                },
                onNavigateToInsights = {
                    navController.navigate(LedgerRoute.Insights.route)
                }
            )
        }

        composable(LedgerRoute.Accounts.route) {
            val viewModel: AccountsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.core.common.logging.LedgerLogger.d("NavHost: COLLECTED AccountsState=$state")

            AccountsScreen(
                state = state,
                onNavigateToInsights = {
                    navController.navigate(LedgerRoute.Insights.route)
                }
            )
        }

        composable(LedgerRoute.Insights.route) {
            val viewModel: InsightsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            InsightsScreen(state = state)
        }

        composable(LedgerRoute.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(LedgerRoute.Settings.route) },
                onNavigateToDebugConsole = { navController.navigate(LedgerRoute.DebugConsole.route) },
                onNavigateToAdjustBalance = { navController.navigate(LedgerRoute.AdjustBalance.route) },
                onNavigateToEditProfile = { navController.navigate(LedgerRoute.EditProfile.route) },
            )
        }

        composable(LedgerRoute.EditProfile.route) {
            ProfileSetupScreen(
                onComplete = { navController.popBackStack() },
                isEditMode = true,
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(LedgerRoute.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(LedgerRoute.AdjustBalance.route) {
            AdjustBalanceScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(LedgerRoute.Transactions.route) {
            val viewModel: TransactionsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.core.common.logging.LedgerLogger.d("NavHost: COLLECTED TransactionsState. Groups=${state.groups.size}")

            TransactionsScreen(
                state = state,
                onTransactionClick = { id ->
                    com.sherif.ledger.core.common.logging.LedgerLogger.d("NAVIGATING: TransactionDetails with ID=$id")
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

        composable(LedgerRoute.SmsOnboarding.route) {
            SmsOnboardingScreen(
                onComplete = {
                    navController.navigate(LedgerRoute.Home.route) {
                        popUpTo(LedgerRoute.SmsOnboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = LedgerRoute.TransactionDetails.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
            com.sherif.ledger.core.common.logging.LedgerLogger.d("NavHost: REACHED TransactionDetails route. ID in arguments: $transactionId")
            
            val viewModel: TransactionDetailsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.core.common.logging.LedgerLogger.d("NavHost: COLLECTED TransactionDetailsState. Merchant=${state?.merchant}")

            state?.let {
                TransactionDetailsScreen(
                    state = it,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        debugNavGraph(navController)
    }
}

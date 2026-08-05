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
import com.sherif.ledger.feature.ai.presentation.AiSettingsScreen
import com.sherif.ledger.feature.analytics.presentation.InsightsScreen
import com.sherif.ledger.feature.analytics.presentation.viewmodel.InsightsViewModel
import com.sherif.ledger.feature.onboarding.presentation.ProfileSetupScreen
import com.sherif.ledger.feature.onboarding.presentation.SmsOnboardingScreen
import com.sherif.ledger.feature.review.presentation.ReviewInboxScreen
import com.sherif.ledger.feature.search.presentation.UniversalSearchScreen
import com.sherif.ledger.feature.story.presentation.FinancialStoryScreen
import com.sherif.ledger.feature.settings.presentation.AdjustBalanceScreen
import com.sherif.ledger.feature.settings.presentation.ProfileScreen
import com.sherif.ledger.feature.settings.presentation.SettingsScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.viewmodel.TransactionsViewModel
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.viewmodel.TransactionDetailsViewModel
import com.sherif.ledger.presentation.dashboard.DashboardScreen
import com.sherif.ledger.presentation.dashboard.viewmodel.DashboardViewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavBackStackEntry

@Composable
fun LedgerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // A move between two bottom-bar tabs is a sideways move between peers; a move
    // to anything else is a push down the hierarchy. They must not look the same:
    // sliding a tab in from the right implies Search lives "after" Review, which is
    // not true of any tab bar. Deciding here, from the routes actually involved,
    // keeps the choice in one place rather than per-destination.
    fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
        initialState.destination.route in LedgerRoute.tabRoutes &&
            targetState.destination.route in LedgerRoute.tabRoutes

    NavHost(
        navController = navController,
        startDestination = LedgerRoute.Home.route,
        modifier = modifier,
        enterTransition = { if (isTabSwitch()) LedgerAnimations.tabEnter else LedgerAnimations.screenEnter },
        exitTransition = { if (isTabSwitch()) LedgerAnimations.tabExit else LedgerAnimations.screenExit },
        popEnterTransition = { if (isTabSwitch()) LedgerAnimations.tabEnter else LedgerAnimations.screenPopEnter },
        popExitTransition = { if (isTabSwitch()) LedgerAnimations.tabExit else LedgerAnimations.screenPopExit },
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
                },
                onSearchClick = {
                    navController.navigate(LedgerRoute.Search.route) { launchSingleTop = true }
                },
                onMerchantClick = { merchant ->
                    navController.navigate(LedgerRoute.Merchant.create(merchant)) { launchSingleTop = true }
                }
            )
        }

        // Merchant relationship page (P2) — real merchant intelligence, reached by
        // tapping a merchant in Recent Activity; the merchant identity travels as a
        // nav argument and MerchantViewModel aggregates that merchant's transactions.
        composable(
            route = LedgerRoute.Merchant.route,
            arguments = listOf(navArgument("merchantKey") { type = NavType.StringType }),
        ) {
            val viewModel: com.sherif.ledger.feature.merchant.presentation.viewmodel.MerchantViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.feature.merchant.presentation.MerchantScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
            )
        }

        // Primary destination: Financial Story — real narrative feed from the
        // intelligence engine (StoryViewModel), honest empty state until activity.
        composable(LedgerRoute.Story.route) {
            val viewModel: com.sherif.ledger.feature.story.presentation.viewmodel.StoryViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            FinancialStoryScreen(
                state = state,
                onItemClick = { id -> navController.navigate(LedgerRoute.TransactionDetails.create(id)) },
                onOpenGraph = { navController.navigate(LedgerRoute.StoryGraph.route) },
            )
        }

        // Primary destination: Universal Search — real search over captured data.
        composable(LedgerRoute.Search.route) {
            UniversalSearchScreen(
                onOpenTransactions = { navController.navigate(LedgerRoute.Transactions.route) },
                onOpenAccounts = { navController.navigate(LedgerRoute.Accounts.route) },
                onOpenInsights = { navController.navigate(LedgerRoute.Insights.route) },
                onOpenStory = { navController.navigate(LedgerRoute.Story.route) { launchSingleTop = true } },
                onResultClick = { id -> navController.navigate(LedgerRoute.TransactionDetails.create(id)) },
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
                },
                onBackClick = { navController.popBackStack() },
                onAccountClick = { accountId ->
                    navController.navigate(LedgerRoute.CreditCard.create(accountId))
                },
            )
        }

        composable(
            route = LedgerRoute.CreditCard.route,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
        ) {
            val viewModel: com.sherif.ledger.feature.creditcard.presentation.viewmodel.CreditCardViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.feature.creditcard.presentation.CreditCardScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
                onTransactionClick = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id.toString()))
                },
            )
        }

        composable(LedgerRoute.Budgets.route) {
            val viewModel: com.sherif.ledger.feature.budget.presentation.viewmodel.BudgetViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.feature.budget.presentation.BudgetScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
                onSetBudget = { category, limit, currency -> viewModel.setBudget(category, limit, currency) },
                onRemoveBudget = { category -> viewModel.removeBudget(category) },
            )
        }

        composable(LedgerRoute.Goals.route) {
            val viewModel: com.sherif.ledger.feature.goal.presentation.viewmodel.GoalViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.feature.goal.presentation.GoalScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
                onAddGoal = { name, target, accountId, currency -> viewModel.addGoal(name, target, accountId, currency) },
                onRemoveGoal = { id -> viewModel.removeGoal(id) },
            )
        }

        composable(LedgerRoute.StoryGraph.route) {
            val viewModel: com.sherif.ledger.feature.storygraph.presentation.viewmodel.StoryGraphViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            com.sherif.ledger.feature.storygraph.presentation.StoryGraphScreen(
                state = state,
                onBackClick = { navController.popBackStack() },
                onSelect = { viewModel.select(it) },
                onSearch = { viewModel.search(it) },
                onPaletteReady = { viewModel.setPalette(it) },
                onOpenTransaction = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id.toString()))
                },
                onDateRangeSelected = { viewModel.setDateRange(it) },
                onToggleAccountFilter = { viewModel.toggleAccountFilter(it) },
                onToggleCategoryFilter = { viewModel.toggleCategoryFilter(it) },
                onToggleTagFilter = { viewModel.toggleTagFilter(it) },
                onToggleKindVisible = { viewModel.toggleKindVisible(it) },
                onClearFilters = { viewModel.clearFilters() },
            )
        }

        composable(LedgerRoute.Insights.route) {
            val viewModel: InsightsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()
            InsightsScreen(state = state, onBackClick = { navController.popBackStack() })
        }

        // Settings hub (served by the Profile control hub). Also carries the entry
        // points to the secondary financial destinations (Accounts, Activity,
        // Insights) so they remain reachable now that they are off the bottom bar.
        // Their proper home is the Dashboard's Accounts/Recent-Activity/Insights
        // sections — that integration is later product-experience work.
        composable(LedgerRoute.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(LedgerRoute.Settings.route) },
                onNavigateToDebugConsole = { navController.navigate(LedgerRoute.DebugConsole.route) },
                onNavigateToAdjustBalance = { navController.navigate(LedgerRoute.AdjustBalance.route) },
                onNavigateToEditProfile = { navController.navigate(LedgerRoute.EditProfile.route) },
                onNavigateToReviewInbox = { navController.navigate(LedgerRoute.ReviewInbox.route) },
                onNavigateToAiSettings = { navController.navigate(LedgerRoute.AiSettings.route) },
                onNavigateToBudgets = { navController.navigate(LedgerRoute.Budgets.route) },
                onNavigateToGoals = { navController.navigate(LedgerRoute.Goals.route) },
                onNavigateToAccounts = { navController.navigate(LedgerRoute.Accounts.route) },
                onNavigateToActivity = { navController.navigate(LedgerRoute.Transactions.route) },
                onNavigateToInsights = { navController.navigate(LedgerRoute.Insights.route) },
                onNavigateToPrivacyPolicy = { navController.navigate(LedgerRoute.PrivacyPolicy.route) },
                onNavigateToLicenses = { navController.navigate(LedgerRoute.Licenses.route) },
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

        composable(LedgerRoute.AiSettings.route) {
            AiSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(LedgerRoute.PrivacyPolicy.route) {
            com.sherif.ledger.feature.settings.presentation.PrivacyPolicyScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(LedgerRoute.Licenses.route) {
            com.sherif.ledger.feature.settings.presentation.LicensesScreen(
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
                    navController.navigate(LedgerRoute.Search.route) { launchSingleTop = true }
                },
                onBackClick = { navController.popBackStack() },
                onAddClick = { navController.navigate(LedgerRoute.ManualEntry.route) },
            )
        }

        composable(LedgerRoute.ManualEntry.route) {
            com.sherif.ledger.feature.transactions.presentation.entry.ManualEntryScreen(
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
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

            val knownTags by viewModel.knownTags.collectAsState()

            state?.let {
                TransactionDetailsScreen(
                    state = it,
                    onBackClick = { navController.popBackStack() },
                    onSaveNote = { note -> viewModel.updateNote(note) },
                    onSplitClick = { transactionId?.let { id -> navController.navigate(LedgerRoute.Split.create(id)) } },
                    onAddTag = { name -> viewModel.addTag(name) },
                    onRemoveTag = { tagId -> viewModel.removeTag(tagId) },
                    knownTags = knownTags,
                )
            }
        }

        composable(
            route = LedgerRoute.Split.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) {
            com.sherif.ledger.feature.split.presentation.SplitScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        debugNavGraph(navController)
    }
}

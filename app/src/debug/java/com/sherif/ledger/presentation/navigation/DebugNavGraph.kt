package com.sherif.ledger.presentation.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.sherif.ledger.feature.debug.presentation.DebugConsoleScreen
import com.sherif.ledger.feature.debug.presentation.viewmodel.DebugConsoleViewModel

fun NavGraphBuilder.debugNavGraph(navController: NavHostController) {
    composable(LedgerRoute.DebugConsole.route) {
        val viewModel: DebugConsoleViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()
        val dbSummary by viewModel.dbSummary.collectAsState()
        
        DebugConsoleScreen(
            state = state,
            dbSummary = dbSummary,
            onAction = { action -> viewModel.handleAction(action) },
            onBackClick = { navController.popBackStack() }
        )
    }
}

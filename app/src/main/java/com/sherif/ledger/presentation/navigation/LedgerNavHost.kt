package com.sherif.ledger.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sherif.ledger.presentation.dashboard.DashboardScreen

@Composable
fun LedgerNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LedgerRoute.Dashboard.route,
    ) {
        composable(route = LedgerRoute.Dashboard.route) {
            DashboardScreen()
        }
    }
}

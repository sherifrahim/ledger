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

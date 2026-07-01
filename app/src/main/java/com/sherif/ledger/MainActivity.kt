package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
        
        // Root Cause Fix: Ensure the window is configured for edge-to-edge rendering.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        
        setContent {
            LedgerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LedgerTheme.colors.surfaceLevel0
                ) {
                    LedgerApp()
                }
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
        LedgerRoute.Profile.route,
    )

    Box(Modifier.fillMaxSize()) {
        LedgerNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize()
        )
        
        if (currentRoute in tabRoutes) {
            Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) {
                LedgerBottomBar(navController)
            }
        }
    }
}

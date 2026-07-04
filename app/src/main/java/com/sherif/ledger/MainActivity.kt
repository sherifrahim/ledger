package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sherif.ledger.core.common.util.PermissionUtils
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.onboarding.presentation.NotificationAccessScreen
import com.sherif.ledger.feature.onboarding.presentation.SmsOnboardingScreen
import com.sherif.ledger.presentation.navigation.LedgerBottomBar
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import com.sherif.ledger.presentation.navigation.LedgerRoute
import com.sherif.ledger.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity onCreate - Checking Notification Access")
        val isEnabled = PermissionUtils.isNotificationServiceEnabled(this)
        com.sherif.ledger.core.common.logging.LedgerLogger.d("Notification Access Status: $isEnabled")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        
        setContent {
            LedgerTheme {
                val context = LocalContext.current
                val mainViewModel: MainViewModel = hiltViewModel()
                val isSmsImported by mainViewModel.isSmsImported.collectAsState()

                var isPermissionGranted by remember { 
                    mutableStateOf(PermissionUtils.isNotificationServiceEnabled(context)) 
                }

                // Refresh state when returning from settings
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    isPermissionGranted = PermissionUtils.isNotificationServiceEnabled(context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LedgerTheme.colors.surfaceLevel0
                ) {
                    if (isPermissionGranted) {
                        if (isSmsImported) {
                            com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity: SMS Imported=true. Launching Dashboard.")
                            LedgerApp()
                        } else {
                            com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity: SMS Imported=false. Launching SMS Onboarding.")
                            SmsOnboardingScreen(onComplete = {
                                // No-op, isSmsImported will update automatically
                            })
                        }
                    } else {
                        com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity: Notification Access=false. Launching Onboarding.")
                        NotificationAccessScreen()
                    }
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
            Box(Modifier.align(Alignment.BottomCenter)) {
                LedgerBottomBar(navController)
            }
        }
    }
}

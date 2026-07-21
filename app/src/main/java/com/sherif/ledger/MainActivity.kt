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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sherif.ledger.core.common.util.PermissionUtils
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.onboarding.presentation.NotificationAccessScreen
import com.sherif.ledger.feature.onboarding.presentation.ProfileSetupScreen
import com.sherif.ledger.feature.onboarding.presentation.SmsOnboardingScreen
import com.sherif.ledger.presentation.navigation.LedgerBottomBar
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import com.sherif.ledger.presentation.navigation.LedgerRoute
import com.sherif.ledger.presentation.splash.LedgerSplashScreen
import com.sherif.ledger.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() per the SplashScreen API
        // contract. No setKeepOnScreenCondition — there's nothing slow to
        // wait for, and the system splash should hand off to our own
        // Compose sequence (LedgerSplashScreen) the instant the first frame
        // draws, never adding an artificial delay on top of it.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity onCreate - Checking Notification Access")
        val isEnabled = PermissionUtils.isNotificationServiceEnabled(this)
        com.sherif.ledger.core.common.logging.LedgerLogger.d("Notification Access Status: $isEnabled")

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Set by AndroidTransactionCaptureNotifier's "Split" action — landing
        // spot is Transaction Details until a dedicated split-creation screen exists.
        val deepLinkTransactionId = intent.getLongExtra(
            com.sherif.ledger.feature.notification.AndroidTransactionCaptureNotifier.EXTRA_TRANSACTION_ID, -1L,
        ).takeIf { it > 0 }
        
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeType by mainViewModel.themeType.collectAsState()
            
            LedgerTheme(themeType = themeType) {
                val context = LocalContext.current
                val isSmsImported by mainViewModel.isSmsImported.collectAsState()
                val isProfileSetup by mainViewModel.isProfileSetup.collectAsState()

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
                    Box(Modifier.fillMaxSize()) {
                        // The real destination is composed immediately —
                        // never blocked on the splash — so the splash's
                        // fade-out at the end reveals a screen that's
                        // already there, not a blank frame or a hard cut.
                        if (!isProfileSetup) {
                            com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity: Profile not set up. Launching Profile Setup.")
                            ProfileSetupScreen(onComplete = {
                                // No-op, isProfileSetup will update automatically
                            })
                        } else if (isPermissionGranted) {
                            if (isSmsImported) {
                                com.sherif.ledger.core.common.logging.LedgerLogger.d("MainActivity: SMS Imported=true. Launching Dashboard.")
                                LedgerApp(deepLinkTransactionId = deepLinkTransactionId)
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

                        var showSplash by remember { mutableStateOf(true) }
                        if (showSplash) {
                            LedgerSplashScreen(onFinished = { showSplash = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerApp(deepLinkTransactionId: Long? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = setOf(
        LedgerRoute.Home.route,
        LedgerRoute.Accounts.route,
        LedgerRoute.Transactions.route,
        LedgerRoute.Profile.route,
    )

    androidx.compose.runtime.LaunchedEffect(deepLinkTransactionId) {
        if (deepLinkTransactionId != null) {
            navController.navigate(LedgerRoute.TransactionDetails.create(deepLinkTransactionId.toString()))
        }
    }

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



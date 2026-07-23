package com.sherif.ledger

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
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
import com.sherif.ledger.core.designsystem.theme.LocalCardHazeState
import com.sherif.ledger.core.designsystem.theme.LocalNavHazeState
import com.sherif.ledger.core.designsystem.theme.ledgerAmbientBackground
import dev.chrisbanes.haze.hazeSource
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
        // The notification's "Split" action deep-links straight to the split screen.
        val deepLinkOpenSplit = intent.getBooleanExtra(
            com.sherif.ledger.feature.notification.AndroidTransactionCaptureNotifier.EXTRA_OPEN_SPLIT, false,
        )
        
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val themeType by mainViewModel.themeType.collectAsState()
            val liquidGlass by mainViewModel.liquidGlass.collectAsState()

            LedgerTheme(themeType = themeType, liquidGlass = liquidGlass) {
                val context = LocalContext.current

                // Android 13+: capture-confirmation notifications (and their
                // Split / Add-note / Undo actions) need POST_NOTIFICATIONS at
                // runtime. Requested once on launch; declining does not affect
                // capture itself, only the confirmation UX.
                val notifPermLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* result intentionally ignored */ }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                val isSmsImported by mainViewModel.isSmsImported.collectAsState()
                val isProfileSetup by mainViewModel.isProfileSetup.collectAsState()

                var isPermissionGranted by remember {
                    mutableStateOf(PermissionUtils.isNotificationServiceEnabled(context))
                }

                // Refresh state when returning from settings
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    isPermissionGranted = PermissionUtils.isNotificationServiceEnabled(context)
                }

                val cardHazeState = LocalCardHazeState.current

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LedgerTheme.colors.surfaceLevel0
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // Ambient backdrop that Liquid Glass cards refract. Sits
                        // behind opaque screen content, so it is invisible
                        // directly — it only appears, blurred, through glass
                        // surfaces. That is what makes a card at the top of a
                        // screen read as glass rather than a flat panel.
                        if (liquidGlass && cardHazeState != null) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .ledgerAmbientBackground(LedgerTheme.colors.isDark)
                                    .hazeSource(cardHazeState),
                            )
                        }
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
                                LedgerApp(deepLinkTransactionId = deepLinkTransactionId, deepLinkOpenSplit = deepLinkOpenSplit)
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
private fun LedgerApp(deepLinkTransactionId: Long? = null, deepLinkOpenSplit: Boolean = false) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The five primary destinations that own the floating bottom bar. Must match
    // LedgerBottomBar's tabs exactly — any drift hides the bar on a real tab (or
    // shows it, unselected, on a pushed secondary screen). Secondary destinations
    // (Accounts, Transactions, Insights, Merchant, detail screens) are reached by
    // push navigation and carry their own back affordance instead.
    val tabRoutes = setOf(
        LedgerRoute.Home.route,
        LedgerRoute.Story.route,
        LedgerRoute.ReviewInbox.route,
        LedgerRoute.Search.route,
        LedgerRoute.Profile.route,
    )

    androidx.compose.runtime.LaunchedEffect(deepLinkTransactionId, deepLinkOpenSplit) {
        if (deepLinkTransactionId != null) {
            val route = if (deepLinkOpenSplit) {
                LedgerRoute.Split.create(deepLinkTransactionId.toString())
            } else {
                LedgerRoute.TransactionDetails.create(deepLinkTransactionId.toString())
            }
            navController.navigate(route)
        }
    }

    val navHazeState = LocalNavHazeState.current
    val glass = LedgerTheme.glass

    Box(Modifier.fillMaxSize()) {
        LedgerNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                // When glass is on, the whole screen content becomes the blur
                // source the nav island samples — an authentic iOS nav bar that
                // frosts the transactions scrolling beneath it.
                .then(if (glass && navHazeState != null) Modifier.hazeSource(navHazeState) else Modifier),
        )

        if (currentRoute in tabRoutes) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                LedgerBottomBar(navController)
            }
        }
    }
}



package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Developer screen selector. Change [DEV_ACTIVE_SCREEN] to review
 * different features during development. This enum and the when-block
 * are temporary and will be replaced by production navigation.
 */
enum class DevScreen { Dashboard, Accounts }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                when (DEV_ACTIVE_SCREEN) {
                    DevScreen.Dashboard -> LedgerNavHost()
                    DevScreen.Accounts -> AccountsScreen()
                }
            }
        }
    }

    companion object {
        /** Change this value to launch a different screen. */
        val DEV_ACTIVE_SCREEN = DevScreen.Dashboard
    }
}

package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import dagger.hilt.android.AndroidEntryPoint

enum class DevScreen { Dashboard, Accounts, Transactions }

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
                    DevScreen.Transactions -> TransactionsScreen()
                }
            }
        }
    }

    companion object {
        val DEV_ACTIVE_SCREEN = DevScreen.Dashboard
    }
}

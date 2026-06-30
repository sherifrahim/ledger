package com.sherif.ledger.presentation.navigation

/**
 * All navigation destinations in Ledger.
 */
sealed class LedgerRoute(val route: String) {
    data object Home : LedgerRoute("home")
    data object Accounts : LedgerRoute("accounts")
    data object Transactions : LedgerRoute("transactions")
    data object Insights : LedgerRoute("insights")
    data object Profile : LedgerRoute("profile")
    data object Settings : LedgerRoute("settings")
    data object SearchFilter : LedgerRoute("search_filter")
    data object ReviewInbox : LedgerRoute("review")
    data object TransactionDetails : LedgerRoute("transaction/{transactionId}") {
        fun create(transactionId: String): String = "transaction/$transactionId"
    }
}

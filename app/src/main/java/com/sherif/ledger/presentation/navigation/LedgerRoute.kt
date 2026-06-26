package com.sherif.ledger.presentation.navigation

sealed class LedgerRoute(val route: String) {
    data object Dashboard : LedgerRoute("dashboard")
}

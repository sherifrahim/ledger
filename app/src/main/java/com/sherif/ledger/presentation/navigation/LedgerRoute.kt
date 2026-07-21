package com.sherif.ledger.presentation.navigation

/**
 * All navigation destinations in Ledger.
 */
sealed class LedgerRoute(val route: String) {
    // Primary destinations (spec Chapter 34): Dashboard, Story, Review, Search, Settings.
    // `Home` is the Dashboard destination (route id kept as "home" to avoid churn).
    data object Home : LedgerRoute("home")
    data object Story : LedgerRoute("story")
    data object Search : LedgerRoute("search")
    // Settings hub is served by the Profile destination (the existing control hub).
    // Secondary destinations, reachable from primary destinations (not on the bottom bar):
    data object Accounts : LedgerRoute("accounts")
    data object Transactions : LedgerRoute("transactions")
    data object Insights : LedgerRoute("insights")
    data object Profile : LedgerRoute("profile")
    data object Settings : LedgerRoute("settings")
    data object AdjustBalance : LedgerRoute("adjust_balance")
    data object EditProfile : LedgerRoute("edit_profile")
    data object AiSettings : LedgerRoute("ai_settings")
    data object SearchFilter : LedgerRoute("search_filter")
    data object ReviewInbox : LedgerRoute("review")
    data object DebugConsole : LedgerRoute("debug_console")
    data object PipelineDiagnostics : LedgerRoute("pipeline_diagnostics")
    data object LedgerDiagnostics : LedgerRoute("ledger_diagnostics")
    data object BalanceInspector : LedgerRoute("balance_inspector")
    data object AiMetrics : LedgerRoute("ai_metrics")
    data object AiDebug : LedgerRoute("ai_debug")
    data object IntelligenceInspector : LedgerRoute("intelligence_inspector")
    data object SmsOnboarding : LedgerRoute("sms_onboarding")
    data object TransactionDetails : LedgerRoute("transaction/{transactionId}") {
        fun create(transactionId: String): String = "transaction/$transactionId"
    }
}




package com.sherif.ledger.core.domain.model

/**
 * Point-in-time financial position — "right now," not "for this period." Kept
 * separate from [FinancialAnalytics] deliberately: net worth and account balances
 * aren't scoped to a date range the way spend/income/trends are, so folding them
 * into that period-scoped model would be the wrong shape. Both are produced by the
 * same [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase]
 * entry point every screen consumes — this is a second, purpose-built output of
 * that single source of truth, not a second source of truth.
 *
 * Every figure here is replayed from persisted transactions via
 * [com.sherif.ledger.core.domain.service.transaction.AccountBalanceService] —
 * never a cached field.
 */
data class NetWorthSnapshot(
    val netWorthMinor: Long,
    val currency: CurrencyCode,
    val accountBalances: List<AccountBalanceSummary>,
)

data class AccountBalanceSummary(
    val accountId: Long,
    val accountName: String,
    val accountType: AccountType,
    val isLiability: Boolean,
    val balanceMinor: Long,
)


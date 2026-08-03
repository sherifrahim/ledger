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
    /**
     * Money the user actually HAS: the total of asset (non-liability) accounts in
     * [currency]. This is what "Total Balance" means to a person — deliberately
     * NOT [netWorthMinor], which subtracts credit-card debt.
     *
     * Showing net worth under a "Total Balance" label made the dashboard read
     * AED -11,771.65 for someone holding AED 1,568.52, because an AED 11,888 card
     * balance was folded in. Debt is real and still reported, but as its own
     * figure ([cardDebtMinor]) — never silently mixed into "your balance".
     */
    val cashBalanceMinor: Long,
    /** What is owed on liability (credit-card) accounts, as a positive number. */
    val cardDebtMinor: Long,
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


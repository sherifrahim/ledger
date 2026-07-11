package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * The single, authoritative shape of financial analytics for a period. Produced
 * ONLY by [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase].
 * Every screen (Dashboard, Insights, Accounts, Transactions) consumes this instead
 * of computing its own aggregation — this is the single source of truth for
 * spending, income, refunds, transfers, categories, merchants, and trends.
 */
data class FinancialAnalytics(
    val periodStart: Instant,
    val periodEnd: Instant,
    val currency: CurrencyCode,

    /** Sum of all EXPENSE-typed transactions, unadjusted. Diagnostic only — the
     *  user-facing "spend" figure is [netSpendMinor], never this. */
    val grossExpenseMinor: Long,

    /** Real spending: gross expense, minus refunds netted against their purchase,
     *  minus transactions that are structurally not spending (credit-card
     *  payments, cash withdrawals) even though extraction typed them EXPENSE. */
    val netSpendMinor: Long,

    /** All INCOME-typed transactions (salary, etc.). Never includes refunds or
     *  incoming transfers — those are refunds/transfers, not income. */
    val incomeMinor: Long,

    /** income - netSpend. */
    val netFlowMinor: Long,

    /** Total refunded amount, netted out of [netSpendMinor]. Shown separately so
     *  the user can see refund activity without it silently vanishing. */
    val refundedMinor: Long,

    /** Amount excluded from spending because it was a credit-card payment, cash
     *  withdrawal, or other internal movement — not a purchase. Surfaced for
     *  transparency/diagnostics, never added back into netSpendMinor. */
    val excludedFromSpendingMinor: Long,

    /** Net movement OUT across matched internal transfers between the user's own
     *  accounts (transfer/savings/investment). Diagnostic — total wealth is
     *  unaffected by these; they never appear in spend or income. */
    val internalTransfersMinor: Long,

    val categoryTotals: List<CategoryTotal>,
    val merchantTotals: List<MerchantTotal>,

    /** One point per day in the period, real net spend for that day, in display
     *  order. This is what feeds the Insights line chart — never placeholder data. */
    val trendPoints: List<TrendPoint>,
)

data class CategoryTotal(
    val category: String,
    val amountMinor: Long,
    val transactionCount: Int,
)

data class MerchantTotal(
    val merchantName: String,
    val amountMinor: Long,
    val transactionCount: Int,
)

data class TrendPoint(
    val label: String,
    val amountMinor: Long,
)



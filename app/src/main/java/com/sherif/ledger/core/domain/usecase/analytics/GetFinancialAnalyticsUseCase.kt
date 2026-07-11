package com.sherif.ledger.core.domain.usecase.analytics

import com.sherif.ledger.core.domain.model.AccountBalanceSummary
import com.sherif.ledger.core.domain.model.CategoryTotal
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.FinancialAnalytics
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.MerchantTotal
import com.sherif.ledger.core.domain.model.NetWorthSnapshot
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TrendPoint
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.relationship.RelationshipEngine
import com.sherif.ledger.feature.relationship.RelationshipType
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * THE single source of truth for financial analytics. Every screen (Dashboard,
 * Insights, Accounts, Transactions) consumes this instead of computing its own
 * spending/income/category/merchant/trend aggregation.
 *
 * Composition of already-built systems, not a new pipeline:
 *  - [TransactionRepository.observeTransactionsBetween] — the existing, indexed,
 *    date-bounded repository query. Reused as-is.
 *  - [RelationshipEngine.analyze] — run ONCE per call, over the bounded window
 *    (never the full history), to identify credit-card payments, cash
 *    withdrawals, internal transfers, and refund-of-purchase links. This is what
 *    tells this use case a transaction typed EXPENSE is not actually a purchase.
 *  - [MerchantResolver] (Merchant Intelligence) — resolves category and canonical
 *    merchant name for grouping.
 *
 * Performance: one repository query, one RelationshipEngine pass, one linear scan
 * over the bounded transaction list. No per-transaction repository calls, no
 * quadratic scans over transaction history.
 */
class GetFinancialAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val relationshipEngine: RelationshipEngine,
    private val merchantResolver: MerchantResolver,
    private val accountBalanceService: AccountBalanceService,
) {

    /** Relationship types whose SOURCE transaction is EXPENSE-typed but is
     *  structurally not a purchase (credit-card settlement, ATM withdrawal). */
    private val nonSpendingRelationshipTypes = setOf(
        RelationshipType.CREDIT_CARD_PAYMENT,
        RelationshipType.CASH_WITHDRAWAL,
    )

    /** Relationship types representing a matched internal transfer between the
     *  user's own accounts — total wealth unaffected, never spend or income. */
    private val internalTransferRelationshipTypes = setOf(
        RelationshipType.TRANSFER_BETWEEN_ACCOUNTS,
        RelationshipType.SAVINGS_MOVEMENT,
        RelationshipType.INVESTMENT_CONTRIBUTION,
    )

    /**
     * Point-in-time net worth and every account's current balance. Delegates
     * entirely to [AccountBalanceService] — this use case never computes balance
     * arithmetic itself, only shapes that service's output for screens to consume.
     */
    suspend fun computeNetWorth(): NetWorthSnapshot {
        val balances = accountBalanceService.currentBalances()
        val currency = balances.firstOrNull()?.balance?.currencyCode ?: CurrencyCode.AED
        val assets = balances.filter { !it.account.type.isLiability }.sumOf { it.balance.minorUnits }
        val liabilities = balances.filter { it.account.type.isLiability }.sumOf { it.balance.minorUnits }
        return NetWorthSnapshot(
            netWorthMinor = assets - liabilities,
            currency = currency,
            accountBalances = balances.map {
                AccountBalanceSummary(
                    accountId = it.account.id,
                    accountName = it.account.name,
                    accountType = it.account.type,
                    isLiability = it.account.type.isLiability,
                    balanceMinor = it.balance.minorUnits,
                )
            },
        )
    }

    suspend fun execute(start: Instant, end: Instant): FinancialAnalytics {
        val result = transactionRepository.observeTransactionsBetween(start, end).first()
        val transactions = (result as? LedgerResult.Success)?.data ?: emptyList()
        return compute(transactions, start, end)
    }

    /**
     * Per-transaction net spend contribution: EXPENSE-typed transactions only,
     * excluded ones absent from the map, refunded ones netted. Backed by the SAME
     * single [RelationshipEngine.analyze] pass as [compute]. Lets a screen that
     * needs a different SLICE of the same period (e.g. per-day subtotals in a
     * scrollable list) stay consistent with the monthly aggregate, without running
     * relationship analysis a second time or reimplementing the exclusion/netting
     * rules independently.
     *
     * IMPORTANT: callers must pass the SAME transaction list they intend to slice
     * (e.g. all fetched transactions), not a pre-filtered subset — relationship
     * matching (refund-to-purchase, transfer legs) requires seeing both sides.
     */
    internal fun effectiveSpendByTransactionId(transactions: List<Transaction>): Map<Long, Long> {
        if (transactions.isEmpty()) return emptyMap()
        val core = analyzeCore(transactions)
        return transactions
            .filter { it.type == TransactionType.EXPENSE && it.id !in core.excludedIds }
            .associate { it.id to (it.amount.minorUnits - (core.refundedAgainstPurchase[it.id] ?: 0L)).coerceAtLeast(0L) }
    }

    /** The one relationship-analysis pass, shared by every consumer above. */
    private fun analyzeCore(transactions: List<Transaction>): AnalysisCore {
        val byId = transactions.associateBy { it.id }
        val relationships = relationshipEngine.analyze(transactions)

        val excludedIds = relationships
            .filter { it.type in nonSpendingRelationshipTypes }
            .map { it.sourceTransactionId }
            .toSet()

        val refundedAgainstPurchase: Map<Long, Long> = relationships
            .filter { it.type == RelationshipType.REFUND_OF_PURCHASE && it.targetTransactionId != null }
            .groupBy { it.targetTransactionId!! }
            .mapValues { (_, links) -> links.sumOf { byId[it.sourceTransactionId]?.amount?.minorUnits ?: 0L } }

        val internalTransfersTotal = relationships
            .filter { it.type in internalTransferRelationshipTypes }
            .sumOf { byId[it.sourceTransactionId]?.amount?.minorUnits ?: 0L }

        return AnalysisCore(excludedIds, refundedAgainstPurchase, internalTransfersTotal)
    }

    private data class AnalysisCore(
        val excludedIds: Set<Long>,
        val refundedAgainstPurchase: Map<Long, Long>,
        val internalTransfersTotal: Long,
    )

    /** Pure computation, separated from the repository call so it can be tested
     *  directly against hand-built transaction lists without a database. */
    internal fun compute(transactions: List<Transaction>, start: Instant, end: Instant): FinancialAnalytics {
        val currency = transactions.firstOrNull()?.amount?.currencyCode ?: CurrencyCode.AED

        if (transactions.isEmpty()) {
            return FinancialAnalytics(
                periodStart = start, periodEnd = end, currency = currency,
                grossExpenseMinor = 0, netSpendMinor = 0, incomeMinor = 0, netFlowMinor = 0,
                refundedMinor = 0, excludedFromSpendingMinor = 0, internalTransfersMinor = 0,
                categoryTotals = emptyList(), merchantTotals = emptyList(),
                trendPoints = buildEmptyTrend(start, end),
            )
        }

        val core = analyzeCore(transactions)

        var grossExpense = 0L
        var netSpend = 0L
        var income = 0L
        var excludedFromSpending = 0L

        val zone = ZoneId.systemDefault()
        val dailyNet = sortedMapOf<java.time.LocalDate, Long>()

        val categoryAgg = mutableMapOf<String, MutableList<Long>>()
        val merchantAgg = mutableMapOf<String, MutableList<Long>>()

        for (t in transactions) {
            when (t.type) {
                TransactionType.INCOME -> income += t.amount.minorUnits
                TransactionType.EXPENSE -> {
                    grossExpense += t.amount.minorUnits
                    if (t.id in core.excludedIds) {
                        excludedFromSpending += t.amount.minorUnits
                    } else {
                        val refunded = core.refundedAgainstPurchase[t.id] ?: 0L
                        val effective = (t.amount.minorUnits - refunded).coerceAtLeast(0L)
                        netSpend += effective
                        if (effective > 0) {
                            val day = t.timestamp.atZone(zone).toLocalDate()
                            dailyNet[day] = (dailyNet[day] ?: 0L) + effective

                            val resolution = merchantResolver.resolve(t.rawText)
                            val category = (resolution as? MerchantResolution.Resolved)?.category?.name ?: "UNKNOWN"
                            val merchantName = resolution.displayName
                            categoryAgg.getOrPut(category) { mutableListOf() }.add(effective)
                            merchantAgg.getOrPut(merchantName) { mutableListOf() }.add(effective)
                        }
                    }
                }
                TransactionType.REFUND, TransactionType.TRANSFER -> {
                    // REFUND: tracked below via refundedAgainstPurchase (matched) and
                    // the diagnostic total (all refunds). Never spend, never income.
                    // TRANSFER: never spend or income by type; internal-transfer
                    // totals are diagnostic only, computed in analyzeCore().
                }
            }
        }

        // Diagnostic: total of ALL refund-typed transactions in the period. Netting
        // against netSpend above only applies to MATCHED refund-of-purchase links
        // (Part 3: "Refund: Original expense decreases") — an unmatched refund still
        // shows here for transparency, without silently reducing an unrelated
        // expense it cannot be confidently tied to.
        val refundedTotal = transactions.filter { it.type == TransactionType.REFUND }.sumOf { it.amount.minorUnits }

        val categoryTotals = categoryAgg.map { (category, amounts) ->
            CategoryTotal(category, amounts.sum(), amounts.size)
        }.sortedByDescending { it.amountMinor }

        val merchantTotals = merchantAgg.map { (merchant, amounts) ->
            MerchantTotal(merchant, amounts.sum(), amounts.size)
        }.sortedByDescending { it.amountMinor }

        val trendPoints = buildTrend(start, end, dailyNet, zone)

        return FinancialAnalytics(
            periodStart = start,
            periodEnd = end,
            currency = currency,
            grossExpenseMinor = grossExpense,
            netSpendMinor = netSpend,
            incomeMinor = income,
            netFlowMinor = income - netSpend,
            refundedMinor = refundedTotal,
            excludedFromSpendingMinor = excludedFromSpending,
            internalTransfersMinor = core.internalTransfersTotal,
            categoryTotals = categoryTotals,
            merchantTotals = merchantTotals,
            trendPoints = trendPoints,
        )
    }

    private val dayLabelFormatter = DateTimeFormatter.ofPattern("d MMM")

    private fun buildTrend(
        start: Instant,
        end: Instant,
        dailyNet: Map<java.time.LocalDate, Long>,
        zone: ZoneId,
    ): List<TrendPoint> {
        val startDate = start.atZone(zone).toLocalDate()
        val endDate = end.atZone(zone).toLocalDate()
        if (endDate.isBefore(startDate)) return emptyList()
        val points = mutableListOf<TrendPoint>()
        var day = startDate
        while (!day.isAfter(endDate)) {
            points += TrendPoint(day.format(dayLabelFormatter), dailyNet[day] ?: 0L)
            day = day.plusDays(1)
        }
        return points
    }

    private fun buildEmptyTrend(start: Instant, end: Instant): List<TrendPoint> =
        buildTrend(start, end, emptyMap(), ZoneId.systemDefault())
}



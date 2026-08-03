package com.sherif.ledger.core.domain.usecase.analytics

import com.sherif.ledger.core.domain.model.AccountBalanceSummary
import com.sherif.ledger.core.domain.model.CategoryTotal
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.FinancialAnalytics
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.MerchantTotal
import com.sherif.ledger.core.domain.model.NetWorthSnapshot
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionStory
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TrendPoint
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.service.transaction.FinancialStoryPresenter
import com.sherif.ledger.feature.merchant.GenericCategoryKeywords
import com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.relationship.FinancialRelationship
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
 * spending/income/category/merchant/trend/relationship aggregation.
 *
 * Phase 10: this is also the ONLY place [RelationshipEngine] and
 * [FinancialStoryPresenter] are invoked. No ViewModel imports either directly —
 * "the UI never computes relationships" (Phase 10 architecture rule) is enforced
 * by this being the sole call site, not by convention.
 *
 * Composition of already-built systems, not a new pipeline:
 *  - [TransactionRepository.observeTransactionsBetween] — the existing, indexed,
 *    date-bounded repository query. Reused as-is.
 *  - [RelationshipEngine.analyze] — run ONCE per call, over the bounded window
 *    (never the full history), to identify credit-card payments, cash
 *    withdrawals, internal transfers, and refund-of-purchase links. This is what
 *    tells this use case a transaction typed EXPENSE is not actually a purchase,
 *    and is also the source of every "Financial Story" and intelligence fact.
 *  - [MerchantResolver] (Merchant Intelligence) — resolves category and canonical
 *    merchant name for grouping.
 *  - [FinancialStoryPresenter] — formats a relationship into a human-readable
 *    explanation. Never reinterprets; only formats what RelationshipEngine already
 *    decided.
 *
 * Performance: one repository query, one RelationshipEngine pass, one linear scan
 * over the bounded transaction list. No per-transaction repository calls, no
 * quadratic scans over transaction history. [transactionStories] and
 * [computeMonthOverMonthChange] each necessarily add one additional bounded
 * pass/query when called, because they serve a genuinely different transaction
 * window than [compute] (a recent-activity slice, and a prior month respectively)
 * — not a duplicate of the same computation.
 */
class GetFinancialAnalyticsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val relationshipEngine: RelationshipEngine,
    private val merchantResolver: MerchantResolver,
    private val accountBalanceService: AccountBalanceService,
    private val storyPresenter: FinancialStoryPresenter,
    private val learnedMerchantCategoryStore: LearnedMerchantCategoryStore,
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
     *
     * RC7 Phase C: this previously violated its own doc comment above — it
     * recomputed assets-minus-liabilities itself (a second, independently
     * drifting copy of [AccountBalanceService.netWorth]'s arithmetic) instead
     * of calling it, and that duplicate summed raw minor units across every
     * account regardless of currency, a real cross-currency corruption bug of
     * the same shape [BalanceCalculator.effect] already guards per-transaction
     * (RC6). Now genuinely delegates — this is what the Dashboard displays,
     * so it is the most user-visible fix in Phase C.
     */
    suspend fun computeNetWorth(): NetWorthSnapshot {
        val balances = accountBalanceService.currentBalances()
        val netWorth = accountBalanceService.netWorth()
        // Split assets from debt, in the net-worth currency only — a balance in a
        // different currency is never added to a total denominated in this one.
        val inCurrency = balances.filter { it.balance.currencyCode == netWorth.currencyCode }
        val cashBalanceMinor = inCurrency
            .filterNot { it.account.type.isLiability }
            .sumOf { it.balance.minorUnits }
        // A liability account's balance is already positive-as-owed (see
        // BalanceCalculator.effect, which flips the sign for liability accounts).
        val cardDebtMinor = inCurrency
            .filter { it.account.type.isLiability }
            .sumOf { it.balance.minorUnits }
        return NetWorthSnapshot(
            netWorthMinor = netWorth.minorUnits,
            cashBalanceMinor = cashBalanceMinor,
            cardDebtMinor = cardDebtMinor,
            currency = netWorth.currencyCode,
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

    /**
     * The real explanation and category for each transaction in [transactions] —
     * "Refund processed" / "GROCERIES", not a presentation-layer guess from the
     * merchant string. This is the ONLY place [FinancialStoryPresenter] is called.
     * Runs its own [RelationshipEngine.analyze] pass because [transactions] here is
     * typically a different window than [compute]'s period (e.g. "recent 20"
     * across a screen's activity feed) — necessary additional work, not a
     * duplicate of the same computation over the same data.
     */
    fun transactionStories(transactions: List<Transaction>): Map<Long, TransactionStory> {
        if (transactions.isEmpty()) return emptyMap()
        val core = analyzeCore(transactions)
        return transactions.associate { t ->
            val explanation = storyPresenter.format(t, core.relationships)
            val resolution = merchantResolver.resolve(t.rawText)
            val category = (resolution as? MerchantResolution.Resolved)?.category?.name
                ?: learnedMerchantCategoryStore.categoryFor(t.rawText)?.name
                ?: GenericCategoryKeywords.classify(t.rawText)?.name
                ?: "UNKNOWN"
            t.id to TransactionStory(explanation, category)
        }
    }

    /**
     * Percentage change in net spend versus the prior calendar month, computed
     * from the SAME [compute] function this use case already uses — no new
     * financial concept introduced, just the existing period-scoped computation
     * run over a second, adjacent window. Returns null — never a fabricated
     * value — whenever a meaningful comparison isn't possible: no prior-period
     * spend to compare against, or a non-finite result.
     */
    suspend fun computeMonthOverMonthChange(currentNetSpendMinor: Long, currentPeriodStart: Instant): String? {
        val zone = ZoneId.systemDefault()
        val currentStartDate = currentPeriodStart.atZone(zone).toLocalDate().withDayOfMonth(1)
        val previousMonthDate = currentStartDate.minusMonths(1)
        val previousStart = previousMonthDate.atStartOfDay(zone).toInstant()
        val previousEnd = previousMonthDate.withDayOfMonth(previousMonthDate.lengthOfMonth())
            .atTime(23, 59, 59).atZone(zone).toInstant()

        val previousResult = transactionRepository.observeTransactionsBetween(previousStart, previousEnd).first()
        val previousTransactions = (previousResult as? LedgerResult.Success)?.data ?: emptyList()
        if (previousTransactions.isEmpty()) return null

        val previousAnalytics = compute(previousTransactions, previousStart, previousEnd)
        val previousSpend = previousAnalytics.netSpendMinor
        if (previousSpend <= 0L) return null

        val change = (currentNetSpendMinor - previousSpend) * 100.0 / previousSpend
        if (!change.isFinite()) return null
        val sign = if (change >= 0) "+" else ""
        return "$sign${change.toInt()}%"
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

        return AnalysisCore(excludedIds, refundedAgainstPurchase, internalTransfersTotal, relationships)
    }

    private data class AnalysisCore(
        val excludedIds: Set<Long>,
        val refundedAgainstPurchase: Map<Long, Long>,
        val internalTransfersTotal: Long,
        val relationships: List<FinancialRelationship>,
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
                intelligenceSummary = emptyList(),
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
                            val category = (resolution as? MerchantResolution.Resolved)?.category?.name
                ?: learnedMerchantCategoryStore.categoryFor(t.rawText)?.name
                ?: GenericCategoryKeywords.classify(t.rawText)?.name
                ?: "UNKNOWN"
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
            intelligenceSummary = buildIntelligenceSummary(core.relationships),
        )
    }

    /** Real, ordered facts about what was found this period. Only includes a line
     *  when the count backing it is greater than zero — never "0 subscriptions
     *  found" noise, and never a fabricated confidence figure. */
    private fun buildIntelligenceSummary(relationships: List<FinancialRelationship>): List<String> {
        if (relationships.isEmpty()) return emptyList()
        val summary = mutableListOf<String>()

        val subscriptions = relationships.count { it.type == RelationshipType.SUBSCRIPTION }
        if (subscriptions > 0) summary += "$subscriptions recurring subscription${if (subscriptions != 1) "s" else ""} identified"

        val bills = relationships.count { it.type == RelationshipType.RECURRING_BILL }
        if (bills > 0) summary += "$bills recurring bill${if (bills != 1) "s" else ""} identified"

        val refunds = relationships.count { it.type == RelationshipType.REFUND_OF_PURCHASE }
        if (refunds > 0) summary += "$refunds refund${if (refunds != 1) "s" else ""} matched automatically"

        val cardPayments = relationships.count { it.type == RelationshipType.CREDIT_CARD_PAYMENT }
        if (cardPayments > 0) summary += "$cardPayments credit card payment${if (cardPayments != 1) "s" else ""} matched"

        if (relationships.any { it.type == RelationshipType.SALARY_FUNDS_EXPENSE }) {
            summary += "Salary pattern confirmed"
        }

        summary.add(0, "${relationships.size} Financial ${if (relationships.size != 1) "Stories" else "Story"} matched")
        return summary
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


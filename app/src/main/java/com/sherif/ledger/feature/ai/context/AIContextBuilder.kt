package com.sherif.ledger.feature.ai.context

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.feature.ai.domain.DuplicateAnalysisContext
import com.sherif.ledger.feature.ai.domain.ForecastAnalysisContext
import com.sherif.ledger.feature.ai.domain.InsightAnalysisContext
import com.sherif.ledger.feature.ai.domain.MerchantAnalysisContext
import com.sherif.ledger.feature.ai.domain.RelationshipAnalysisContext
import com.sherif.ledger.feature.ai.domain.SearchAnalysisContext
import com.sherif.ledger.feature.ai.domain.TransactionSummary
import javax.inject.Inject

/**
 * RC5's "AI Context Builder" — the ONE place raw domain objects are reduced
 * to the structured, minimal shape a provider is allowed to see. Every
 * capability's context is built here, never inline at a call site — keeps
 * prompts consistent regardless of which capability or provider is asking,
 * and keeps the privacy boundary in one auditable place rather than
 * scattered across callers.
 */
class AIContextBuilder @Inject constructor() {

    fun merchant(
        rawMerchantText: String,
        amountMinor: Long,
        currencyCode: CurrencyCode,
        existingMerchantMatches: List<String>,
        knownCategories: List<String>,
    ): MerchantAnalysisContext = MerchantAnalysisContext(
        rawMerchantText = rawMerchantText,
        amountMinor = amountMinor,
        currencyCode = currencyCode.name,
        existingMerchantMatches = existingMerchantMatches,
        knownCategories = knownCategories,
    )

    fun duplicate(a: Transaction, b: Transaction): DuplicateAnalysisContext = DuplicateAnalysisContext(
        candidateA = a.toSummary(),
        candidateB = b.toSummary(),
    )

    fun relationship(transactions: List<Transaction>): RelationshipAnalysisContext =
        RelationshipAnalysisContext(transactions.map { it.toSummary() })

    fun insight(
        periodLabel: String,
        totalIncomeMinor: Long,
        totalExpenseMinor: Long,
        categoryTotals: Map<String, Long>,
        currencyCode: CurrencyCode,
    ): InsightAnalysisContext = InsightAnalysisContext(
        periodLabel = periodLabel,
        totalIncomeMinor = totalIncomeMinor,
        totalExpenseMinor = totalExpenseMinor,
        categoryTotals = categoryTotals,
        currencyCode = currencyCode.name,
    )

    fun forecast(historicalMonthlyTotalsMinor: List<Long>, currencyCode: CurrencyCode): ForecastAnalysisContext =
        ForecastAnalysisContext(historicalMonthlyTotalsMinor, currencyCode.name)

    fun search(query: String, recentTransactions: List<Transaction>): SearchAnalysisContext =
        SearchAnalysisContext(query, recentTransactions.map { it.toSummary() })

    // Deliberately excludes accountId, id, fingerprint, origin, note — a
    // provider has no use for them and Part "Privacy" says send only the
    // minimum required for the requested task.
    private fun Transaction.toSummary(): TransactionSummary = TransactionSummary(
        amountMinor = amount.minorUnits,
        currencyCode = amount.currencyCode.name,
        timestampEpochMillis = timestamp.toEpochMilli(),
        merchant = rawText,
        type = type.name,
    )
}

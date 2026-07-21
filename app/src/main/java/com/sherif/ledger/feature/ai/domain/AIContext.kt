package com.sherif.ledger.feature.ai.domain

/**
 * RC5 "AI Context Builder": every provider receives ONE of these, never a
 * raw domain object (Transaction, Account, etc.) — see AIContextBuilder,
 * the single place these are assembled. Keeps prompts consistent across
 * providers and keeps privacy scoped: each context type carries only the
 * fields its capability actually needs (merchant classification never sees
 * account balances; duplicate detection never sees the user's full history).
 */
sealed interface AIContext

/** A transaction reduced to the minimum a provider might reasonably need — never the full domain model. */
data class TransactionSummary(
    val amountMinor: Long,
    val currencyCode: String,
    val timestampEpochMillis: Long,
    val merchant: String?,
    val type: String,
)

data class MerchantAnalysisContext(
    val rawMerchantText: String,
    val amountMinor: Long,
    val currencyCode: String,
    val existingMerchantMatches: List<String>,
    val knownCategories: List<String>,
) : AIContext

data class DuplicateAnalysisContext(
    val candidateA: TransactionSummary,
    val candidateB: TransactionSummary,
) : AIContext

data class RelationshipAnalysisContext(
    val transactions: List<TransactionSummary>,
) : AIContext

data class InsightAnalysisContext(
    val periodLabel: String,
    val totalIncomeMinor: Long,
    val totalExpenseMinor: Long,
    val categoryTotals: Map<String, Long>,
    val currencyCode: String,
) : AIContext

data class ForecastAnalysisContext(
    val historicalMonthlyTotalsMinor: List<Long>,
    val currencyCode: String,
) : AIContext

data class SearchAnalysisContext(
    val query: String,
    val recentTransactions: List<TransactionSummary>,
) : AIContext

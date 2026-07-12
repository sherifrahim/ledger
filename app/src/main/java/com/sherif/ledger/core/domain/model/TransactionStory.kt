package com.sherif.ledger.core.domain.model

/**
 * A transaction's real-world explanation and category, derived entirely from
 * backend analysis (RelationshipEngine + Merchant Intelligence) — never from
 * presentation-layer heuristics. Produced by
 * [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase.transactionStories],
 * the single source every screen consumes for "what is this transaction, and why."
 *
 * [category] is the domain [com.sherif.ledger.feature.merchant.MerchantCategory]
 * name (e.g. "GROCERIES"). Presentation may map this to whatever icon/enum a
 * specific screen renders with — that is formatting, not reinterpretation.
 */
data class TransactionStory(
    val explanation: String,
    val category: String,
)


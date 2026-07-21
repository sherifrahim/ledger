package com.sherif.ledger.feature.ai.domain

/**
 * RC5 Part 7 — capabilities, not providers, are the unit of design. Every
 * capability is independently pluggable and independently assignable to a
 * provider (see CapabilityRegistry) — adding a new one is a new enum entry
 * plus a new PromptLibrary/AIContextBuilder case, never a change to the
 * orchestrator or any provider.
 */
enum class AICapability(val displayName: String) {
    MERCHANT_CLASSIFICATION("Merchant Classification"),
    DUPLICATE_DETECTION("Duplicate Detection"),
    TRANSFER_DETECTION("Transfer Detection"),
    RELATIONSHIP_DETECTION("Relationship Detection"),
    FINANCIAL_INSIGHTS("Financial Insights"),
    FORECASTING("Forecasting"),
    NATURAL_LANGUAGE_SEARCH("Natural Language Search"),
}

package com.sherif.ledger.feature.relationship

/**
 * Financial Relationship & Narrative Engine — deterministic, read-only reasoning
 * over ALREADY PERSISTED transactions.
 *
 * This layer answers "how are these transactions related?" It NEVER extracts,
 * reconciles, modifies, inserts, or re-identifies a transaction. It consumes a
 * List<Transaction> and produces a List<FinancialRelationship> as a pure function.
 */

/**
 * The kind of relationship between transactions. Adding a new type here plus a
 * new [RelationshipResolver] is the only change needed to extend the engine;
 * existing resolvers are untouched (open/closed).
 */
enum class RelationshipType {
    SALARY_FUNDS_EXPENSE,
    TRANSFER_BETWEEN_ACCOUNTS,
    SAVINGS_MOVEMENT,
    CREDIT_CARD_PAYMENT,
    CONFIRMATION_OF_PAYMENT,
    REFUND_OF_PURCHASE,
    RECURRING_MERCHANT,
    RECURRING_BILL,
    LOAN_REPAYMENT,
    INTEREST_CREDIT,
    CASH_WITHDRAWAL,
    SUBSCRIPTION,
    INSTALLMENT_PAYMENT,
    INVESTMENT_CONTRIBUTION,
}

/**
 * Confidence in a relationship. Each relationship computes its OWN confidence
 * (never global). Exposed as a band plus a numeric backing so the benchmark can
 * calibrate.
 */
@JvmInline
value class RelationshipConfidence(val value: Int) {
    init {
        require(value in 0..100) { "Confidence must be 0..100, was $value" }
    }

    val band: Band
        get() = when {
            value >= 80 -> Band.HIGH
            value >= 55 -> Band.MEDIUM
            else -> Band.LOW
        }

    enum class Band { HIGH, MEDIUM, LOW }

    companion object {
        fun high(v: Int = 90) = RelationshipConfidence(v)
        fun medium(v: Int = 65) = RelationshipConfidence(v)
        fun low(v: Int = 40) = RelationshipConfidence(v)
    }
}

/**
 * Developer-mode-only observability for a relationship. Never affects business
 * logic. Captures the evidence that produced the relationship.
 */
data class RelationshipDiagnostics(
    val relationshipType: String,
    val confidence: Int,
    val reasoning: List<String>,
    val matchedTransactionIds: List<Long>,
    val timeDifferenceSeconds: Long?,
    val amountDifferenceMinor: Long?,
    val merchantMatch: Boolean,
    val cardMatch: Boolean,
    val accountMatch: Boolean,
    val decision: String,
)

/**
 * A relationship between transactions. A SEPARATE object — neither transaction is
 * modified. [targetTransactionId] is nullable so single-transaction
 * classifications (recurring merchant, cash withdrawal, interest) are
 * representable without inventing a pair.
 */
data class FinancialRelationship(
    val relationshipId: String,
    val type: RelationshipType,
    val sourceTransactionId: Long,
    val targetTransactionId: Long?,
    val confidence: RelationshipConfidence,
    val reasoning: List<String>,
    val createdByEngineVersion: Int,
    val diagnostics: RelationshipDiagnostics,
)


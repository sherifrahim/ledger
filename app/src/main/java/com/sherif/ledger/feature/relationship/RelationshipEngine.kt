package com.sherif.ledger.feature.relationship

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.feature.merchant.MerchantRegistry
import com.sherif.ledger.feature.merchant.MerchantResolver
import javax.inject.Inject

/**
 * The Financial Relationship & Narrative Engine.
 *
 * PURE FUNCTION: consumes a List<Transaction> and produces a
 * List<FinancialRelationship>. No persistence, no mutation, no insertion, no
 * re-identification. Deterministic — the same input always yields the same output.
 *
 * Versioning: [engineVersion] is stamped onto every relationship. A future v2 can
 * change the algorithm (add resolvers, retune confidence) without changing this
 * public contract, exactly like Extraction v1 -> v2.
 *
 * Extensibility: [resolvers] is the single registration point. A new relationship
 * type = a new [RelationshipResolver] added to this list. No existing resolver or
 * engine logic changes (open/closed).
 *
 * Merchant reasoning is delegated to the frozen [MerchantResolver], used strictly
 * read-only. The engine never normalizes merchants itself.
 */
class RelationshipEngine @Inject constructor(
    private val merchantResolver: MerchantResolver,
) {
    val engineVersion: Int = ENGINE_VERSION

    private val resolvers: List<RelationshipResolver> = listOf(
        RefundOfPurchaseResolver(),
        CreditCardPaymentResolver(),
        TransferBetweenAccountsResolver(),
        SalaryFundsExpenseResolver(),
        RecurringMerchantResolver(),
        RecurringBillResolver(),
        CashWithdrawalResolver(),
        InterestCreditResolver(),
        LoanRepaymentResolver(),
    )

    fun analyze(transactions: List<Transaction>): List<FinancialRelationship> {
        if (transactions.isEmpty()) return emptyList()
        val sorted = transactions.sortedBy { it.timestamp }
        val context = RelationshipContext(sorted, merchantResolver, engineVersion)
        return resolvers.flatMap { it.resolve(context) }
    }

    companion object {
        const val ENGINE_VERSION = 1

        /** Convenience factory for tests / non-DI callers. */
        fun default(): RelationshipEngine =
            RelationshipEngine(MerchantResolver(MerchantRegistry()))
    }
}


package com.sherif.ledger.feature.transactions.presentation

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.relationship.FinancialRelationship
import com.sherif.ledger.feature.relationship.RelationshipType
import javax.inject.Inject

/**
 * Responsible for formatting Financial Stories (relationships) into human-readable 
 * narrative for the presentation layer. 
 * 
 * Ensures there is only one source of financial truth (the RelationshipEngine)
 * while providing the product-level "explanation" for every transaction.
 */
class FinancialStoryPresenter @Inject constructor() {

    fun format(transaction: Transaction, relationships: List<FinancialRelationship>): String {
        val rel = relationships.find { it.sourceTransactionId == transaction.id || it.targetTransactionId == transaction.id }
        
        return when (rel?.type) {
            RelationshipType.SALARY_FUNDS_EXPENSE -> "Salary received"
            RelationshipType.TRANSFER_BETWEEN_ACCOUNTS -> "Internal transfer"
            RelationshipType.SAVINGS_MOVEMENT -> "Savings allocation"
            RelationshipType.CREDIT_CARD_PAYMENT -> "Credit card payment"
            RelationshipType.CONFIRMATION_OF_PAYMENT -> "Payment confirmed"
            RelationshipType.REFUND_OF_PURCHASE -> "Refund processed"
            RelationshipType.RECURRING_MERCHANT -> "Recurring expense"
            RelationshipType.RECURRING_BILL -> "Monthly bill"
            RelationshipType.LOAN_REPAYMENT -> "Loan repayment"
            RelationshipType.INTEREST_CREDIT -> "Interest earned"
            RelationshipType.CASH_WITHDRAWAL -> "Cash withdrawal"
            RelationshipType.SUBSCRIPTION -> "Recurring subscription"
            RelationshipType.INSTALLMENT_PAYMENT -> "Installment payment"
            RelationshipType.INVESTMENT_CONTRIBUTION -> "Investment contribution"
            else -> defaultNarrative(transaction)
        }
    }

    private fun defaultNarrative(transaction: Transaction): String {
        return when (transaction.type) {
            TransactionType.INCOME -> "Income"
            TransactionType.EXPENSE -> "Expense"
            TransactionType.REFUND -> "Refund"
            TransactionType.TRANSFER -> "Transfer"
        }
    }
}

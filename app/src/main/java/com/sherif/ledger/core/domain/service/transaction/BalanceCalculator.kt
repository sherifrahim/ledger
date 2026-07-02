package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import javax.inject.Inject

/**
 * Service responsible for calculating new account balances based on transaction effects.
 */
class BalanceCalculator @Inject constructor() {

    fun calculate(currentBalance: Money, transaction: Transaction): Money {
        return when (transaction.type) {
            TransactionType.INCOME -> currentBalance + transaction.amount
            TransactionType.EXPENSE -> currentBalance - transaction.amount
            TransactionType.REFUND -> currentBalance + transaction.amount
            TransactionType.TRANSFER -> currentBalance // Internal transfer logic handled at orchestrator level if needed
        }
    }
}

package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import javax.inject.Inject

/**
 * Service responsible for validating transaction input data and business constraints.
 */
class TransactionValidator @Inject constructor() {

    fun validateInput(params: InsertTransactionUseCase.Params): LedgerError? {
        if (params.amountMinor <= 0) {
            return LedgerError.Unknown("Amount must be greater than zero")
        }
        if (params.rawMerchantText.isBlank()) {
            return LedgerError.Unknown("Merchant text cannot be blank")
        }
        return null
    }

    fun validateAccount(account: Account, params: InsertTransactionUseCase.Params): LedgerError? {
        if (account.openingBalance.currencyCode != params.currencyCode) {
            return LedgerError.InvalidCurrency
        }
        return null
    }
}


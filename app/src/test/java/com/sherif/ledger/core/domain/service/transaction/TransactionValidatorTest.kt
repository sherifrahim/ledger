package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerError
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class TransactionValidatorTest {

    private val validator = TransactionValidator()

    @Test
    fun `validateInput returns error for zero amount`() {
        val params = createParams(amount = 0L)
        val error = validator.validateInput(params)
        assertEquals(LedgerError.Unknown("Amount must be greater than zero"), error)
    }

    @Test
    fun `validateAccount returns error for currency mismatch`() {
        val account = Account(1L, "Test", AccountType.CHECKING, Money(0L, CurrencyCode.INR), null, null)
        val params = createParams(currency = CurrencyCode.AED)
        
        val error = validator.validateAccount(account, params)
        assertEquals(LedgerError.InvalidCurrency, error)
    }

    @Test
    fun `valid data returns null error`() {
        val params = createParams(amount = 100L)
        assertNull(validator.validateInput(params))
    }

    private fun createParams(amount: Long = 1000L, currency: CurrencyCode = CurrencyCode.AED) = 
        InsertTransactionUseCase.Params(
            accountId = 1L,
            amountMinor = amount,
            currencyCode = currency,
            type = TransactionType.EXPENSE,
            timestamp = Instant.now(),
            source = IngestionSource.MANUAL,
            rawMerchantText = "Amazon"
        )
}

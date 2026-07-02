package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BalanceCalculatorTest {

    private val calculator = BalanceCalculator()

    @Test
    fun `expense subtracts from balance`() {
        val current = Money(5000L, CurrencyCode.AED)
        val transaction = createTransaction(1000L, TransactionType.EXPENSE)
        
        val result = calculator.calculate(current, transaction)
        
        assertEquals(4000L, result.minorUnits)
    }

    @Test
    fun `income adds to balance`() {
        val current = Money(5000L, CurrencyCode.AED)
        val transaction = createTransaction(1000L, TransactionType.INCOME)
        
        val result = calculator.calculate(current, transaction)
        
        assertEquals(6000L, result.minorUnits)
    }

    private fun createTransaction(amount: Long, type: TransactionType) = Transaction(
        id = 1,
        accountId = 1,
        brandId = null,
        categoryId = null,
        amount = Money(amount, CurrencyCode.AED),
        type = type,
        timestamp = Instant.now(),
        source = IngestionSource.MANUAL,
        rawText = null,
        fingerprint = "f"
    )
}

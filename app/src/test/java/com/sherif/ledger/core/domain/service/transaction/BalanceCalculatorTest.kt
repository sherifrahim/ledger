package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
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

    // ---- Phase 8 regressions: BalanceCalculator consumes ONLY the normalized
    //      transferDirection field, never raw text. ----

    @Test
    fun `refund adds back to balance`() {
        val current = Money(5000L, CurrencyCode.AED)
        val transaction = createTransaction(1000L, TransactionType.REFUND)
        assertEquals(6000L, calculator.calculate(current, transaction).minorUnits)
    }

    @Test
    fun `outgoing transfer decreases balance`() {
        val current = Money(10000L, CurrencyCode.AED)
        val transaction = createTransaction(3000L, TransactionType.TRANSFER, TransferDirection.OUTGOING)
        assertEquals(7000L, calculator.calculate(current, transaction).minorUnits)
    }

    @Test
    fun `incoming transfer increases balance`() {
        val current = Money(10000L, CurrencyCode.AED)
        val transaction = createTransaction(3000L, TransactionType.TRANSFER, TransferDirection.INCOMING)
        assertEquals(13000L, calculator.calculate(current, transaction).minorUnits)
    }

    @Test
    fun `transfer with no direction leaves balance unchanged rather than guessing`() {
        // If extraction failed to normalize direction, BalanceCalculator must NOT
        // infer it from text — it leaves the balance untouched and the gap is
        // surfaced via logging, never silently guessed.
        val current = Money(10000L, CurrencyCode.AED)
        val transaction = createTransaction(3000L, TransactionType.TRANSFER, direction = null)
        assertEquals(10000L, calculator.calculate(current, transaction).minorUnits)
    }

    @Test
    fun `credit card payment (outgoing transfer) reduces the paying account balance`() {
        // A credit-card payment is typed TRANSFER with OUTGOING direction by
        // extraction (see HeuristicExtractor.inferDirection). Balance correctly
        // decreases here even though this is excluded from "spending" in analytics
        // — those are different, orthogonal questions.
        val current = Money(795536L, CurrencyCode.AED)
        val transaction = createTransaction(20000L, TransactionType.TRANSFER, TransferDirection.OUTGOING)
        assertEquals(775536L, calculator.calculate(current, transaction).minorUnits)
    }

    private fun createTransaction(
        amount: Long,
        type: TransactionType,
        direction: TransferDirection? = null,
    ) = Transaction(
        id = 1,
        accountId = 1,
        brandId = null,
        categoryId = null,
        amount = Money(amount, CurrencyCode.AED),
        type = type,
        timestamp = Instant.now(),
        source = IngestionSource.MANUAL,
        rawText = null,
        fingerprint = "f",
        transferDirection = direction,
    )
}



package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Phase 9 API: [BalanceCalculator.effect] returns the signed delta a transaction
 * contributes to an account's balance, given that account's [AccountType] — not a
 * new balance computed from a supplied current balance. Account-type-aware: an
 * asset account (CHECKING/SAVINGS/CASH/INVESTMENT) behaves as before; a liability
 * account (CREDIT) inverts the natural effect, since its balance means amount
 * owed, not amount held.
 */
class BalanceCalculatorTest {

    private val calculator = BalanceCalculator()

    // ---- Asset accounts: unchanged intent from Phase 8, expressed as a delta ----

    @Test
    fun `expense decreases an asset account`() {
        val transaction = createTransaction(1000L, TransactionType.EXPENSE)
        assertEquals(-1000L, calculator.effect(transaction, AccountType.CHECKING))
    }

    @Test
    fun `income increases an asset account`() {
        val transaction = createTransaction(1000L, TransactionType.INCOME)
        assertEquals(1000L, calculator.effect(transaction, AccountType.CHECKING))
    }

    @Test
    fun `refund increases an asset account`() {
        val transaction = createTransaction(1000L, TransactionType.REFUND)
        assertEquals(1000L, calculator.effect(transaction, AccountType.CHECKING))
    }

    @Test
    fun `outgoing transfer decreases an asset account`() {
        val transaction = createTransaction(3000L, TransactionType.TRANSFER, TransferDirection.OUTGOING)
        assertEquals(-3000L, calculator.effect(transaction, AccountType.CHECKING))
    }

    @Test
    fun `incoming transfer increases an asset account`() {
        val transaction = createTransaction(3000L, TransactionType.TRANSFER, TransferDirection.INCOMING)
        assertEquals(3000L, calculator.effect(transaction, AccountType.CHECKING))
    }

    @Test
    fun `transfer with no direction contributes zero effect rather than guessing`() {
        // If extraction failed to normalize direction, BalanceCalculator must NOT
        // infer it from text — it contributes nothing and the gap is surfaced via
        // logging, never silently guessed.
        val transaction = createTransaction(3000L, TransactionType.TRANSFER, direction = null)
        assertEquals(0L, calculator.effect(transaction, AccountType.CHECKING))
    }

    // ---- Currency safety (found via a real diagnostic bundle: an INR
    //      transaction logged against an AED account) — accountCurrencyCode is
    //      optional so existing sign/type-only tests above are unaffected. ----

    @Test
    fun `mismatched transaction currency contributes zero effect rather than mixing units`() {
        val transaction = createTransaction(1000L, TransactionType.EXPENSE, currency = CurrencyCode.INR)
        assertEquals(0L, calculator.effect(transaction, AccountType.CHECKING, accountCurrencyCode = CurrencyCode.AED))
    }

    @Test
    fun `matching transaction currency behaves exactly as when no currency is supplied`() {
        val transaction = createTransaction(1000L, TransactionType.EXPENSE, currency = CurrencyCode.AED)
        assertEquals(-1000L, calculator.effect(transaction, AccountType.CHECKING, accountCurrencyCode = CurrencyCode.AED))
    }

    // ---- Liability accounts (Phase 9): the natural effect inverts, since balance
    //      here means amount owed, not amount held. ----

    @Test
    fun `expense (a purchase) increases what a liability account owes`() {
        val transaction = createTransaction(5000L, TransactionType.EXPENSE)
        assertEquals(5000L, calculator.effect(transaction, AccountType.CREDIT))
    }

    @Test
    fun `refund decreases what a liability account owes`() {
        val transaction = createTransaction(1000L, TransactionType.REFUND)
        assertEquals(-1000L, calculator.effect(transaction, AccountType.CREDIT))
    }

    @Test
    fun `income decreases what a liability account owes`() {
        // Rare in practice (a credit account rarely receives INCOME-typed
        // transactions directly), but the inversion rule must hold symmetrically
        // for every transaction type, not just the common ones.
        val transaction = createTransaction(500L, TransactionType.INCOME)
        assertEquals(-500L, calculator.effect(transaction, AccountType.CREDIT))
    }

    @Test
    fun `outgoing transfer increases what a liability account owes`() {
        val transaction = createTransaction(2000L, TransactionType.TRANSFER, TransferDirection.OUTGOING)
        assertEquals(2000L, calculator.effect(transaction, AccountType.CREDIT))
    }

    // ---- liabilityPaymentEffect: the dual-effect primitive, always a reduction ----

    @Test
    fun `liabilityPaymentEffect always reduces what's owed, regardless of magnitude`() {
        assertEquals(-20000L, calculator.liabilityPaymentEffect(Money(20000L, CurrencyCode.AED)))
        assertEquals(-100L, calculator.liabilityPaymentEffect(Money(100L, CurrencyCode.AED)))
    }

    private fun createTransaction(
        amount: Long,
        type: TransactionType,
        direction: TransferDirection? = null,
        currency: CurrencyCode = CurrencyCode.AED,
    ) = Transaction(
        id = 1,
        accountId = 1,
        brandId = null,
        categoryId = null,
        amount = Money(amount, currency),
        type = type,
        timestamp = Instant.now(),
        source = IngestionSource.MANUAL,
        rawText = null,
        fingerprint = "f",
        transferDirection = direction,
    )
}


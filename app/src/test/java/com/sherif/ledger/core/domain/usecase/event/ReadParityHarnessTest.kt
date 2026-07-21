package com.sherif.ledger.core.domain.usecase.event

import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.core.domain.service.transaction.BalanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReadParityHarnessTest {

    @Test
    fun `parity report separates intentional from unexpected differences`() {
        val proven = ParityReport(
            listOf(
                FeatureParity("A", "1", "1", match = true),
                FeatureParity("B", "2", "3", match = false, classification = "known event gap"),
            ),
        )
        assertEquals(2, proven.total)
        assertEquals(1, proven.passed)
        assertEquals(1, proven.failed)
        assertEquals(1, proven.intentionalDifferences)
        assertEquals(0, proven.unexpectedDifferences)
        assertTrue(proven.proven) // an explained difference does not fail parity

        val broken = ParityReport(
            listOf(FeatureParity("C", "4", "5", match = false)), // no classification
        )
        assertEquals(1, broken.unexpectedDifferences)
        assertFalse(broken.proven)
    }

    // The ONE field a read depends on that the FinancialEvent mirror omits.
    @Test
    fun `dropping transferDirection changes balance only for TRANSFER items`() {
        val calc = BalanceCalculator()
        fun txn(type: TransactionType, dir: TransferDirection?) = Transaction(
            id = 1, accountId = 1, brandId = null, categoryId = null,
            amount = Money(1000, CurrencyCode.AED), type = type,
            timestamp = Instant.EPOCH, source = IngestionSource.SMS,
            rawText = "x", fingerprint = "fp", transferDirection = dir,
        )

        // A transfer's effect flips to 0 when direction is lost — the event-derived path.
        val transferWithDir = txn(TransactionType.TRANSFER, TransferDirection.OUTGOING)
        val transferNoDir = txn(TransactionType.TRANSFER, null)
        assertNotEquals(
            calc.effect(transferWithDir, AccountType.CHECKING, CurrencyCode.AED),
            calc.effect(transferNoDir, AccountType.CHECKING, CurrencyCode.AED),
        )

        // Every non-transfer type is unaffected — so balance parity holds for them.
        for (type in listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.REFUND)) {
            assertEquals(
                calc.effect(txn(type, TransferDirection.OUTGOING), AccountType.CHECKING, CurrencyCode.AED),
                calc.effect(txn(type, null), AccountType.CHECKING, CurrencyCode.AED),
            )
        }
    }
}

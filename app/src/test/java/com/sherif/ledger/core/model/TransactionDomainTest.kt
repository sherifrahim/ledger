package com.sherif.ledger.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for immutable transaction domain models and typed transaction helpers.
 */
class TransactionDomainTest {
    @Test
    fun ledgerTransaction_storesImmutableDomainFacts() {
        val createdAt = Instant.parse("2026-06-26T10:15:30Z")
        val transaction = LedgerTransaction(
            id = TransactionId("txn_1"),
            amount = Money(2_500L, Currency.AED),
            type = TransactionType.Expense,
            occurredOn = LocalDate.of(2026, 6, 26),
            paymentMethod = PaymentMethod.CreditCard,
            source = TransactionSource.Manual,
            reviewStatus = ReviewStatus.Approved,
            category = Category(CategoryId("food"), CategoryName("Food")),
            merchant = Merchant(MerchantId("merchant_1"), MerchantName("Cafe")),
            tags = TransactionTags.of(
                listOf(
                    TransactionTag.of("Lunch"),
                    TransactionTag.of("lunch"),
                ),
            ),
            metadata = LedgerTransaction.manualMetadata(createdAt),
        )

        assertEquals(TransactionId("txn_1"), transaction.id)
        assertEquals(Money(2_500L, Currency.AED), transaction.amount)
        assertEquals(1, transaction.tags.values.size)
        assertEquals(createdAt, transaction.metadata.createdAt)
    }

    @Test
    fun ledgerTransaction_rejectsZeroAmount() {
        val createdAt = Instant.parse("2026-06-26T10:15:30Z")

        assertThrows(IllegalArgumentException::class.java) {
            LedgerTransaction(
                id = TransactionId("txn_1"),
                amount = Money.zero(Currency.AED),
                type = TransactionType.Expense,
                occurredOn = LocalDate.of(2026, 6, 26),
                paymentMethod = PaymentMethod.Cash,
                source = TransactionSource.Manual,
                reviewStatus = ReviewStatus.Approved,
                metadata = LedgerTransaction.manualMetadata(createdAt),
            )
        }
    }

    @Test
    fun transactionTags_removeDuplicatesAndRemainImmutable() {
        val original = TransactionTags.of(
            listOf(
                TransactionTag.of("Groceries"),
                TransactionTag.of(" groceries "),
                TransactionTag.of("Weekend"),
            ),
        )
        val groceries = TransactionTag.of("groceries")
        val fuel = TransactionTag.of("fuel")

        val updated = original.plus(fuel)

        assertEquals(2, original.values.size)
        assertTrue(groceries in original)
        assertFalse(fuel in original)
        assertTrue(fuel in updated)
    }

    @Test
    fun confidence_usesBasisPointsAndValidatesRange() {
        val confidence = Confidence.fromPercent(87)

        assertEquals(8_700, confidence.basisPoints)
        assertEquals(87, confidence.percent())
        assertThrows(IllegalArgumentException::class.java) {
            Confidence.fromBasisPoints(10_001)
        }
    }

    @Test
    fun transactionMetadata_rejectsUpdatedAtBeforeCreatedAt() {
        assertThrows(IllegalArgumentException::class.java) {
            TransactionMetadata(
                createdAt = Instant.parse("2026-06-26T10:15:30Z"),
                updatedAt = Instant.parse("2026-06-26T10:15:29Z"),
            )
        }
    }

    @Test
    fun parserMetadata_storesPassiveParserFacts() {
        val parsedAt = Instant.parse("2026-06-26T10:15:30Z")
        val metadata = ParserMetadata(
            source = TransactionSource.Sms,
            parserName = "BankSmsParser",
            parsedAt = parsedAt,
            confidence = Confidence.fromBasisPoints(9_250),
            rawText = "AED 25.00 spent",
            externalReference = "sms_1",
        )

        assertEquals(TransactionSource.Sms, metadata.source)
        assertEquals("BankSmsParser", metadata.parserName)
        assertEquals(parsedAt, metadata.parsedAt)
        assertEquals(9_250, metadata.confidence.basisPoints)
        assertEquals("AED 25.00 spent", metadata.rawText)
        assertEquals("sms_1", metadata.externalReference)
    }
}

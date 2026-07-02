package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ParserRegistryTest {

    @Test
    fun `parse returns Failure if no matching parser registered`() {
        val registry = ParserRegistry(emptySet())
        val envelope = createEnvelope(packageName = "com.test.bank")
        val result = registry.parse(envelope)
        assertTrue(result is ParseResult.Failed)
    }

    @Test
    fun `parse delegates to matching parser`() {
        val bankPackage = "com.test.bank"
        val mockParser = object : BankParser {
            override fun supports(envelope: NotificationEnvelope): Boolean = envelope.packageName == bankPackage
            override fun parse(envelope: NotificationEnvelope): ParseResult {
                return ParseResult.Success(
                    TransactionCandidate(
                        source = IngestionSource.SMS,
                        rawText = envelope.text,
                        merchantName = "Test Merchant",
                        amountMinor = 1000,
                        currencyCode = null,
                        timestamp = envelope.timestamp,
                        accountHint = null,
                        transactionType = null
                    )
                )
            }
        }

        val registry = ParserRegistry(setOf(mockParser))
        val envelope = createEnvelope(packageName = bankPackage)
        val result = registry.parse(envelope)

        assertTrue(result is ParseResult.Success)
    }

    private fun createEnvelope(packageName: String) = NotificationEnvelope(
        packageName = packageName,
        title = "Title",
        text = "Text",
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "key"
    )
}

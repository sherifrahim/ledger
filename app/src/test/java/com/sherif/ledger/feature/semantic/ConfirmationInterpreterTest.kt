package com.sherif.ledger.feature.semantic

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.capture.extraction.ConfirmationMatcher
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ConfirmationInterpreterTest {

    private fun txn() = Transaction(
        id = 7,
        accountId = 1,
        brandId = null,
        categoryId = null,
        amount = Money(20000, CurrencyCode.AED),
        type = TransactionType.EXPENSE,
        timestamp = Instant.now(),
        source = IngestionSource.NOTIFICATION,
        rawText = "raw",
        cardTail = "6989",
        fingerprint = "fp",
    )

    @Test fun `high confidence match is a confirmed match`() {
        val outcome = ConfirmationInterpreter.interpret(
            ConfirmationMatcher.MatchResult.Matched(txn(), confidence = 97),
        )
        assertTrue(outcome is ConfirmationOutcome.ConfirmedMatch)
    }

    @Test fun `borderline confidence match is a likely match`() {
        val outcome = ConfirmationInterpreter.interpret(
            ConfirmationMatcher.MatchResult.Matched(txn(), confidence = 70),
        )
        assertTrue(outcome is ConfirmationOutcome.LikelyMatch)
    }

    @Test fun `no match is unmatched and never invents a transaction`() {
        val outcome = ConfirmationInterpreter.interpret(
            ConfirmationMatcher.MatchResult.Unmatched("no nearby transaction"),
        )
        assertTrue(outcome is ConfirmationOutcome.Unmatched)
    }
}


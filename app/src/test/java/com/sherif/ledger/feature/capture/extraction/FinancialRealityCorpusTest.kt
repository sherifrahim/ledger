package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.GenericBankParser
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * Phase 8 — Financial Reality Corpus (Part 4). Runs REAL notification text through
 * the REAL extraction registry (no fakes) and asserts the type AND transfer
 * direction the registry actually selects — i.e. what BalanceCalculator and
 * analytics will actually see. Every scenario here was verified against the exact
 * committed vocabulary before being written, including which extractor wins the
 * registry's confidence ranking for each message shape.
 */
class FinancialRealityCorpusTest {

    private fun registry(): ExtractionRegistry {
        val textNormalizer = TextNormalizer()
        val merchantNormalizer = MerchantNormalizer()
        val parserRegistry = ParserRegistry(setOf(GenericBankParser(textNormalizer, merchantNormalizer)))
        return ExtractionRegistry(
            setOf(
                KnownBankExtractor(parserRegistry),
                HeuristicExtractor(textNormalizer, merchantNormalizer, FinancialPhraseLibrary()),
            ),
            ExtractionValidator(),
        )
    }

    private fun envelope(text: String) = NotificationEnvelope(
        packageName = "com.bank.app",
        title = "",
        text = text,
        subText = null,
        timestamp = Instant.now(),
        notificationKey = "k",
    )

    private fun corpusText(): String {
        val stream = FinancialRealityCorpusTest::class.java.classLoader
            ?.getResourceAsStream("financial-reality-corpus/scenarios.json")
        if (stream != null) return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val candidates = listOf(
            File("src/test/resources/financial-reality-corpus/scenarios.json"),
            File("app/src/test/resources/financial-reality-corpus/scenarios.json"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("financial-reality-corpus/scenarios.json not found; looked in $candidates")
        return file.readText()
    }

    @Test
    fun `every scenario extracts with the expected type and transfer direction`() = runBlocking {
        val registry = registry()
        val fixtures = Json { ignoreUnknownKeys = true }.parseToJsonElement(corpusText()).jsonArray

        val failures = mutableListOf<String>()
        fixtures.forEach { el ->
            val o = el.jsonObject
            val name = o["name"]!!.jsonPrimitive.content
            val text = o["text"]!!.jsonPrimitive.content
            val expectedType = TransactionType.valueOf(o["expectedType"]!!.jsonPrimitive.content)
            val expectedDirection = o["expectedDirection"]?.jsonPrimitive?.contentOrNull
                ?.let { TransferDirection.valueOf(it) }

            val outcome = registry.extract(envelope(text))
            if (outcome !is ExtractionRegistry.ExtractionOutcome.Success) {
                failures += "[$name] expected Success, got ${outcome::class.simpleName}"
                return@forEach
            }
            val candidate = outcome.candidate
            if (candidate.transactionType != expectedType) {
                failures += "[$name] type: expected $expectedType, got ${candidate.transactionType}"
            }
            if (candidate.transferDirection != expectedDirection) {
                failures += "[$name] direction: expected $expectedDirection, got ${candidate.transferDirection}"
            }
        }
        assertTrue("Corpus mismatches:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `corpus is non-trivial`() {
        val fixtures = Json { ignoreUnknownKeys = true }.parseToJsonElement(corpusText()).jsonArray
        assertTrue(fixtures.size >= 8)
    }
}



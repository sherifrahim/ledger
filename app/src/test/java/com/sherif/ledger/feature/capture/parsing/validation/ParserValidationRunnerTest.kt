package com.sherif.ledger.feature.capture.parsing.validation

import com.sherif.ledger.feature.capture.parsing.AdcbParser
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.PatternEngine
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserValidationRunnerTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val registry = ParserRegistry(setOf(
        AdcbParser(PatternEngine(TextNormalizer()), MerchantNormalizer())
    ))
    private val runner = ParserValidationRunner(registry)

    @Test
    fun `validate runs against adcb fixtures`() {
        val fixturesJson = javaClass.classLoader!!.getResourceAsStream("fixtures/adcb.json")!!.bufferedReader().readText()
        val fixtures = json.decodeFromString<List<ParserFixture>>(fixturesJson)

        val results = runner.validate(fixtures)
        
        assertEquals(fixtures.size, results.size)
        results.forEach { result ->
            assertTrue("Fixture ${result.fixtureId} failed: ${result.errors}", result.isSuccess)
        }

        val stats = runner.calculateStats(results)
        val adcbStats = stats.find { it.bank == "ADCB" }!!
        
        assertEquals(3, adcbStats.total)
        assertEquals(3, adcbStats.success) 
        assertEquals(1, adcbStats.ignored)
        assertEquals(0, adcbStats.failed)
    }
}

package com.sherif.ledger.regression

import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.feature.capture.parsing.AdcbParser
import com.sherif.ledger.feature.capture.parsing.GenericBankParser
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.PatternEngine
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import com.sherif.ledger.feature.merchant.MerchantRegistry
import com.sherif.ledger.feature.merchant.MerchantResolver
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RC9 Phase D: proves the regression corpus infrastructure actually runs
 * end to end against real (not faked) parsing/institution/merchant/category
 * code. Deliberately a SMALL number of fixtures (2 files, 2-3 entries) — this
 * is infrastructure validation, not an attempt at corpus coverage; see
 * `PipelineCorpusRunner`'s doc comment for what this harness can and can't
 * check yet.
 */
class PipelineCorpusRunnerTest {

    private val merchantNormalizer = MerchantNormalizer()
    private val parserRegistry = ParserRegistry(
        setOf(
            AdcbParser(PatternEngine(TextNormalizer()), merchantNormalizer),
            GenericBankParser(TextNormalizer(), merchantNormalizer),
        ),
    )
    private val runner = PipelineCorpusRunner(
        parserRegistry = parserRegistry,
        institutionRegistry = InstitutionRegistry(),
        merchantResolver = MerchantResolver(MerchantRegistry()),
    )

    @Test
    fun `pipeline corpus fixtures parse and resolve as expected`() {
        val entries = PipelineCorpusLoader.load()
        assertTrue("Expected at least one pipeline-corpus fixture to be loaded", entries.isNotEmpty())

        val results = runner.run(entries)
        val failures = results.filterNot { it.isSuccess }.filterNot { it.entry.knownGap }

        assertTrue(
            "Pipeline corpus regressions found:\n" + failures.joinToString("\n") {
                "${it.entry.label} -> parsed=${it.parsed}, mismatches=${it.mismatches}"
            },
            failures.isEmpty(),
        )
    }
}

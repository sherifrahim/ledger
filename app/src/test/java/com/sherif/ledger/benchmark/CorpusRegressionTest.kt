package com.sherif.ledger.benchmark

import com.sherif.ledger.feature.capture.extraction.FinancialExtractor
import com.sherif.ledger.feature.capture.extraction.HeuristicExtractor
import com.sherif.ledger.feature.capture.extraction.KnownBankExtractor
import com.sherif.ledger.feature.capture.parsing.AdcbParser
import com.sherif.ledger.feature.capture.parsing.GenericBankParser
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.capture.parsing.extraction.MerchantNormalizer
import com.sherif.ledger.feature.capture.parsing.extraction.PatternEngine
import com.sherif.ledger.feature.capture.parsing.extraction.TextNormalizer
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The extractor set under test. This is the ONLY place that names concrete
 * extractors. To benchmark a future GemmaExtractor instead, change this one
 * function — no other benchmark code knows which extractor it measures.
 */
object BenchExtractors {
    fun default(): Set<FinancialExtractor> {
        val textNormalizer = TextNormalizer()
        val merchantNormalizer = MerchantNormalizer()
        val parserRegistry = ParserRegistry(
            setOf(
                AdcbParser(PatternEngine(textNormalizer), merchantNormalizer),
                GenericBankParser(textNormalizer, merchantNormalizer),
            ),
        )
        return setOf(
            KnownBankExtractor(parserRegistry),
            HeuristicExtractor(textNormalizer, merchantNormalizer, com.sherif.ledger.feature.capture.extraction.FinancialPhraseLibrary()),
        )
    }
}

/**
 * DELIVERABLE 5 — Regression Suite. Every corpus fixture that is NOT a known gap
 * becomes an assertion. Any future extractor change that breaks salary, purchase,
 * confirmation, promotion rejection, etc. fails CI immediately. Known gaps are
 * excluded so the gate is green until they are deliberately fixed.
 */
class CorpusRegressionTest {

    private val results by lazy {
        BenchmarkRunner(BenchExtractors.default()).run(CorpusLoader.load())
    }

    @Test fun `every non-gap fixture passes`() {
        val failures = results.filter { !it.passed && !it.fixture.knownGap }
        val detail = failures.joinToString("\n") { "  ${it.fixture.label} -> ${it.failureReason}" }
        assertTrue(
            "Corpus regressions (${failures.size}):\n$detail",
            failures.isEmpty(),
        )
    }

    @Test fun `no false positives among non-gap fixtures`() {
        val fp = BenchmarkReport.falsePositives(results).filter { !it.fixture.knownGap }
        assertTrue(
            "False positives (${fp.size}): ${fp.joinToString { it.fixture.label }}",
            fp.isEmpty(),
        )
    }

    @Test fun `corpus is non-trivial`() {
        // Guard against an empty/mis-located corpus silently passing.
        assertTrue("Corpus too small: ${results.size}", results.size >= 40)
    }
}

/**
 * DELIVERABLES 3, 4, 7 — Benchmark runner + reports. Not an assertion gate; it
 * prints the accuracy/latency/calibration reports, writes the Markdown developer
 * report, and emits review templates for failures. Always passes so CI shows the
 * report without blocking on known gaps.
 */
class BenchmarkReportTest {

    @Test fun `run corpus and emit reports`() {
        val results = BenchmarkRunner(BenchExtractors.default()).run(CorpusLoader.load())

        // Console summary (JUnit-visible).
        println(BenchmarkReport.junitSummary(results))
        val md = BenchmarkReport.markdown(results)
        println(md)

        // Write the Markdown report next to the corpus for inspection.
        val root = CorpusLoader.corpusRoot()
        File(root, "REPORT.md").writeText(md)

        // Unknown fixture generator -> financial-corpus/review/.
        BenchmarkReport.writeReviewTemplates(results, File(root, "review"))

        // This test documents, it does not gate.
        assertTrue(true)
    }
}


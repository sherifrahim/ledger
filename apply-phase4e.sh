#!/bin/bash
set -e
echo "Phase 4E — Extraction Quality & Validation framework (purely additive: test code + corpus resources)..."

mkdir -p "app/src/test/java/com/sherif/ledger/benchmark"
cat > "app/src/test/java/com/sherif/ledger/benchmark/CorpusFixture.kt" << 'LEDGEREOF'
package com.sherif.ledger.benchmark

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * A single corpus fixture: a raw bank message plus its expected extraction
 * outcome. Diagnostics/benchmark only — never used by production code.
 *
 * [knownGap] marks fixtures whose CORRECT expected value the current engine does
 * not yet produce. The benchmark counts them against accuracy (so the number is
 * honest), but the regression suite skips them (so CI stays green and only new
 * breakage fails). Removing the flag is how a future fix "graduates" a gap.
 */
data class CorpusFixture(
    val bank: String,
    val region: String,
    val rawMessage: String,
    val expectedDecision: String,
    val expectedType: String? = null,
    val expectedAmount: Double? = null,
    val expectedCurrency: String? = null,
    val expectedMerchant: String? = null,
    val expectedCardTail: String? = null,
    val expectedCategory: String? = null,
    val knownGap: Boolean = false,
    val gapNote: String? = null,
) {
    val label: String get() = "[$region/$bank] ${rawMessage.take(48)}"
}

/**
 * Loads every `fixtures.json` under `financial-corpus/`. Region and bank are
 * derived from the directory layout (financial-corpus/<region>/<bank>/...).
 */
object CorpusLoader {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Resolves the corpus directory. Works both when tests run from the module
     * root and when resources are on the classpath.
     */
    fun corpusRoot(): File {
        val candidates = listOf(
            File("src/test/resources/financial-corpus"),
            File("app/src/test/resources/financial-corpus"),
            File(javaClass.classLoader?.getResource("financial-corpus")?.file ?: ""),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("financial-corpus not found; looked in $candidates")
    }

    fun load(): List<CorpusFixture> {
        val root = corpusRoot()
        val fixtures = mutableListOf<CorpusFixture>()
        root.walkTopDown()
            .filter { it.isFile && it.name == "fixtures.json" }
            .forEach { file ->
                // financial-corpus/<region>/<bank>/fixtures.json
                val bank = file.parentFile.name
                val region = file.parentFile.parentFile.name
                val arr = json.parseToJsonElement(file.readText()).jsonArray
                arr.forEach { el ->
                    val o = el.jsonObject
                    val exp = o["expected"]!!.jsonObject
                    fun str(k: String) = exp[k]?.jsonPrimitive?.contentOrNull
                    fun dbl(k: String) = exp[k]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                    fixtures += CorpusFixture(
                        bank = bank,
                        region = region,
                        rawMessage = o["rawMessage"]!!.jsonPrimitive.content,
                        expectedDecision = str("decision") ?: "Extracted",
                        expectedType = str("transactionType"),
                        expectedAmount = dbl("amount"),
                        expectedCurrency = str("currency"),
                        expectedMerchant = str("merchant"),
                        expectedCardTail = str("cardTail"),
                        expectedCategory = str("category"),
                        knownGap = o["knownGap"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                        gapNote = o["gapNote"]?.jsonPrimitive?.contentOrNull,
                    )
                }
            }
        return fixtures
    }
}

LEDGEREOF

mkdir -p "app/src/test/java/com/sherif/ledger/benchmark"
cat > "app/src/test/java/com/sherif/ledger/benchmark/BenchmarkRunner.kt" << 'LEDGEREOF'
package com.sherif.ledger.benchmark

import com.sherif.ledger.feature.capture.extraction.ExtractionRegistry
import com.sherif.ledger.feature.capture.extraction.ExtractionValidator
import com.sherif.ledger.feature.capture.extraction.FinancialExtractor
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import kotlinx.coroutines.runBlocking
import java.time.Instant

/** The engine's actual decision for a fixture, normalized to corpus vocabulary. */
enum class BenchDecision { EXTRACTED, IGNORED, CONFIRMATION, FAILED }

/** One fixture's benchmark result. */
data class BenchmarkResult(
    val fixture: CorpusFixture,
    val decision: BenchDecision,
    val category: String?,
    val transactionType: String?,
    val amountMinor: Long?,
    val cardTail: String?,
    val confidence: Int,
    val extractorName: String,
    val latencyNanos: Long,
    val passed: Boolean,
    val failureReason: String?,
)

/**
 * Runs a corpus through the extraction engine and collects results.
 *
 * The runner is written against the [FinancialExtractor] INTERFACE, never a
 * concrete extractor. Swapping HeuristicExtractor for a future GemmaExtractor is
 * a one-line change at the call site; no benchmark code knows which extractor it
 * is measuring. It exercises the real [ExtractionRegistry] path (ranking +
 * validation) so the numbers reflect production behavior.
 */
class BenchmarkRunner(
    private val extractors: Set<FinancialExtractor>,
) {
    private val registry = ExtractionRegistry(extractors, ExtractionValidator())

    fun run(fixtures: List<CorpusFixture>): List<BenchmarkResult> =
        fixtures.map { evaluate(it) }

    private fun evaluate(fx: CorpusFixture): BenchmarkResult {
        val envelope = NotificationEnvelope(
            packageName = "benchmark",
            title = "",
            text = fx.rawMessage,
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "bench-${fx.hashCode()}",
        )

        val start = System.nanoTime()
        val outcome = runBlocking { registry.extract(envelope) }
        val latency = System.nanoTime() - start

        val i = interpret(outcome)
        val (passed, reason) = judge(fx, i.decision, i.category, i.type, i.amount, i.tail)

        return BenchmarkResult(
            fixture = fx,
            decision = i.decision,
            category = i.category,
            transactionType = i.type,
            amountMinor = i.amount,
            cardTail = i.tail,
            confidence = i.confidence,
            extractorName = i.extractor,
            latencyNanos = latency,
            passed = passed,
            failureReason = reason,
        )
    }

    private fun interpret(outcome: ExtractionRegistry.ExtractionOutcome): Interpreted {
        val diag = outcome.diagnostics.lastOrNull()
        val conf = diag?.confidence ?: 0
        val extractor = diag?.extractor ?: "none"
        return when (outcome) {
            is ExtractionRegistry.ExtractionOutcome.Success -> Interpreted(
                BenchDecision.EXTRACTED,
                "Transaction",
                outcome.candidate.transactionType?.name,
                outcome.candidate.amountMinor,
                outcome.candidate.accountHint,
                conf, extractor,
            )
            is ExtractionRegistry.ExtractionOutcome.Ignored -> Interpreted(
                BenchDecision.IGNORED,
                diag?.category,
                null, null, null, conf, extractor,
            )
            is ExtractionRegistry.ExtractionOutcome.Confirmation -> Interpreted(
                BenchDecision.CONFIRMATION,
                "Confirmation",
                null,
                outcome.amountMinor,
                outcome.accountTail,
                conf, extractor,
            )
            is ExtractionRegistry.ExtractionOutcome.Failed -> Interpreted(
                BenchDecision.FAILED, "Unknown", null, null, null, conf, extractor,
            )
        }
    }

    private fun judge(
        fx: CorpusFixture,
        decision: BenchDecision,
        category: String?,
        type: String?,
        amount: Long?,
        tail: String?,
    ): Pair<Boolean, String?> {
        val expectedDecision = when (fx.expectedDecision) {
            "Extracted" -> BenchDecision.EXTRACTED
            "Ignored" -> BenchDecision.IGNORED
            "Confirmation" -> BenchDecision.CONFIRMATION
            "Failed" -> BenchDecision.FAILED
            else -> BenchDecision.EXTRACTED
        }
        if (decision != expectedDecision) {
            return false to "decision ${decision.name.lowercase()} != expected ${fx.expectedDecision.lowercase()}"
        }
        if (expectedDecision == BenchDecision.EXTRACTED) {
            fx.expectedType?.let { if (!it.equals(type, ignoreCase = true)) return false to "type $type != $it" }
            fx.expectedAmount?.let {
                val exp = Math.round(it * 100)
                if (amount != exp) return false to "amount $amount != $exp"
            }
            fx.expectedCardTail?.let { if (it != tail) return false to "tail $tail != $it" }
        }
        if (expectedDecision == BenchDecision.IGNORED) {
            fx.expectedCategory?.let { if (it != category) return false to "category $category != $it" }
        }
        return true to null
    }
}

private data class Interpreted(
    val decision: BenchDecision,
    val category: String?,
    val type: String?,
    val amount: Long?,
    val tail: String?,
    val confidence: Int,
    val extractor: String,
)

LEDGEREOF

mkdir -p "app/src/test/java/com/sherif/ledger/benchmark"
cat > "app/src/test/java/com/sherif/ledger/benchmark/BenchmarkReport.kt" << 'LEDGEREOF'
package com.sherif.ledger.benchmark

import java.io.File

/**
 * Turns [BenchmarkResult]s into the reports of Phase 5: overall and per-bank /
 * per-type accuracy, false positives / negatives, promotion & confirmation
 * accuracy, latency percentiles, confidence calibration, a Markdown developer
 * report, and JSON templates for failures (the "unknown fixture generator").
 *
 * All diagnostics-only. Reads results; changes nothing in the engine.
 */
object BenchmarkReport {

    data class Stats(
        val total: Int,
        val passed: Int,
        val knownGaps: Int,
        val realFailures: Int,
    ) {
        /** Accuracy counting known gaps as failures — the honest number. */
        val accuracyPct: Double get() = if (total == 0) 0.0 else passed * 100.0 / total
        /** Pass rate over fixtures the regression suite actually gates on. */
        val gatedPassPct: Double get() {
            val gated = passed + realFailures
            return if (gated == 0) 100.0 else passed * 100.0 / gated
        }
    }

    fun stats(results: List<BenchmarkResult>): Stats {
        val passed = results.count { it.passed }
        val gaps = results.count { !it.passed && it.fixture.knownGap }
        val real = results.count { !it.passed && !it.fixture.knownGap }
        return Stats(results.size, passed, gaps, real)
    }

    // ---- Accuracy breakdowns ----

    fun accuracyByBank(results: List<BenchmarkResult>): Map<String, Stats> =
        results.groupBy { "${it.fixture.region}/${it.fixture.bank}" }
            .mapValues { stats(it.value) }
            .toSortedMap()

    fun accuracyByType(results: List<BenchmarkResult>): Map<String, Stats> =
        results.filter { it.fixture.expectedDecision == "Extracted" }
            .groupBy { it.fixture.expectedType ?: "Unknown" }
            .mapValues { stats(it.value) }
            .toSortedMap()

    /** False positive: a non-transaction the engine extracted as a transaction. */
    fun falsePositives(results: List<BenchmarkResult>): List<BenchmarkResult> =
        results.filter {
            it.fixture.expectedDecision != "Extracted" && it.decision == BenchDecision.EXTRACTED
        }

    /** False negative: a real transaction the engine did NOT extract. */
    fun falseNegatives(results: List<BenchmarkResult>): List<BenchmarkResult> =
        results.filter {
            it.fixture.expectedDecision == "Extracted" && it.decision != BenchDecision.EXTRACTED
        }

    fun promotionAccuracy(results: List<BenchmarkResult>): Stats =
        stats(results.filter { it.fixture.expectedDecision == "Ignored" })

    fun confirmationAccuracy(results: List<BenchmarkResult>): Stats =
        stats(results.filter { it.fixture.expectedDecision == "Confirmation" })

    // ---- Latency ----

    data class Latency(val avgMs: Double, val p95Ms: Double, val p99Ms: Double, val maxMs: Double)

    fun latency(results: List<BenchmarkResult>): Latency {
        if (results.isEmpty()) return Latency(0.0, 0.0, 0.0, 0.0)
        val ms = results.map { it.latencyNanos / 1_000_000.0 }.sorted()
        fun pct(p: Double) = ms[(p * (ms.size - 1)).toInt()]
        return Latency(ms.average(), pct(0.95), pct(0.99), ms.last())
    }

    // ---- Confidence calibration (statistics, not ML) ----

    data class Calibration(val extractor: String, val claimedAvg: Double, val actualSuccessPct: Double, val n: Int)

    fun calibration(results: List<BenchmarkResult>): List<Calibration> =
        results.groupBy { it.extractorName }.map { (name, rs) ->
            Calibration(
                extractor = name,
                claimedAvg = rs.map { it.confidence }.average(),
                actualSuccessPct = rs.count { it.passed } * 100.0 / rs.size,
                n = rs.size,
            )
        }.sortedByDescending { it.n }

    // ---- Markdown developer report ----

    fun markdown(results: List<BenchmarkResult>): String {
        val s = stats(results)
        val lat = latency(results)
        val fp = falsePositives(results)
        val fn = falseNegatives(results)
        val failures = results.filter { !it.passed }
        val sb = StringBuilder()
        sb.appendLine("# Financial Extraction Report")
        sb.appendLine()
        sb.appendLine("## Overall")
        sb.appendLine("- Fixtures: ${s.total}")
        sb.appendLine("- Passed: ${s.passed}")
        sb.appendLine("- Accuracy: ${"%.1f".format(s.accuracyPct)}% (known gaps counted as failures)")
        sb.appendLine("- Known gaps: ${s.knownGaps} (documented; excluded from the regression gate)")
        sb.appendLine("- Untagged failures: ${s.realFailures}")
        sb.appendLine("- False positives: ${fp.size}")
        sb.appendLine("- False negatives: ${fn.size}")
        sb.appendLine()
        sb.appendLine("## Accuracy by bank")
        accuracyByBank(results).forEach { (bank, st) ->
            sb.appendLine("- $bank: ${"%.0f".format(st.accuracyPct)}% (${st.passed}/${st.total})")
        }
        sb.appendLine()
        sb.appendLine("## Accuracy by transaction type")
        accuracyByType(results).forEach { (type, st) ->
            sb.appendLine("- $type: ${"%.0f".format(st.accuracyPct)}% (${st.passed}/${st.total})")
        }
        sb.appendLine()
        sb.appendLine("## Latency")
        sb.appendLine("- Average: ${"%.2f".format(lat.avgMs)} ms")
        sb.appendLine("- P95: ${"%.2f".format(lat.p95Ms)} ms")
        sb.appendLine("- P99: ${"%.2f".format(lat.p99Ms)} ms")
        sb.appendLine("- Max: ${"%.2f".format(lat.maxMs)} ms")
        sb.appendLine()
        sb.appendLine("## Confidence calibration")
        calibration(results).forEach {
            sb.appendLine("- ${it.extractor}: claims ${"%.0f".format(it.claimedAvg)}, actual ${"%.1f".format(it.actualSuccessPct)}% (n=${it.n})")
        }
        sb.appendLine()
        sb.appendLine("## Top failures (max 20)")
        failures.take(20).forEach {
            val gap = if (it.fixture.knownGap) " [known gap]" else ""
            sb.appendLine("- ${it.fixture.label}$gap")
            sb.appendLine("  - ${it.failureReason}${it.fixture.gapNote?.let { n -> " ($n)" } ?: ""}")
        }
        sb.appendLine()
        sb.appendLine("## Most common failure reason")
        val common = failures.mapNotNull { it.failureReason?.substringBefore(" ") }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }
        sb.appendLine("- ${common?.key ?: "none"} (${common?.value ?: 0})")
        sb.appendLine()
        sb.appendLine("## Recommendations")
        if (fp.isNotEmpty()) sb.appendLine("- Reduce false positives: ${fp.size} non-transaction(s) extracted. Review promotion/confirmation phrases.")
        if (fn.isNotEmpty()) sb.appendLine("- Reduce false negatives: ${fn.size} real transaction(s) missed. Add missing verbs/vocabulary.")
        if (s.knownGaps > 0) sb.appendLine("- Address ${s.knownGaps} known gap(s) in a dedicated fix phase, then remove their knownGap flags.")
        if (fp.isEmpty() && fn.isEmpty() && s.knownGaps == 0) sb.appendLine("- No action; corpus fully green.")
        return sb.toString()
    }

    /** JUnit-style one-line summary. */
    fun junitSummary(results: List<BenchmarkResult>): String {
        val s = stats(results)
        return "Corpus: ${s.total} fixtures, ${s.passed} passed, ${s.realFailures} failed, ${s.knownGaps} known gaps, ${"%.1f".format(s.accuracyPct)}% accuracy"
    }

    /**
     * Unknown fixture generator: for each failure, emit a JSON template into
     * `financial-corpus/review/` for manual classification. No production change.
     */
    fun writeReviewTemplates(results: List<BenchmarkResult>, reviewDir: File) {
        reviewDir.mkdirs()
        results.filter { !it.passed && !it.fixture.knownGap }.forEachIndexed { idx, r ->
            val f = File(reviewDir, "review-${idx.toString().padStart(3, '0')}.json")
            f.writeText(
                """
                {
                  "rawMessage": ${jsonString(r.fixture.rawMessage)},
                  "observed": {
                    "decision": "${r.decision.name.lowercase()}",
                    "category": ${jsonString(r.category)},
                    "transactionType": ${jsonString(r.transactionType)},
                    "amountMinor": ${r.amountMinor},
                    "cardTail": ${jsonString(r.cardTail)},
                    "confidence": ${r.confidence},
                    "extractor": "${r.extractorName}"
                  },
                  "expectedFromCorpus": {
                    "decision": "${r.fixture.expectedDecision}"
                  },
                  "failureReason": ${jsonString(r.failureReason)},
                  "TODO": "classify and move into the correct bank fixtures.json"
                }
                """.trimIndent(),
            )
        }
    }

    private fun jsonString(v: String?): String =
        if (v == null) "null" else "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

LEDGEREOF

mkdir -p "app/src/test/java/com/sherif/ledger/benchmark"
cat > "app/src/test/java/com/sherif/ledger/benchmark/CorpusRegressionTest.kt" << 'LEDGEREOF'
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

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/axis"
cat > "app/src/test/resources/financial-corpus/india/axis/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your axis account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your axis card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/bob"
cat > "app/src/test/resources/financial-corpus/india/bob/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your bob account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your bob card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/canara"
cat > "app/src/test/resources/financial-corpus/india/canara/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your canara account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your canara card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/federal"
cat > "app/src/test/resources/financial-corpus/india/federal/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your federal account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your federal card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/hdfc"
cat > "app/src/test/resources/financial-corpus/india/hdfc/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs. 500.00 debited from a/c XXXX1234 for UPI to JOHN on 05-Jul. Avl bal Rs 12000.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 500.0}},
  {"rawMessage": "Salary of Rs 60000 credited to your HDFC account XXXX1234.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 60000.0}},
  {"rawMessage": "Your HDFC OTP is 112233. Valid for 5 minutes.", "expected": {"decision": "Ignored", "category": "OTP"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/icici"
cat > "app/src/test/resources/financial-corpus/india/icici/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 1500.00 spent at BIG BAZAAR using card ending 4321.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 1500.0, "cardTail": "4321"}},
  {"rawMessage": "Interest of Rs 320.00 credited to your ICICI savings account XXXX7788.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 320.0}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/idfc"
cat > "app/src/test/resources/financial-corpus/india/idfc/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your idfc account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your idfc card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/kotak"
cat > "app/src/test/resources/financial-corpus/india/kotak/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your kotak account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your kotak card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/pnb"
cat > "app/src/test/resources/financial-corpus/india/pnb/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 750.00 debited from your pnb account XXXX9090 at DMART.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 750.0}},
  {"rawMessage": "Get 5% cashback on your pnb card. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/india/sbi"
cat > "app/src/test/resources/financial-corpus/india/sbi/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Rs 250.00 debited via NEFT to RAVI from account XXXX5678.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 250.0}},
  {"rawMessage": "You are pre-approved for an SBI personal loan. Apply now!", "expected": {"decision": "Ignored", "category": "Loan Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/adcb"
cat > "app/src/test/resources/financial-corpus/uae/adcb/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Your salary AED6000.00 has been credited to your account no. XXX920001 on Jul 3 2026 2:12PM. The available balance is AED9079.30.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 6000.0, "currency": "AED"}},
  {"rawMessage": "Notification Purchase of AED 50.00 at COSTA COFFEE with card ending 1234.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 50.0, "currency": "AED", "cardTail": "1234"}},
  {"rawMessage": "AED700.00 transferred via ADCB Personal Internet Banking / Mobile App from acc. no. XXX920001 on Jun 29 2026 5:06PM. Avl. bal. AED 1630.30.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 700.0, "currency": "AED"}},
  {"rawMessage": "AED3000.00 withdrawn from acc. XXX920001 on Jun 24 2026 6:01PM at ATM-Index EXC Hamdaan. Avl.Bal.AED958.80.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 3000.0, "currency": "AED"}},
  {"rawMessage": "Your debit card XXX5986 linked to acc. XXX920001 was used for USD21.00 on Jul 3 2026 3:25PM at ANTHROPIC CLAUD,US. Avl.Bal AED 8999.38.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 21.0}},
  {"rawMessage": "A Cr. transaction of AED 1245.00 on your account no. XXX920001 was successful.Available balance is AED2975.80.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 1245.0, "currency": "AED"}, "knownGap": true, "gapNote": "'Cr. transaction' not yet mapped to Income; engine returns Expense"},
  {"rawMessage": "Your OTP for ADCB login is 445566. Do not share with anyone.", "expected": {"decision": "Ignored", "category": "OTP"}},
  {"rawMessage": "Your ADCB monthly statement is ready. Total due AED 500.", "expected": {"decision": "Ignored", "category": "Statement"}},
  {"rawMessage": "You are eligible for a personal loan of AED 50,000. Apply now for instant approval.", "expected": {"decision": "Ignored", "category": "Loan Offer"}},
  {"rawMessage": "AED 200 debited from your account towards FAB Credit Card.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 200.0}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/adib"
cat > "app/src/test/resources/financial-corpus/uae/adib/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "AED 120.50 debited from your ADIB account XXXX4455 at CARREFOUR.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 120.5}},
  {"rawMessage": "Annual fee of AED 100.00 debited from card ending 5566.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 100.0, "cardTail": "5566"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/cbd"
cat > "app/src/test/resources/financial-corpus/uae/cbd/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Cheque of AED 3000.00 deposited to your CBD account XXXX1122.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 3000.0}},
  {"rawMessage": "EMI of AED 1,200 debited from your account XXXX1234 towards loan.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 1200.0}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/dib"
cat > "app/src/test/resources/financial-corpus/uae/dib/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "AED 75.00 sent to AHMED via fund transfer from A/C XXXX2233.", "expected": {"decision": "Extracted", "transactionType": "Transfer", "amount": 75.0}},
  {"rawMessage": "Profit of AED 45.20 credited to your DIB savings account XXXX9012.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 45.2}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/enbd"
cat > "app/src/test/resources/financial-corpus/uae/enbd/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Purchase of AED 37.75 with Credit Card ending 8165 at Noon Minutes, Dubai. Avl Cr. Limit is AED 12,441.91", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 37.75, "cardTail": "8165"}},
  {"rawMessage": "Payment of AED 2.25 to Noon Food with Credit Card ending 8165. Avl Cr. Limit is AED 11,779.66.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 2.25, "cardTail": "8165"}},
  {"rawMessage": "Your Emirates NBD verification code is 998877. Do not share.", "expected": {"decision": "Ignored", "category": "OTP"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/fab"
cat > "app/src/test/resources/financial-corpus/uae/fab/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Credit Card Purchase Card No XXXX6989 AED 54.69 Amazon Grocery Dubai ARE 15/06/26 20:01 Avl Bal AED 2077.96", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 54.69, "cardTail": "6989"}},
  {"rawMessage": "Credit Card Purchase Card No XXXX6989 AED 12.00 CARS TAXI ABU DHABI ARE 16/05/26 00:36 Avl Bal AED 10532.65", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 12.0, "cardTail": "6989"}},
  {"rawMessage": "Payment of AED 200 received. Outstanding balance AED 3450.00. Thank you for using FAB.", "expected": {"decision": "Confirmation"}},
  {"rawMessage": "Payment received for card ending 1959. Outstanding balance AED 1,200.00.", "expected": {"decision": "Confirmation"}},
  {"rawMessage": "Get 10% cashback on your next FAB card purchase. Avail now. Limited time offer.", "expected": {"decision": "Ignored", "category": "Offer"}, "knownGap": true, "gapNote": "'purchase' verb + '10%' fallback amount cause false Extracted; should be Offer"}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/mashreq"
cat > "app/src/test/resources/financial-corpus/uae/mashreq/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "Mashreq Credit Card ending 1959 was used for a transaction of AED 3.00 at FRESH WAY BAQALA on Sunday, 5 July 2026, 3:51 pm. Available limit: AED 7,869.08", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 3.0, "cardTail": "1959"}},
  {"rawMessage": "Thank you for using your card ending 1959 for AED 30.50 at SEA SHELL CORNICHE on 04-JUL-2026 01:22 AM. Avl.Limit: AED 7,980.08", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 30.5, "cardTail": "1959"}, "knownGap": true, "gapNote": "'thank you' falsely triggers Confirmation on a real purchase"},
  {"rawMessage": "Increase your Mashreq credit limit instantly. You are pre-approved.", "expected": {"decision": "Ignored", "category": "Credit Limit Offer"}}
]

LEDGEREOF

mkdir -p "app/src/test/resources/financial-corpus/uae/wio"
cat > "app/src/test/resources/financial-corpus/uae/wio/fixtures.json" << 'LEDGEREOF'
[
  {"rawMessage": "AED 250.00 spent at STARBUCKS using card ending 9999.", "expected": {"decision": "Extracted", "transactionType": "Expense", "amount": 250.0, "cardTail": "9999"}},
  {"rawMessage": "Your loan of AED 50,000 has been disbursed and credited to account XXXX1234.", "expected": {"decision": "Extracted", "transactionType": "Income", "amount": 50000.0}}
]

LEDGEREOF

mkdir -p app/src/test/resources/financial-corpus/review
echo "Phase 4E applied. Run: ./gradlew testDebugUnitTest --tests \"*CorpusRegressionTest\" --tests \"*BenchmarkReportTest\""

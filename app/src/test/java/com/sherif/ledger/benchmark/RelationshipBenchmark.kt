package com.sherif.ledger.benchmark

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.relationship.FinancialRelationship
import com.sherif.ledger.feature.relationship.RelationshipEngine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/** One expected relationship in a corpus scenario. */
data class ExpectedRelationship(
    val type: String,
    val source: Long,
    val target: Long?,
    val minConfidence: Int,
)

/** One corpus scenario: a set of transactions plus the relationships expected. */
data class RelationshipScenario(
    val name: String,
    val transactions: List<Transaction>,
    val expected: List<ExpectedRelationship>,
)

/** Loads `relationship-corpus/scenarios.json` from test resources. */
object RelationshipCorpusLoader {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun corpusFile(): File = listOf(
        File("src/test/resources/relationship-corpus/scenarios.json"),
        File("app/src/test/resources/relationship-corpus/scenarios.json"),
    ).firstOrNull { it.isFile } ?: error("relationship-corpus/scenarios.json not found")

    fun load(): List<RelationshipScenario> {
        val arr = json.parseToJsonElement(corpusFile().readText()).jsonArray
        return arr.map { el ->
            val o = el.jsonObject
            val txns = o["transactions"]!!.jsonArray.map { t ->
                val to = t.jsonObject
                Transaction(
                    id = to["id"]!!.jsonPrimitive.long,
                    accountId = to["accountId"]!!.jsonPrimitive.long,
                    brandId = null,
                    categoryId = null,
                    amount = Money(to["amountMinor"]!!.jsonPrimitive.long, CurrencyCode.AED),
                    type = TransactionType.valueOf(to["type"]!!.jsonPrimitive.content),
                    timestamp = Instant.ofEpochSecond(to["epochSecond"]!!.jsonPrimitive.long),
                    source = IngestionSource.NOTIFICATION,
                    rawText = to["rawText"]?.jsonPrimitive?.contentOrNull,
                    cardTail = to["cardTail"]?.jsonPrimitive?.contentOrNull,
                    fingerprint = "bench-${to["id"]!!.jsonPrimitive.long}",
                )
            }
            val expected = o["expected"]!!.jsonArray.map { e ->
                val eo = e.jsonObject
                ExpectedRelationship(
                    type = eo["type"]!!.jsonPrimitive.content,
                    source = eo["source"]!!.jsonPrimitive.long,
                    target = eo["target"]?.jsonPrimitive?.longOrNull(),
                    minConfidence = eo["minConfidence"]!!.jsonPrimitive.int,
                )
            }
            RelationshipScenario(o["name"]!!.jsonPrimitive.content, txns, expected)
        }
    }

    private fun kotlinx.serialization.json.JsonPrimitive.longOrNull(): Long? =
        this.contentOrNull?.toLongOrNull()
}

/** Metrics for one benchmark run. */
data class RelationshipBenchmarkReport(
    val scenarios: Int,
    val expectedTotal: Int,
    val matched: Int,
    val falseRelationships: Int,
    val missedRelationships: Int,
    val precision: Double,
    val recall: Double,
    val coverage: Double,
    val avgLatencyMs: Double,
    val p95LatencyMs: Double,
    val calibration: Map<String, Pair<Int, Int>>, // band -> (count, correct)
    val markdown: String,
)

/**
 * Benchmarks the RelationshipEngine on the relationship corpus: accuracy,
 * precision, recall, coverage, calibration, latency, and false / missed
 * relationships. Additive; touches no production or frozen code.
 */
class RelationshipBenchmarkRunner(
    private val engine: RelationshipEngine = RelationshipEngine.default(),
) {

    fun run(scenarios: List<RelationshipScenario>): RelationshipBenchmarkReport {
        var expectedTotal = 0
        var matched = 0
        var falseRel = 0
        val latencies = mutableListOf<Double>()
        val calibration = mutableMapOf("HIGH" to Pair(0, 0), "MEDIUM" to Pair(0, 0), "LOW" to Pair(0, 0))
        val missDetails = mutableListOf<String>()
        val fpDetails = mutableListOf<String>()

        scenarios.forEach { sc ->
            val t0 = System.nanoTime()
            val produced = engine.analyze(sc.transactions)
            latencies += (System.nanoTime() - t0) / 1_000_000.0

            expectedTotal += sc.expected.size
            val producedMatchedFlags = BooleanArray(produced.size)

            sc.expected.forEach { exp ->
                val idx = produced.indexOfFirst { p ->
                    p.type.name == exp.type &&
                        p.sourceTransactionId == exp.source &&
                        p.targetTransactionId == exp.target &&
                        p.confidence.value >= exp.minConfidence
                }
                if (idx >= 0) {
                    matched++
                    producedMatchedFlags[idx] = true
                    val band = produced[idx].confidence.band.name
                    val (c, ok) = calibration.getValue(band)
                    calibration[band] = Pair(c + 1, ok + 1)
                } else {
                    missDetails += "${sc.name}: ${exp.type} ${exp.source}->${exp.target} (>=${exp.minConfidence})"
                }
            }

            // Any produced relationship not matching an expected one is a false relationship,
            // but only counted in scenarios that declare their full expected set (all here do).
            produced.forEachIndexed { i, p ->
                val expectedHere = sc.expected.any {
                    it.type == p.type.name && it.source == p.sourceTransactionId && it.target == p.targetTransactionId
                }
                if (!expectedHere) {
                    falseRel++
                    fpDetails += "${sc.name}: ${p.type.name} ${p.sourceTransactionId}->${p.targetTransactionId}"
                    val band = p.confidence.band.name
                    val (c, ok) = calibration.getValue(band)
                    calibration[band] = Pair(c + 1, ok) // counted, not correct
                }
            }
        }

        val missed = expectedTotal - matched
        val producedTotal = matched + falseRel
        val precision = if (producedTotal == 0) 1.0 else matched.toDouble() / producedTotal
        val recall = if (expectedTotal == 0) 1.0 else matched.toDouble() / expectedTotal
        val coverage = scenarios.count { sc ->
            sc.expected.isEmpty() || sc.expected.all { exp ->
                engine.analyze(sc.transactions).any {
                    it.type.name == exp.type && it.sourceTransactionId == exp.source
                }
            }
        }.toDouble() / scenarios.size
        val sortedLat = latencies.sorted()
        val avg = if (sortedLat.isEmpty()) 0.0 else sortedLat.average()
        val p95 = if (sortedLat.isEmpty()) 0.0 else sortedLat[(0.95 * (sortedLat.size - 1)).toInt()]

        val md = buildString {
            appendLine("# Relationship Engine Benchmark")
            appendLine("- Engine version: ${engine.engineVersion}")
            appendLine("- Scenarios: ${scenarios.size}")
            appendLine("- Expected relationships: $expectedTotal")
            appendLine("- Matched: $matched")
            appendLine("- Missed: $missed")
            appendLine("- False relationships: $falseRel")
            appendLine("- Precision: ${"%.1f".format(precision * 100)}%")
            appendLine("- Recall: ${"%.1f".format(recall * 100)}%")
            appendLine("- Scenario coverage: ${"%.1f".format(coverage * 100)}%")
            appendLine("- Latency avg: ${"%.3f".format(avg)}ms, p95: ${"%.3f".format(p95)}ms")
            appendLine()
            appendLine("## Confidence calibration")
            calibration.forEach { (band, cc) ->
                val (count, correct) = cc
                val pct = if (count == 0) 0.0 else correct * 100.0 / count
                appendLine("- $band: $correct/$count correct (${"%.0f".format(pct)}%)")
            }
            if (missDetails.isNotEmpty()) {
                appendLine(); appendLine("## Missed"); missDetails.forEach { appendLine("- $it") }
            }
            if (fpDetails.isNotEmpty()) {
                appendLine(); appendLine("## False relationships"); fpDetails.forEach { appendLine("- $it") }
            }
        }

        return RelationshipBenchmarkReport(
            scenarios = scenarios.size,
            expectedTotal = expectedTotal,
            matched = matched,
            falseRelationships = falseRel,
            missedRelationships = missed,
            precision = precision,
            recall = recall,
            coverage = coverage,
            avgLatencyMs = avg,
            p95LatencyMs = p95,
            calibration = calibration,
            markdown = md,
        )
    }
}

/** DELIVERABLE — Relationship regression suite: every expected relationship must
 *  be produced, and no false relationships may appear. */
class RelationshipRegressionTest {

    private val report by lazy {
        RelationshipBenchmarkRunner().run(RelationshipCorpusLoader.load())
    }

    @Test fun `all expected relationships are produced`() {
        assertTrue(
            "Missed ${report.missedRelationships} relationships:\n${report.markdown}",
            report.missedRelationships == 0,
        )
    }

    @Test fun `no false relationships`() {
        assertTrue(
            "Produced ${report.falseRelationships} false relationships:\n${report.markdown}",
            report.falseRelationships == 0,
        )
    }

    @Test fun `corpus is non-trivial`() {
        assertTrue("Relationship corpus too small: ${report.scenarios}", report.scenarios >= 10)
    }

    @Test fun `engine is deterministic`() {
        val scenarios = RelationshipCorpusLoader.load()
        val a = RelationshipBenchmarkRunner().run(scenarios)
        val b = RelationshipBenchmarkRunner().run(scenarios)
        assertTrue("Engine not deterministic", a.matched == b.matched && a.falseRelationships == b.falseRelationships)
    }
}

/** Prints the relationship benchmark report; documents, does not gate. */
class RelationshipBenchmarkReportTest {

    @Test fun `run relationship corpus and print report`() {
        val report = RelationshipBenchmarkRunner().run(RelationshipCorpusLoader.load())
        println(report.markdown)
        assertTrue(true)
    }
}


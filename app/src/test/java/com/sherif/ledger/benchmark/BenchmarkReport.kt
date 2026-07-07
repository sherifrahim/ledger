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


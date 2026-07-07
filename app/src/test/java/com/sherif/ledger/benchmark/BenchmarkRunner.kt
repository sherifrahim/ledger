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


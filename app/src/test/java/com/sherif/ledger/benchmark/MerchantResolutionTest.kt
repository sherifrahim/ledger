package com.sherif.ledger.benchmark

import com.sherif.ledger.feature.merchant.MerchantRegistry
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** One merchant-corpus fixture: raw text plus expected canonical/category. */
data class MerchantFixture(
    val raw: String,
    val expectedCanonical: String?,
    val expectedCategory: String,
)

/** Loads `merchant-corpus/merchants.json` from test resources. */
object MerchantCorpusLoader {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun corpusFile(): File = listOf(
        File("src/test/resources/merchant-corpus/merchants.json"),
        File("app/src/test/resources/merchant-corpus/merchants.json"),
    ).firstOrNull { it.isFile }
        ?: error("merchant-corpus/merchants.json not found")

    fun load(): List<MerchantFixture> {
        val arr = json.parseToJsonElement(corpusFile().readText()).jsonArray
        return arr.map { el ->
            val o = el.jsonObject
            MerchantFixture(
                raw = o["raw"]!!.jsonPrimitive.content,
                expectedCanonical = o["expectedCanonical"]?.jsonPrimitive?.contentOrNull,
                expectedCategory = o["expectedCategory"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
            )
        }
    }
}

/** Result of resolving one merchant fixture. */
data class MerchantBenchResult(
    val fixture: MerchantFixture,
    val resolvedCanonical: String?,
    val resolvedCategory: String,
    val matchedAlias: String?,
    val confidence: Int,
    val passed: Boolean,
)

/**
 * Benchmarks merchant normalization accuracy. Runs every merchant fixture through
 * the real [MerchantResolver] and measures how many raw strings resolve to the
 * correct canonical name and category.
 */
class MerchantBenchmarkRunner(
    private val resolver: MerchantResolver = MerchantResolver(MerchantRegistry()),
) {
    fun run(fixtures: List<MerchantFixture>): List<MerchantBenchResult> = fixtures.map { fx ->
        val r = resolver.resolve(fx.raw)
        val canonical = (r as? MerchantResolution.Resolved)?.canonicalName
        val category = when (r) {
            is MerchantResolution.Resolved -> r.category.name
            is MerchantResolution.Unresolved -> "UNKNOWN"
        }
        val alias = (r as? MerchantResolution.Resolved)?.matchedAlias
        val confidence = (r as? MerchantResolution.Resolved)?.confidence ?: 0
        val passed = canonical == fx.expectedCanonical &&
            (canonical == null || category == fx.expectedCategory)
        MerchantBenchResult(fx, canonical, category, alias, confidence, passed)
    }

    fun report(results: List<MerchantBenchResult>): String {
        val total = results.size
        val passed = results.count { it.passed }
        val resolved = results.count { it.resolvedCanonical != null }
        val sb = StringBuilder()
        sb.appendLine("# Merchant Normalization Report")
        sb.appendLine("- Fixtures: $total")
        sb.appendLine("- Accuracy: ${"%.1f".format(passed * 100.0 / total)}% ($passed/$total)")
        sb.appendLine("- Resolved: $resolved, Unresolved: ${total - resolved}")
        sb.appendLine()
        sb.appendLine("## Raw -> Normalized -> Alias -> Confidence")
        results.forEach {
            val arrow = it.resolvedCanonical ?: "(unresolved)"
            sb.appendLine("- ${it.fixture.raw} -> $arrow -> ${it.matchedAlias ?: "-"} -> ${it.confidence}")
        }
        val fails = results.filter { !it.passed }
        if (fails.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Failures")
            fails.forEach {
                sb.appendLine("- ${it.fixture.raw}: got ${it.resolvedCanonical}/${it.resolvedCategory}, expected ${it.fixture.expectedCanonical}/${it.fixture.expectedCategory}")
            }
        }
        return sb.toString()
    }
}

/**
 * DELIVERABLE — Merchant regression suite. Every merchant fixture becomes an
 * assertion: any change that breaks resolution fails CI. Additive; touches no
 * production or frozen code.
 */
class MerchantResolutionTest {

    private val results by lazy {
        MerchantBenchmarkRunner().run(MerchantCorpusLoader.load())
    }

    @Test fun `every merchant fixture resolves correctly`() {
        val failures = results.filter { !it.passed }
        val detail = failures.joinToString("\n") {
            "  ${it.fixture.raw} -> ${it.resolvedCanonical}/${it.resolvedCategory} (expected ${it.fixture.expectedCanonical}/${it.fixture.expectedCategory})"
        }
        assertTrue("Merchant resolution regressions (${failures.size}):\n$detail", failures.isEmpty())
    }

    @Test fun `known merchants resolve, unknown stay unresolved`() {
        // No false resolutions: an unknown raw must not be mapped to a canonical.
        val falseResolutions = results.filter {
            it.fixture.expectedCanonical == null && it.resolvedCanonical != null
        }
        assertTrue(
            "False resolutions: ${falseResolutions.joinToString { it.fixture.raw }}",
            falseResolutions.isEmpty(),
        )
    }

    @Test fun `corpus is non-trivial`() {
        assertTrue("Merchant corpus too small: ${results.size}", results.size >= 25)
    }
}

/** Prints the merchant normalization report; documents, does not gate. */
class MerchantBenchmarkReportTest {

    @Test fun `run merchant corpus and print report`() {
        val runner = MerchantBenchmarkRunner()
        val results = runner.run(MerchantCorpusLoader.load())
        println(runner.report(results))
        assertTrue(true)
    }
}


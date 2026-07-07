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


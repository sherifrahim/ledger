package com.sherif.ledger.regression

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Loads every `fixtures.json` under `pipeline-corpus/` — same directory-walk
 * convention as `benchmark/CorpusLoader`, but grouped by SOURCE TYPE
 * (`pipeline-corpus/<sourceType>/fixtures.json`) rather than by region/bank,
 * since Phase D's explicit axis is "which capture source," not "which bank."
 */
object PipelineCorpusLoader {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun corpusRoot(): File {
        val candidates = listOf(
            File("src/test/resources/pipeline-corpus"),
            File("app/src/test/resources/pipeline-corpus"),
            File(javaClass.classLoader?.getResource("pipeline-corpus")?.file ?: ""),
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("pipeline-corpus not found; looked in $candidates")
    }

    fun load(): List<PipelineCorpusEntry> {
        val root = corpusRoot()
        val entries = mutableListOf<PipelineCorpusEntry>()
        root.walkTopDown()
            .filter { it.isFile && it.name == "fixtures.json" }
            .forEach { file ->
                // pipeline-corpus/<sourceType>/fixtures.json — sourceType is directory-derived,
                // but every entry may also override it (useful if a directory ever mixes sources).
                val directorySourceType = file.parentFile.name.uppercase()
                val arr = json.parseToJsonElement(file.readText()).jsonArray
                arr.forEach { el ->
                    val o = el.jsonObject
                    val exp = o["expected"]?.jsonObject
                    fun str(k: String) = exp?.get(k)?.jsonPrimitive?.contentOrNull
                    fun long(k: String) = exp?.get(k)?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    entries += PipelineCorpusEntry(
                        id = o["id"]!!.jsonPrimitive.content,
                        sourceType = o["sourceType"]?.jsonPrimitive?.contentOrNull ?: directorySourceType,
                        rawText = o["rawText"]!!.jsonPrimitive.content,
                        senderOrPackage = o["senderOrPackage"]!!.jsonPrimitive.content,
                        expectedInstitution = str("institution"),
                        expectedMerchant = str("merchant"),
                        expectedCategory = str("category"),
                        expectedRelationshipType = str("relationshipType"),
                        expectedAccountHint = str("accountHint"),
                        expectedBalanceEffectMinor = long("balanceEffectMinor"),
                        expectedDuplicateStatus = str("duplicateStatus"),
                        expectedForecastNote = str("forecastNote"),
                        knownGap = o["knownGap"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false,
                        gapNote = o["gapNote"]?.jsonPrimitive?.contentOrNull,
                    )
                }
            }
        return entries
    }
}

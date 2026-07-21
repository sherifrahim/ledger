package com.sherif.ledger.regression

import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.ParseResult
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import com.sherif.ledger.feature.merchant.GenericCategoryKeywords
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import java.time.Instant

data class PipelineCorpusResult(
    val entry: PipelineCorpusEntry,
    val parsed: Boolean,
    val mismatches: List<String>,
    val notValidated: List<String>,
) {
    val isSuccess: Boolean get() = parsed && mismatches.isEmpty()
}

/**
 * RC9 Phase D — the harness half of the regression corpus infrastructure.
 * Deliberately lightweight and Android-framework-free (this repo has no
 * Robolectric — plain JVM unit tests only, per CLAUDE.md), so it exercises
 * only the pieces that are pure Kotlin and deterministic:
 *
 * - **Parsing** — real `ParserRegistry` (whichever `BankParser`s the caller wires in).
 * - **Institution** — real `InstitutionRegistry.resolve()`.
 * - **Merchant / Category** — real `feature.merchant.MerchantResolver` +
 *   `GenericCategoryKeywords`, the same deterministic tiers
 *   `CategoryIntelligenceEngine`/`GetFinancialAnalyticsUseCase` use. Does NOT
 *   construct `CategoryIntelligenceEngine` itself — that class also depends on
 *   `AIOrchestrator`'s full graph (settings, secure storage, HTTP), which is
 *   both heavy to fake here and the wrong thing for a DETERMINISTIC regression
 *   suite to assert against (AI output isn't a stable expected value).
 *
 * **Honestly NOT validated** (the entry schema carries these fields for
 * forward-compatibility, but this harness version cannot check them — a
 * single raw message has no persisted-transaction context to relate to):
 * relationship type, account balance effect, duplicate status, forecast
 * behavior. Extending this harness to cover them means feeding a SEQUENCE
 * of corpus entries through a real `Transaction` repository fake and running
 * `RelationshipEngine`/`ReconciliationEngine`/`AccountBalanceService`/
 * `ForecastEngine` over the result — a real, larger follow-up, not attempted
 * here (infrastructure only, per the instruction not to over-build this RC).
 */
class PipelineCorpusRunner(
    private val parserRegistry: ParserRegistry,
    private val institutionRegistry: InstitutionRegistry,
    private val merchantResolver: MerchantResolver,
) {
    fun run(entries: List<PipelineCorpusEntry>): List<PipelineCorpusResult> = entries.map(::validate)

    private fun validate(entry: PipelineCorpusEntry): PipelineCorpusResult {
        val envelope = NotificationEnvelope(
            packageName = entry.senderOrPackage,
            title = "",
            text = entry.rawText,
            subText = null,
            timestamp = Instant.now(),
            notificationKey = "corpus_${entry.id}",
        )
        val parseResult = parserRegistry.parse(envelope)
        val candidate = (parseResult as? ParseResult.Success)?.candidate

        val mismatches = mutableListOf<String>()

        entry.expectedInstitution?.let { expected ->
            val actual = institutionRegistry.resolve(entry.senderOrPackage)?.name
            if (actual != expected) mismatches += "institution: expected '$expected', got '$actual'"
        }

        val merchantResolution = candidate?.merchantName?.let { merchantResolver.resolve(it) }

        entry.expectedMerchant?.let { expected ->
            val actual = merchantResolution?.displayName
            if (actual != expected) mismatches += "merchant: expected '$expected', got '$actual'"
        }

        entry.expectedCategory?.let { expected ->
            val actual = (merchantResolution as? MerchantResolution.Resolved)?.category?.name
                ?: candidate?.merchantName?.let { GenericCategoryKeywords.classify(it)?.name }
            if (actual != expected) mismatches += "category: expected '$expected', got '$actual'"
        }

        entry.expectedAccountHint?.let { expected ->
            val actual = candidate?.accountHint
            if (actual != expected) mismatches += "accountHint: expected '$expected', got '$actual'"
        }

        val notValidated = listOfNotNull(
            entry.expectedRelationshipType?.let { "relationshipType" },
            entry.expectedBalanceEffectMinor?.let { "balanceEffectMinor" },
            entry.expectedDuplicateStatus?.let { "duplicateStatus" },
            entry.expectedForecastNote?.let { "forecastNote" },
        )

        return PipelineCorpusResult(entry, parsed = candidate != null, mismatches = mismatches, notValidated = notValidated)
    }
}

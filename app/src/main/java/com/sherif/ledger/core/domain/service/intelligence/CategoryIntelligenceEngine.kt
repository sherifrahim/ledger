package com.sherif.ledger.core.domain.service.intelligence

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.feature.ai.confidence.ConfidenceGate
import com.sherif.ledger.feature.ai.context.AIContextBuilder
import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.orchestrator.AIOrchestrator
import com.sherif.ledger.feature.ai.orchestrator.AIOrchestratorResult
import com.sherif.ledger.feature.merchant.GenericCategoryKeywords
import com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore
import com.sherif.ledger.feature.merchant.MerchantCategory
import com.sherif.ledger.feature.merchant.MerchantRegistry
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.relationship.RelationshipType
import javax.inject.Inject
import javax.inject.Singleton

/** Where a [CategoryResolution] came from — the Intelligence Inspector shows this, never leaves it implicit. */
enum class CategorySource { LEARNED_MEMORY, MERCHANT_REGISTRY, RELATIONSHIP_HINT, DETERMINISTIC_KEYWORDS, AI_SUGGESTION, UNKNOWN }

/** RC8 Phase C: category + subcategory + confidence + reason + source, always — never a bare string. */
data class CategoryResolution(
    val category: String,
    val subcategory: String?,
    val confidence: Int,
    val reason: String,
    val source: CategorySource,
)

/**
 * RC8 Phase C — Category Intelligence, in the exact resolution order the RC8
 * spec calls for: user-confirmed memory, merchant defaults, institution/
 * relationship hints, deterministic rules, AI suggestion. Every tier returns
 * category + subcategory + confidence + reason, never a bare string the way
 * [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase]'s
 * existing inline chain does.
 *
 * Deliberately a NEW, standalone engine rather than a refactor of that use
 * case's inline chain — `GetFinancialAnalyticsUseCase.kt` is a frozen file
 * (demonstrated-bug-only) and this is a genuine enrichment, not a fix; the
 * two chains are intentionally the same 4 deterministic tiers today (a small,
 * accepted duplication) so this can evolve independently (subcategory,
 * relationship hints, AI fallback) without risking the analytics hot path.
 *
 * [resolveDeterministic] is synchronous and side-effect-free — safe to call
 * from a hot path. [resolveWithAiFallback] is the ONLY suspend method and the
 * ONLY place this engine ever calls [AIOrchestrator] — gated by
 * [ConfidenceGate] so AI is consulted exactly when the RC8 spec says it
 * should be ("no deterministic rule exists... category uncertain"), never
 * unconditionally. Every AI response still passes AIOrchestrator's own
 * validate/audit/metrics/debug-trace pipeline — this class adds no bypass.
 */
@Singleton
class CategoryIntelligenceEngine @Inject constructor(
    private val merchantResolver: MerchantResolver,
    private val merchantRegistry: MerchantRegistry,
    private val learnedMerchantCategoryStore: LearnedMerchantCategoryStore,
    private val confidenceGate: ConfidenceGate,
    private val aiContextBuilder: AIContextBuilder,
    private val aiOrchestrator: AIOrchestrator,
) {
    /**
     * Tiers 1-4. [relationshipHint] is caller-supplied (e.g. from a single
     * shared `RelationshipEngine.analyze()` pass the caller already ran) —
     * this engine never re-runs relationship analysis itself, matching the
     * "one relationship pass, shared" principle the rest of the codebase
     * already follows (see AccountBalanceService/GetFinancialAnalyticsUseCase).
     */
    fun resolveDeterministic(rawMerchantText: String?, relationshipHint: RelationshipType? = null): CategoryResolution {
        learnedMerchantCategoryStore.categoryFor(rawMerchantText)?.let {
            return CategoryResolution(it.name, null, 100, "User-taught category for this exact merchant text (Review Inbox)", CategorySource.LEARNED_MEMORY)
        }

        val resolution = merchantResolver.resolve(rawMerchantText)
        if (resolution is MerchantResolution.Resolved) {
            return CategoryResolution(
                category = resolution.category.name,
                subcategory = resolution.profile.subcategory,
                confidence = resolution.confidence,
                reason = resolution.reason,
                source = CategorySource.MERCHANT_REGISTRY,
            )
        }

        relationshipHint?.let { type -> categoryForRelationship(type) }?.let { (category, reason) ->
            return CategoryResolution(category, null, 75, reason, CategorySource.RELATIONSHIP_HINT)
        }

        GenericCategoryKeywords.classify(rawMerchantText)?.let {
            return CategoryResolution(it.name, null, 60, "Deterministic keyword match (no registry/relationship signal)", CategorySource.DETERMINISTIC_KEYWORDS)
        }

        return CategoryResolution("UNKNOWN", null, 0, "No deterministic signal matched — merchant unrecognized, no relevant relationship, no keyword hit", CategorySource.UNKNOWN)
    }

    /**
     * Tier 5, on demand only — never called from a hot/automatic path. The
     * caller (Intelligence Inspector today) decides WHEN to call this, e.g. a
     * user-triggered "Ask AI" action; this method itself still enforces the
     * confidence gate so it's a no-op (returns [deterministic] unchanged) if
     * the deterministic result was already confident enough.
     */
    suspend fun resolveWithAiFallback(
        rawMerchantText: String,
        amountMinor: Long,
        currencyCode: CurrencyCode,
        deterministic: CategoryResolution,
    ): CategoryResolution {
        if (!confidenceGate.shouldConsultAi(AICapability.MERCHANT_CLASSIFICATION, deterministic.confidence)) {
            return deterministic
        }
        val context = aiContextBuilder.merchant(
            rawMerchantText = rawMerchantText,
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            existingMerchantMatches = merchantRegistry.profiles.map { it.canonicalName },
            knownCategories = MerchantCategory.entries.map { it.name },
        )
        return when (val result = aiOrchestrator.requestSuggestion(AICapability.MERCHANT_CLASSIFICATION, context)) {
            is AIOrchestratorResult.Suggested -> CategoryResolution(
                category = result.suggestion.fields["category"] ?: deterministic.category,
                subcategory = null,
                confidence = result.suggestion.confidencePercent,
                reason = "AI (${result.providerId}/${result.model}): ${result.suggestion.reason}",
                source = CategorySource.AI_SUGGESTION,
            )
            // AiDisabled / NoProviderConfigured / MissingApiKey / Failed — AI never becomes
            // the source of truth, so any non-success outcome just keeps the deterministic answer.
            else -> deterministic
        }
    }

    private fun categoryForRelationship(type: RelationshipType): Pair<String, String>? = when (type) {
        RelationshipType.CASH_WITHDRAWAL -> MerchantCategory.FINANCE.name to "Relationship Engine identified this as an ATM cash withdrawal"
        RelationshipType.CREDIT_CARD_PAYMENT, RelationshipType.CONFIRMATION_OF_PAYMENT -> MerchantCategory.FINANCE.name to "Relationship Engine identified this as a credit card payment"
        RelationshipType.LOAN_REPAYMENT, RelationshipType.INSTALLMENT_PAYMENT -> MerchantCategory.FINANCE.name to "Relationship Engine identified this as a loan/EMI repayment"
        RelationshipType.INTEREST_CREDIT -> MerchantCategory.FINANCE.name to "Relationship Engine identified this as interest/profit credited"
        else -> null
    }
}

package com.sherif.ledger.feature.ai.validation

import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.AISuggestion
import com.sherif.ledger.feature.merchant.MerchantCategory
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AIValidationResult {
    data object Valid : AIValidationResult
    data class Invalid(val reason: String) : AIValidationResult
}

/**
 * RC6's "AI Validation Layer" — every suggestion `AIOrchestrator` gets back
 * passes through here before it's returned as [AIOrchestratorResult.Suggested].
 * This is deliberately structural/sanity validation (well-formed confidence,
 * a real category name, no missing required field) — it does NOT and cannot
 * validate financial correctness (whether a suggested category is the RIGHT
 * one), because nothing here has authority over Financial Truth. A future
 * commit step (Phase C, not built) is what would decide whether to actually
 * accept a *valid* suggestion, and that decision belongs to the deterministic
 * engine, never here.
 */
@Singleton
class AISuggestionValidator @Inject constructor() {

    fun validate(capability: AICapability, suggestion: AISuggestion): AIValidationResult {
        if (suggestion.confidencePercent !in 0..100) {
            return AIValidationResult.Invalid("confidencePercent out of range: ${suggestion.confidencePercent}")
        }
        if (suggestion.reason.isBlank()) {
            return AIValidationResult.Invalid("reason was blank")
        }
        return when (capability) {
            AICapability.MERCHANT_CLASSIFICATION -> validateMerchantClassification(suggestion)
            AICapability.DUPLICATE_DETECTION -> validateBooleanField(suggestion, "isDuplicate")
            AICapability.FALSE_POSITIVE_REVIEW -> validateBooleanField(suggestion, "isRealTransaction")
            else -> AIValidationResult.Valid // Other capabilities' fields are free-form text (relationshipType, summary, etc.) — nothing further to structurally check yet.
        }
    }

    private fun validateMerchantClassification(suggestion: AISuggestion): AIValidationResult {
        val category = suggestion.fields["category"] ?: return AIValidationResult.Invalid("missing 'category' field")
        val validCategory = runCatching { MerchantCategory.valueOf(category.uppercase().replace(' ', '_')) }.isSuccess
        if (!validCategory) return AIValidationResult.Invalid("'$category' is not a recognized MerchantCategory")
        if (suggestion.fields["merchant"].isNullOrBlank()) return AIValidationResult.Invalid("missing 'merchant' field")
        return AIValidationResult.Valid
    }

    private fun validateBooleanField(suggestion: AISuggestion, key: String): AIValidationResult {
        val value = suggestion.fields[key] ?: return AIValidationResult.Invalid("missing '$key' field")
        if (value.lowercase() != "true" && value.lowercase() != "false") {
            return AIValidationResult.Invalid("'$key' was not a boolean: $value")
        }
        return AIValidationResult.Valid
    }
}

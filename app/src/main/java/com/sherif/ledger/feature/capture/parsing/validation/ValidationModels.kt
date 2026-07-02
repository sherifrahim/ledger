package com.sherif.ledger.feature.capture.parsing.validation

data class ValidationResult(
    val fixtureId: String,
    val bank: String,
    val type: String,
    val isSuccess: Boolean,
    val errors: List<String> = emptyList(),
    val wasIgnored: Boolean = false
)

data class ParserStats(
    val bank: String,
    val total: Int,
    val success: Int,
    val ignored: Int,
    val failed: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
    val byType: Map<String, TypeStats> = emptyMap()
) {
    val accuracy: Float get() = if (total > 0) success.toFloat() / total else 0f
}

data class TypeStats(
    val total: Int,
    val success: Int
)

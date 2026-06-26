package com.sherif.ledger.core.model

/**
 * Validation outcome that can carry multiple errors at the same time.
 */
sealed interface ValidationResult {
    /** Indicates that validation passed. */
    data object Valid : ValidationResult

    /** Indicates that validation failed with one or more errors. */
    data class Invalid(val errors: List<ValidationError>) : ValidationResult {
        init {
            require(errors.isNotEmpty()) { "Invalid validation result requires at least one error." }
        }
    }

    companion object {
        /** Creates an invalid result from one or more validation errors. */
        fun invalid(errors: Collection<ValidationError>): ValidationResult = Invalid(errors.toList())
    }
}

/**
 * Strongly typed validation error message.
 */
@JvmInline
value class ValidationError(val message: String) {
    init {
        require(message.isNotBlank()) { "Validation error message cannot be blank." }
    }

    override fun toString(): String = message
}

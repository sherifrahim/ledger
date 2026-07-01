package com.sherif.ledger.core.domain.model

/**
 * Origin of the captured financial data.
 */
enum class IngestionSource {
    SMS,
    MANUAL,
    CSV,
    OCR,
    BANK_API
}

package com.sherif.ledger.feature.capture.extraction

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope

/**
 * The single extension seam of the ingestion pipeline. Name describes WHAT it
 * does, not HOW. Today: deterministic parser + heuristic engine. Tomorrow: an
 * on-device model (Gemma/Phi/Qwen). Adding one = one class + one DI binding.
 * Nothing else changes.
 */
interface FinancialExtractor {
    val name: String
    fun canAttempt(envelope: NotificationEnvelope): Boolean
    suspend fun extract(envelope: NotificationEnvelope): ExtractionResult
}

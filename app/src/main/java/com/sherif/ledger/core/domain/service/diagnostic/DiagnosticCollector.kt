package com.sherif.ledger.core.domain.service.diagnostic

/**
 * One diagnostic's output — either structured JSON (when structure matters:
 * account lists, relationship summaries, database health) or chronological
 * log-format text (when replay order matters: the log buffer itself). The
 * bundle generator writes [json] sections as `<id>.json` and [logText]
 * sections as `<id>.log`, unchanged, directly into the zip.
 */
sealed interface DiagnosticSection {
    val id: String

    data class Json(override val id: String, val json: String) : DiagnosticSection
    data class LogText(override val id: String, val logText: String) : DiagnosticSection
}

/**
 * A single diagnostic's collection logic. Each collector owns exactly one
 * concern and knows nothing about the others or about bundling — the bundle
 * generator just runs every collector in the injected set and writes out
 * whatever each one returns. Adding a new diagnostic later (Split, Notes,
 * Budgets, whatever comes next) means adding one new collector to the Hilt
 * multibinding set, not touching this interface or the generator.
 */
interface DiagnosticCollector {
    val id: String
    suspend fun collect(): DiagnosticSection
}




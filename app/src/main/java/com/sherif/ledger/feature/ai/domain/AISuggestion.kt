package com.sherif.ledger.feature.ai.domain

import kotlinx.serialization.Serializable

/**
 * RC5 Part 10/11 — what a provider is allowed to return. Deliberately
 * generic across ALL capabilities (a flat string field map) rather than one
 * bespoke response class per capability: new capabilities plug in without a
 * new response type, and the shape is simple enough that every provider can
 * be instructed to emit exactly this JSON, regardless of vendor.
 *
 * This is an OPINION, never a write. Nothing in this file — or anywhere in
 * feature/ai — touches TransactionRepository, AccountRepository, or any
 * other persistence boundary. A future validation layer (Phase C, not built
 * yet) is what would ever turn one of these into a committed change, and
 * only after the deterministic engine agrees.
 */
@Serializable
data class AISuggestion(
    val fields: Map<String, String> = emptyMap(),
    val confidencePercent: Int,
    val reason: String,
)

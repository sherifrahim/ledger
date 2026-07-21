package com.sherif.ledger.regression

/**
 * RC9 Phase D — Regression Corpus infrastructure. Deliberately broader than
 * the existing `benchmark/CorpusFixture` (which is extraction-accuracy-only:
 * amount/currency/merchant/type/cardTail). This entry shape spans the WHOLE
 * pipeline (institution → merchant → category → relationship → account →
 * balance effect → duplicate status → forecast) and is source-agnostic —
 * `sourceType` matches `IngestionSource` string values (`SMS`, `NOTIFICATION`,
 * and the currently-unproducer'd `CSV`/`MANUAL`/`EMAIL`/`BANK_API`), so a
 * future source gets its own fixture directory, never a schema change here.
 *
 * Every `expected*` field is nullable and independently optional — an entry
 * only needs to populate the fields it wants validated. [PipelineCorpusRunner]
 * documents exactly which fields it can validate today vs. which require
 * multi-transaction integration fixtures this single-message shape can't
 * express yet (relationship/duplicate/balance/forecast) — see its own doc
 * comment. This is infrastructure, not a claim that everything is wired.
 */
data class PipelineCorpusEntry(
    val id: String,
    /** Matches `IngestionSource.name` — "SMS", "NOTIFICATION", "CSV", "MANUAL", "EMAIL", "BANK_API". */
    val sourceType: String,
    val rawText: String,
    /** The SMS sender ID or Android package name — what `NotificationEnvelope.packageName` would carry. */
    val senderOrPackage: String,
    val expectedInstitution: String? = null,
    val expectedMerchant: String? = null,
    val expectedCategory: String? = null,
    val expectedRelationshipType: String? = null,
    val expectedAccountHint: String? = null,
    val expectedBalanceEffectMinor: Long? = null,
    /** "New" / "Updated" / "Duplicate" / "Ignored" — matches `ReconciliationResult`'s sealed subtypes by name. */
    val expectedDuplicateStatus: String? = null,
    /** Free text, not a structured assertion — forecast behavior (e.g. "should appear as an upcoming bill") isn't a single deterministic value the way the fields above are. */
    val expectedForecastNote: String? = null,
    /** Same convention as `CorpusFixture.knownGap` — the runner counts these against accuracy but never fails the suite on them. */
    val knownGap: Boolean = false,
    val gapNote: String? = null,
) {
    val label: String get() = "[$sourceType] $id: ${rawText.take(48)}"
}

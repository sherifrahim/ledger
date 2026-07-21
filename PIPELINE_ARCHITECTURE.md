# Pipeline Architecture — Stage-by-Stage Audit

RC9 Phase B. Every stage below is documented from the real implementation
file(s), not the conceptual pipeline description in any spec. **The real
orchestrator is `core/domain/usecase/transaction/ProcessNotificationUseCase.kt`**
— read it end to end before trusting any summary, including this one, since
it is the one place the true order of operations is enforced.

## The real order of operations

```
Notification/SMS
  → NotificationFilter (gate)
  → ExtractionRegistry (ALWAYS runs — produces data, decides nothing)
  → DeterministicFinancialIntentClassifier (SOLE routing authority)
  → Router (the only place a behavioral decision is made, from intent alone)
       FINANCIAL_CONFIRMATION → ConfirmationMatcher/ConfirmationInterpreter, never persists
       FINANCIAL_INFORMATION  → discarded, diagnostics only
       UNKNOWN                → discarded, diagnostics only
       FINANCIAL_EVENT        → continues below
  → ReconciliationEngine (New / Updated / Duplicate / Ignored)
  → [New only] DeterministicAccountIdentityResolver (institution + account identity)
  → InsertTransactionUseCase (the ONLY write — brandId/categoryId resolved inline here too)
```

Everything below this line is **read-time only** — never invoked during live
ingestion. Confirmed directly: `ProcessNotificationUseCase.kt`'s own code
calls `tracer.recordStageNotExecuted(PipelineStage.MERCHANT_RESOLVER, "Not
invoked during live ingestion")` and the same for `RELATIONSHIP_ENGINE`,
unconditionally, on every single run. This is a deliberate, load-bearing
architectural line, not an oversight: category, canonical merchant name,
relationship inference, recurring schedules, and forecasts are all computed
fresh whenever a screen opens, from persisted `Transaction.rawText`/history —
never cached at capture time, never risking staleness.

```
[on-demand, at read time, per screen]
  MerchantResolver (feature/merchant) → CategoryIntelligenceEngine / GetFinancialAnalyticsUseCase's inline chain
  → RelationshipEngine.analyze() → RecurringScheduleAnalyzer → ForecastEngine
  → AccountBalanceService (balance replay, independent of the above)
  → GetFinancialAnalyticsUseCase (shapes all of the above for a screen)
```

## Stage detail

### 1. Filter — `NotificationFilter.kt`
- **In**: `NotificationEnvelope`. **Out**: `FilterResult` (`Accepted(reason)`/`Rejected(reason)`).
- **Confidence**: none — binary admit/reject.
- **Reason**: yes, always — every accept/reject carries a human-readable reason string, consumed by `PipelineTracer`.
- **Fallback**: fails OPEN on ambiguity — SMS is always accepted; a known financial package is accepted if it shows ANY financial signal; anything else needs a currency-anchored amount + transaction vocabulary. Its own doc comment: "the real gate is the downstream extractor+validator+registry... this filter's only job is to cheaply discard obvious non-financial noise."
- **AI**: none.
- **Failure mode called out in the code**: the "Jul 2026 regression" — gating solely on a hand-maintained package allowlist silently dropped real transactions when a bank's package name drifted (ADCB shipped as `com.adcb.nexgen`, not the assumed `com.adcb.mobileapp`). Fixed by making content-based admission the primary path and the package list a "fast-path hint," not a hard gate.

### 2. Parsing — `ParserRegistry.kt`, `AdcbParser.kt`, `GenericBankParser.kt`
- **In**: `NotificationEnvelope`. **Out**: `ParseResult` (`Success(candidate)`/`Ignore`/`Failed(reason)`).
- **Confidence**: not at this layer (parsing is pattern-match/no-match, not scored) — confidence is introduced one layer up, in extraction.
- **Reason**: `Failed` carries a reason string; `Ignore` does not explain why (a real, minor gap — an OTP/statement message is silently ignored with no diagnostic trail at the parser layer, though `NotificationFilter`/`ExtractionRegistry` layers around it do log).
- **Fallback**: `AdcbParser` (priority 0, exact package/sender match) tried first; `GenericBankParser` (priority 100, `supports()` always `true`) catches everything else via content shape (currency-anchored amount + card/account tail), registered explicitly AFTER `AdcbParser` so ADCB's specific patterns win when both would match.
- **AI**: none.
- **Failure mode**: confirmed via `MERCHANT_ARCHITECTURE.md`'s investigation and RC7 — `AdcbParser`'s own package constant had drifted stale independently of `InstitutionRegistry`/`NotificationFilter` (fixed in RC7); this class of bug (one bank identity fact duplicated in N places) recurred FOUR times across the codebase before RC7 unified it — see `InstitutionRegistry.kt`'s own doc comment for the full list.

### 3. Extraction — `ExtractionRegistry.kt`
- **In**: `NotificationEnvelope`. **Out**: `ExtractionOutcome` (`Success(candidate, diagnostics)`/`Ignored`/`Failed`/`Confirmation`), always paired with `List<ExtractionDiagnostics>` — one entry per extractor attempted, not just the winner.
- **Confidence**: yes, per extractor (`ExtractionConfidence.value`), and a real **contested-result penalty**: if the top two extractors disagree on amount or type, the winner's confidence is docked 30 points before being accepted (`disagreementPenalty`), and the result is re-validated at the reduced score — a genuine "AI-style" confidence mechanism implemented deterministically.
- **Reason**: yes — `reasoning: List<String>`, `positiveEvidence`/`negativeEvidence`, `rejectedReason`, `confidenceBreakdown` all populated per attempt.
- **Fallback**: runs EVERY registered `FinancialExtractor` that claims `canAttempt()`, ranks by confidence, never returns early on the first match — this is the one stage explicitly designed to consider multiple competing interpretations rather than a single deterministic path.
- **AI**: none today — but this is the most AI-shaped deterministic stage in the pipeline (competing candidates ranked by confidence, with a disagreement penalty) — worth noting as the natural seam if extraction confidence ever needs an AI tie-breaker.
- **Failure mode called out in the code**: an extractor throwing is caught and demoted to `NotApplicable` rather than crashing the pipeline (`runCatching` around every `extractor.extract()` call) — a real defensive measure, not hypothetical.

### 4. Intent Classification — `DeterministicFinancialIntentClassifier.kt`
- **In**: `NotificationEnvelope` + the full `ExtractionOutcome` from stage 3. **Out**: `FinancialIntentResult` (intent + confidence + reasoning + matchedSignals).
- **Confidence**: yes, explicit per branch (95/90/88/85/70/30/0 depending on which tier fired) — never a placeholder value.
- **Reason**: yes, always — every branch returns `reasoning: List<String>`.
- **Fallback**: a genuinely 6-tier deterministic cascade, in priority order: (1) confirmation phrase without movement verb, (2) pure information phrase, (3) movement verb present, (4) extraction registry's own (separately-maintained) confirmation match, (5) extraction produced a valid candidate with no decisive phrase either way, (6) UNKNOWN. Tiers 1-3 are the classifier's OWN independent text reading and are explicitly documented as PRIMARY over the extractor's opinion — its own doc comment states this is "the double-count fix": the classifier decides routing from meaning, never from whether extraction happened to succeed.
- **AI**: none — but the doc comment explicitly names this as the future AI seam: "A future Gemma/Phi model implements the same `FinancialIntentClassifier` and replaces this class without touching downstream code." This is the SOLE routing authority in the entire live pipeline — the single highest-leverage point where a future on-device model could be substituted with zero blast radius elsewhere, by design.
- **Failure mode called out in the code**: "Phase 9 (Bug 1)" comment — a credit-card-payment confirmation message was previously falling through to the extraction-success fallback tier (5) and being misread as a NEW event instead of an acknowledgement of an old one; fixed by expanding the confirmation phrase list, but this documents a real, previously-shipped classification bug.

### 5. Institution Resolution — `InstitutionRegistry.kt`
Covered in full in `FINANCIAL_ENGINE.md`. Summary: exact-key then alias-substring match on `NotificationEnvelope.packageName` (which doubles as SMS sender ID). **Confidence**: implicit binary (recognized or not) — no partial-confidence institution match exists. **Reason**: none returned directly (the caller, `DeterministicAccountIdentityResolver`, constructs its own evidence trail around the null/non-null result). **Fallback**: `null` on no match — never a guess. **AI**: none.

### 6. Account Resolution — `DeterministicAccountIdentityResolver.kt`
Covered in full in `FINANCIAL_ENGINE.md`. Summary: 4 possible decisions (`BOUND_EXISTING`/`CREATED_NEW`/`FALLBACK_DEFAULT`/`CANDIDATE`), each with an explicit `confidence: Int` and an `evidence: List<String>` trail. **Reason**: yes, always. **Fallback**: never merges an unrecognized institution into an unrelated account (RC7); checks `LearnedDecisionStore` before creating a redundant Candidate Account (RC8). **AI**: none — explicitly documented as "no AI, every decision traces to explicit, listed evidence," with the interface (`AccountIdentityResolver`) designed so a future model-assisted implementation could substitute in.

### 7. Reconciliation / Duplicate Detection — `ReconciliationEngine.kt`
- **In**: `TransactionCandidate` + nearby existing `Transaction`s (±24h window). **Out**: `ReconciliationResult` (`New`/`Updated`/`Duplicate`/`Ignored`).
- **Confidence**: yes, a 0-100 score (`ScoreResult.score`) — exact fingerprint match short-circuits to 100/`Duplicate` immediately; otherwise weighted scoring (amount+currency hard-gate at 0 on mismatch, then up to 100: amount 40, merchant 30, tail 30, time proximity up to 20, type 10).
- **Reason**: yes internally (`ScoreResult.details`, a comma-joined breakdown), but this detail is **only logged** (`LedgerLogger.d`), never returned to the caller or exposed structurally — confirmed in `MERCHANT_ARCHITECTURE.md`'s investigation; this is a real, if minor, explainability gap (see Phase C below).
- **Fallback**: `< 90` → `New`; `90-97` → `Updated`; `≥98` → `Duplicate`. Deliberately compares candidates across `IngestionSource` values (SMS vs NOTIFICATION) — same real-world bank event often arrives through both channels with differently-worded merchant text, so merchant match is supporting evidence, never the sole determinant.
- **AI**: none.
- **Failure mode called out in the code**: an accepted, documented trade-off — "two genuinely different transactions on the same card, for the same amount, within a minute of each other, can score high enough to be merged... judged rarer and less harmful than the duplicate-insertion bug this fixes."

### 8. Persistence — `InsertTransactionUseCase.kt`
- **In**: `Params` (account, amount, currency, type, timestamp, source, raw text, etc.). **Out**: `LedgerResult<Transaction>`.
- **Confidence/Reason**: none at this stage — purely mechanical (validate → fingerprint → resolve brand/category → atomic insert). This is intentional: by this point every upstream decision (intent, account, reconciliation) has already been made; this stage's only job is to execute it atomically.
- Also resolves `brandId`/`categoryId` inline here via System B (`core/domain/service/transaction/{MerchantResolver,CategoryResolver}`) — see `MERCHANT_ARCHITECTURE.md` for why `categoryId` is always null and `brandId` is a real but crude exact-text merchant grouping.
- **Fallback**: input/account validation failures return `LedgerResult.Failure` before any write; the insert itself is wrapped in `transactionRunner.runInTransaction` for atomicity.
- **AI**: none. Never mutates any account balance — "the persisted transaction IS the complete effect of this insert," per its own doc comment.

### 9. Merchant Resolution (read-time) — `feature/merchant/MerchantResolver.kt`
Covered in full in `INTELLIGENCE_ENGINE.md`/`MERCHANT_ARCHITECTURE.md`. **Confidence**: yes (`MerchantResolution.Resolved.confidence`). **Reason**: yes (RC8 addition). **Fallback**: title-cased raw text, never invented. **AI**: none at this tier.

### 10. Category Resolution (read-time) — `CategoryIntelligenceEngine.kt` (new, RC8) and `GetFinancialAnalyticsUseCase`'s inline chain (older, still live, intentionally duplicated — see `MERCHANT_ARCHITECTURE.md`)
Both run ONLY when a screen requests analytics/category data — never during ingestion. **Confidence/Reason/Source**: `CategoryIntelligenceEngine` returns all three explicitly per tier; the older inline chain in `GetFinancialAnalyticsUseCase` returns a bare category `String`, no confidence/reason (a real, documented asymmetry — the newer engine is strictly more explainable, kept separate rather than risking the frozen hot path). **AI**: `CategoryIntelligenceEngine.resolveWithAiFallback` only, gated by `ConfidenceGate`, on-demand only (Intelligence Inspector's "Ask AI"), never in the inline chain.

### 11. Relationship Resolution (read-time) — `RelationshipEngine.kt`
Runs once per analytics/balance computation, over the FULL bounded transaction set for that computation (never per-transaction). **Confidence/Reason**: yes, rich — `RelationshipConfidence` (banded) + `reasoning: List<String>` + full `RelationshipDiagnostics` per relationship, covered exhaustively in `INTELLIGENCE_ENGINE.md`. **Fallback**: 9 resolvers, each independently scored; no match = no relationship, never invented. **AI**: none — pure deterministic, read-only, frozen.

### 12. Recurring / Forecast (read-time) — `RecurringScheduleAnalyzer.kt`, `ForecastEngine.kt` (both new, RC8)
Both on-demand (Intelligence Inspector, and any future screen that calls them — none does yet, see `INTELLIGENCE_ENGINE.md`'s Suggested RC9). **Confidence**: yes, variance-based for schedules; the forecast itself doesn't carry a single confidence number (it's a projection built FROM already-confidence-scored schedules, not scored itself — a real, minor gap, noted below). **Reason**: implicit (frequency + gap data), not a prose string — another real, minor explainability gap. **AI**: none, by design (spec: "No AI required initially").

### 13. Balance Computation (read-time, independent of the above) — `BalanceCalculator.kt`, `AccountBalanceService.kt`
The ONLY source of balance truth, replays every persisted transaction on every call — never cached. **Confidence/Reason**: not applicable — this is arithmetic, not inference; correctness is enforced structurally (currency guards, liability sign rules) rather than scored. **Fallback**: a currency-mismatched transaction contributes zero effect, logged as an error (RC6) rather than silently corrupting the sum. **AI**: none, and by design never will be — this is Financial Truth, explicitly outside the intelligence layer's authority (see `FINANCIAL_ENGINE.md`'s AI Boundaries).

### 14. Dashboard / Analytics Display — `GetFinancialAnalyticsUseCase.kt`
The single entry point every screen (Dashboard, Insights, Accounts, Transactions) consumes — confirmed via its own doc comment and by reading `presentation/dashboard/DashboardViewModel.kt`'s injected dependencies. Shapes `AccountBalanceService`'s replayed balances (never recomputes arithmetic itself, per RC7's fix — see `FINANCIAL_ENGINE.md`) plus its own category/merchant/relationship aggregation for the period requested.

## Explainability gaps found by this audit (feed directly into Phase C)

1. `ReconciliationEngine`'s per-pair score breakdown (`ScoreResult.details`) is computed but only logged, never returned structurally — a real gap, not fixed here (frozen file, scoring specifically; see `MERCHANT_ARCHITECTURE.md`'s reasoning for why this wasn't touched).
2. `GetFinancialAnalyticsUseCase`'s inline category chain returns a bare `String`, no confidence/reason — `CategoryIntelligenceEngine` (RC8) is strictly better but intentionally not wired into that hot path.
3. `RecurringSchedule`/`ForecastResult` don't carry a prose "reason" string, only structured fields (frequency, gap, variance) — a human has to interpret the numbers.
4. Parser-level `Ignore` (OTP/statement) doesn't carry a reason string at the parser layer (though upstream `NotificationFilter`/downstream `ExtractionRegistry` diagnostics partially compensate).

# Ledger Intelligence Engine — Canonical Specification

This document is the canonical specification for Ledger's Intelligence Layer —
the deterministic-first, AI-assisted system that turns already-true financial
facts (see `FINANCIAL_ENGINE.md`) into financial *meaning*. Read
`FINANCIAL_ENGINE.md` first: everything here assumes Financial Truth
(balances, accounts, currencies, reconciliation) is already settled and never
touches it.

**Ledger is not an AI expense tracker. It is a deterministic financial engine
with an intelligence layer.** The engine always owns balances, accounts,
reconciliation, currencies, and transfers. The intelligence layer owns
merchant understanding, category suggestions, duplicate detection, recurring
detection, relationship inference, and forecasting. AI may assist the
intelligence layer. AI never becomes the source of truth for either layer.

## Merchant Engine

`feature/merchant/` is the deterministic merchant knowledge base:
`MerchantRegistry` (a curated list of `MerchantProfile`s — canonical name,
aliases, category, optional subcategory, brand color, logo asset slot,
website, country, confidence) and `MerchantResolver` (alias matching:
exact-token first, then longest-substring). RC8 added a `reason: String` to
every `MerchantResolution` (both `Resolved` and `Unresolved`) — this was the
one real gap: confidence already existed, an explanation of *why* did not.

**A known duplication, documented rather than silently carried**: there are
two unrelated merchant/category systems in this codebase.
- **System A** (`feature/merchant/`) — deterministic, in-memory, alias-based.
  This is what `GetFinancialAnalyticsUseCase` actually uses for category/
  merchant display in analytics and what the Intelligence Inspector shows.
- **System B** (`core/domain/service/transaction/MerchantResolver`,
  `core/database/entity/{BrandEntity,CategoryEntity}`) — DB-backed, wired into
  `InsertTransactionUseCase` (the real write path), but largely a stub:
  `CategoryResolver.resolve()` always returns `null` (its own comment
  explains why — the `categories` table is never seeded), and
  `MerchantResolver.resolve()` (System B) just auto-creates a generic `Brand`
  row (`brandKey = "manual"`) for anything unrecognized.

These two systems do not talk to each other. `Transaction.brandId`/
`categoryId` (System B) are not what analytics/category totals are computed
from (System A, from `Transaction.rawText`, at read time). RC8 deliberately
did **not** attempt to merge or untangle these — that is real, sizable,
separate work (see "Suggested RC9"), and RC8's instruction was explicitly to
avoid unnecessary complexity. Both systems continue to work exactly as they
did before RC8; nothing here changes which one wins for which consumer.

## Learning Engine

Two learned-memory stores exist, deliberately kept separate:

- **`LearnedMerchantCategoryStore`** (pre-existing, RC5) — merchant-text →
  user-taught `MerchantCategory`, written from the Review Inbox. Unchanged
  this RC.
- **`LearnedDecisionStore`** (new, RC8) — a generic `(decisionType,
  subjectKey) -> learnedValue` table (`learned_decisions`, migration 9→10).
  Generic on purpose: a future decision type (transfer confirmation,
  relationship correction, recurring-schedule correction) is a new
  `DecisionType` string constant, never a new migration or a new store class.

**The one real, wired-up learning flow this RC adds**: promoting a Candidate
Account (RC7's `PromoteCandidateAccountUseCase`) now also calls
`learnedDecisionStore.learn(DecisionType.INSTITUTION, rawIdentifier,
confirmedName)`. `DeterministicAccountIdentityResolver` checks this store
**before** parking another Candidate Account for an institution
`InstitutionRegistry` still doesn't recognize — if the user already confirmed
this exact raw sender/package once, the resolver binds directly to that
confirmed account instead of creating a second, redundant candidate. This is
the concrete shape of "Ledger must learn" the RC8 spec asked for: a decision
a user made once is never asked again, and it's checked **before** any AI
call would ever be considered (there is no AI involved in this loop at all —
it doesn't need to be, deterministic memory is sufficient).

## Category Engine

`CategoryIntelligenceEngine` (new, RC8) implements the exact resolution order
the spec asked for, every tier returning category + subcategory + confidence
+ reason + source (never a bare string):

1. **User-confirmed memory** — `LearnedMerchantCategoryStore`.
2. **Merchant defaults** — `MerchantResolver` (System A), including the new
   `subcategory` field where a profile has one.
3. **Relationship/institution hints** — if the caller already knows this
   transaction's `RelationshipType` (from a `RelationshipEngine.analyze()`
   pass it ran itself — this engine never re-runs relationship analysis), a
   handful of relationship types map directly to `FINANCE`: cash withdrawal,
   credit card payment, loan/EMI repayment, interest credit.
4. **Deterministic keyword rules** — `GenericCategoryKeywords`.
5. **AI suggestion** — `resolveWithAiFallback()`, the ONE place this engine
   calls `AIOrchestrator`, gated by `ConfidenceGate` so it only fires when
   tiers 1-4 left real uncertainty, and only ever called on demand (today:
   the Intelligence Inspector's "Ask AI" button) — never automatically, never
   from a hot path.

**Deliberately a new, standalone engine, not a refactor of
`GetFinancialAnalyticsUseCase`'s existing inline 4-tier chain.** That use case
is a frozen file (demonstrated-bug-only, per `CLAUDE.md`) and this is a real
enrichment, not a fix — refactoring it to delegate here would have been a
nice-to-have carrying real risk to the analytics hot path for no correctness
gain. The two chains are intentionally the same 4 deterministic tiers today
(a small, accepted, documented duplication); `CategoryIntelligenceEngine` is
free to evolve independently (subcategory, relationship hints, AI fallback)
without that risk.

## Relationship Engine

Unchanged this RC — `RelationshipEngine`/`RelationshipResolvers.kt` remain
frozen (demonstrated-bug-only). They already compute everything the RC8 spec
asked for per relationship: `RelationshipConfidence` (0-100, banded HIGH/
MEDIUM/LOW) and `reasoning: List<String>`, plus a full `RelationshipDiagnostics`
(matched transaction ids, time/amount deltas, merchant/card/account match
flags, decision string). This was simply never *surfaced* anywhere before RC8
— the Intelligence Inspector's Relationship section is the first place a
human (developer, for now) can see it.

**Coverage confirmed against the RC8 spec's list**: Salary
(`SALARY_FUNDS_EXPENSE`), Credit card payment (`CREDIT_CARD_PAYMENT` +
`CONFIRMATION_OF_PAYMENT`), Account transfer (`TRANSFER_BETWEEN_ACCOUNTS`),
Refund (`REFUND_OF_PURCHASE`), Cash withdrawal (`CASH_WITHDRAWAL`),
Investment (`INVESTMENT_CONTRIBUTION`, folded into
`TransferBetweenAccountsResolver` rather than a dedicated resolver), Loan
payment (`LOAN_REPAYMENT`/`INSTALLMENT_PAYMENT`), Subscription
(`SUBSCRIPTION`) — all implemented. "Internal transfer" has no literal enum
value of that name; `TRANSFER_BETWEEN_ACCOUNTS`/`SAVINGS_MOVEMENT` cover it.
**`RelationshipType.UNKNOWN_RELATIONSHIP` is a confirmed-dead enum value** —
zero resolvers, zero other references anywhere — a real RC9 cleanup
candidate, not touched this RC (removing it is a one-line, zero-risk change,
but out of RC8's stated scope).

## Subscription & Recurring Engine

`RecurringScheduleAnalyzer` (new, RC8) computes what `RelationshipEngine`'s
existing resolvers never did: a forward-looking schedule. It does NOT
reimplement recurrence detection — for subscriptions, recurring bills,
recurring merchants, and loan EMIs, it unions `RelationshipEngine`'s
already-detected pairwise relationships back into full time series (by
transaction id), then computes `RecurrenceFrequency` (WEEKLY/MONTHLY/
QUARTERLY/YEARLY/IRREGULAR, from average gap), `lastOccurrence`,
`nextExpectedDate` (last + average gap), and a variance-based `confidence`
(tighter, more numerous occurrences score higher — never a flat number).

Salary and Rent have no existing `RelationshipEngine` resolver (confirmed
before writing this — `SalaryFundsExpenseResolver` pairs salary WITH the
expenses it funds, it does not detect salary recurring against itself), so
those two use a small, separately-scoped keyword grouping inside
`RecurringScheduleAnalyzer` itself — same salary keywords
`SalaryFundsExpenseResolver` already uses (duplicated, not shared, since that
resolver's list is private and inside a frozen file), plus a new rent-keyword
list. Neither touches `RelationshipEngine`/`RelationshipResolvers.kt`.

## Duplicate Engine

Unchanged this RC — `ReconciliationEngine` (frozen: scoring specifically) was
not touched. Confirmed before writing this document, by reading the actual
scoring function: reconciliation already operates across `IngestionSource`
values (SMS vs NOTIFICATION), by design — its own doc comment states the
rationale (the same bank event often arrives through two independent
channels). CSV and Manual entry still don't exist as producers (confirmed:
`IngestionSource.CSV`/`.MANUAL` are unused enum placeholders, same finding as
`FINANCIAL_ENGINE.md`'s Phase E), so "SMS+CSV"/"CSV+Manual"/"Manual+
Notification" combinations from the RC8 spec are architecturally ready
(reconciliation doesn't gate on source) but literally unexercisable until a
producer exists — building fake CSV/Manual dedup tests against a producer
that doesn't exist would be fabricated, so none were added.

The Intelligence Inspector surfaces existing duplicate evidence (a
fingerprint-collision count over all transactions) rather than adding new
detection logic — reconciliation's own per-pair scoring breakdown
(`ScoreResult.details`) is private inside the frozen `ReconciliationEngine`
and was deliberately left untouched/unexposed rather than risk the frozen
scoring file for a diagnostics-only visibility improvement.

## Forecast Engine

`ForecastEngine` (new, RC8) — fully deterministic, no AI, per the spec
("No AI required initially"). For a given account: current balance
(`AccountBalanceService.currentBalance`, unchanged), upcoming schedules within
a horizon (from `RecurringScheduleAnalyzer`, split into projected income —
salary — vs. projected outflow — everything else), an expected balance
(current + projected income − projected outflow), a projected salary date,
and up to 6 months of historical net-flow-per-month (income − expense,
grouped by calendar month).

**"Design interfaces so AI can enhance forecasting later"**: satisfied by
construction, not by new code — `historicalMonthlyNetMinor` is exactly the
shape `AIContextBuilder.forecast(historicalMonthlyTotalsMinor, currencyCode)`
already expects (that builder was built in RC5/RC6 and never wired to
anything). A future RC that wants an AI-narrated forecast summary can call
`AIContextBuilder.forecast(forecastResult.historicalMonthlyNetMinor,
forecastResult.currencyCode)` directly — no interface change needed. RC8
deliberately does NOT make that call itself (constraint: no new AI features).

## AI Boundaries

Unchanged, restated as canonical (see also `FINANCIAL_ENGINE.md`'s AI
Boundaries section): AI is advisory-only, never a write path. RC8 adds
exactly ONE new AI call site — `CategoryIntelligenceEngine.resolveWithAiFallback`,
invoked only from the Intelligence Inspector's user-triggered "Ask AI"
button, only when `ConfidenceGate.shouldConsultAi` says the deterministic
result was uncertain. That call goes through `AIOrchestrator.requestSuggestion`
completely unchanged from RC5/RC6 — cache → retry → fallback provider →
parse → `AISuggestionValidator` → cache write → `AiAuditLogger` → `AiDebugTraceStore`.
Nothing in RC8 bypasses any stage of that pipeline. No AI suggestion is ever
written back to a `Transaction`/`Account`/`Category` row — the Intelligence
Inspector only *displays* the AI's opinion next to the deterministic one.

## Confidence Model

Every intelligence signal in this codebase now carries an explicit,
never-fabricated confidence:

- Merchant resolution: `MerchantResolution.Resolved.confidence` (from
  `MerchantProfile.knownConfidence`, adjusted for exact-vs-substring match).
- Category resolution: `CategoryResolution.confidence`, tier-dependent —
  100 for learned memory, the merchant's own confidence for registry matches,
  75 for relationship hints, 60 for keyword matches, 0 for `UNKNOWN`, and
  whatever the AI provider returned (itself validated to be in 0..100 by
  `AISuggestionValidator`) for the AI tier.
- Relationship confidence: `RelationshipConfidence` (0-100, banded), computed
  per-relationship by the resolver that found it — unchanged, pre-existing.
- Recurring schedule confidence: `RecurringScheduleAnalyzer`'s variance-based
  score (tighter gaps + more occurrences = higher, 30-95 range, never a flat
  number).
- `ConfidenceGate.shouldConsultAi` is the ONE place any of the above scores
  are compared against a threshold to decide whether AI gets consulted at
  all — per-capability, user-configurable, default 70 (unchanged from RC6).

## Memory Model

Two Room-backed stores, both read into an in-memory cache at startup and
written through immediately on `learn()` (no reload lag on write):
`LearnedMerchantCategoryStore` (`merchant_category_overrides`, RC5, merchant
category only) and `LearnedDecisionStore` (`learned_decisions`, RC8, generic
`decisionType`/`subjectKey`/`learnedValue`/`confidence`). Neither is ever
consulted by an AI call — memory is checked (and, on a hit, is authoritative)
strictly BEFORE any `ConfidenceGate`/`AIOrchestrator` involvement, in both
`CategoryIntelligenceEngine.resolveDeterministic` and
`DeterministicAccountIdentityResolver.resolveLocked`.

## Decision Hierarchy

The one hierarchy every intelligence decision in this codebase now follows,
end to end:

```
User-confirmed memory (LearnedMerchantCategoryStore / LearnedDecisionStore)
        ↓ (miss)
Deterministic engine (MerchantRegistry / InstitutionRegistry / RelationshipEngine / GenericCategoryKeywords)
        ↓ (still uncertain, per ConfidenceGate)
AI suggestion (AIOrchestrator — cache → retry → fallback → validate → audit → debug-trace)
        ↓
Never a silent write — the deterministic engine (or a human, via Developer Console
today; a future Review Inbox flow later) always decides what happens next.
```

## Suggested RC9

1. **Untangle merchant/category System A vs System B** — the confirmed
   duplication documented above. Likely direction: retire System B's
   `CategoryResolver`/`CategoryEntity`/`CategoryDao` (genuinely unseeded,
   unused) in favor of System A, or explicitly seed System B and make it the
   one source of truth — either is a real, scoped decision, not attempted
   here.
2. **Remove `RelationshipType.UNKNOWN_RELATIONSHIP`** — confirmed dead
   (zero resolvers, zero references), a one-line, zero-risk cleanup.
3. **Wire `ForecastEngine`/`CategoryIntelligenceEngine` into a real user-facing
   screen** — both currently only reachable from the debug-only Intelligence
   Inspector. A "Financial Meaning" surface in the main app (upcoming bills
   on the Dashboard, a category confidence indicator on Transaction Details)
   is the actual product outcome this infrastructure was built to enable.
4. **CSV import + manual entry** (carried over from `FINANCIAL_ENGINE.md`) —
   still the prerequisite for exercising the cross-source duplicate detection
   combinations RC8's spec asked about but couldn't test.
5. **RelationshipEngine's Investment/Internal-transfer resolution** — folded
   into `TransferBetweenAccountsResolver` today; worth a dedicated resolver
   if investment tracking becomes a real product priority.

# Ledger Financial Engine — Canonical Specification

This document is the canonical specification for Ledger's financial engine —
the deterministic system that turns raw evidence (SMS, notifications, and
eventually CSV/manual entry) into Financial Truth. It exists so that future
work (RC8 and beyond) can be evaluated against a written contract instead of
tribal knowledge scattered across commit messages and code comments.

**Ledger is not an expense tracker. It is a deterministic financial engine.**
The engine computes Financial Truth. AI, where present, only ever provides
Financial *Meaning* on top of a truth the engine already established —see
"AI Boundaries" below. Correctness always outranks convenience: an unresolved
or ambiguous piece of evidence is surfaced, never guessed into a plausible
answer.

## Financial Truth

Financial Truth is the property that every number the app shows is either:

1. A raw fact directly persisted from evidence (a transaction's amount,
   currency, timestamp), or
2. A pure, deterministic replay of persisted facts through one documented
   rule (an account balance, a net worth figure, a category total).

Nothing is ever a cached/stored "current balance" field, and nothing is ever
invented to fill a gap. Where the engine cannot establish truth (an
unrecognized institution, a currency mismatch, an ambiguous account), it
surfaces that gap explicitly — a Candidate Account, a currency-guard warning,
an excluded-account entry — rather than picking the most likely answer.

The Developer Console's **Balance Inspector** exists specifically to prove
this property continuously: it independently replays the same computation the
Dashboard displays and asserts the two agree
(`BalanceInspectorUiState.mismatchMinor`), rather than asking anyone to trust
that they do.

## Institution Registry

`core/domain/service/account/InstitutionRegistry.kt` is the single source of
truth for "which bank sent this." An `InstitutionIdentity` carries a name, its
default operating currency, its country, and a set of alias substrings used
to recognize it inside a raw Android package name or SMS sender ID (bank SMS
headers vary by carrier/DLT template — e.g. HDFC Bank arrives as `HDFCBK`,
`VM-HDFCBK-S`, `AD-HDFCBKS`, etc. — matching on "contains an alias" instead of
enumerating every header variant is what keeps this extensible without
scattering per-bank regexes through the codebase).

Before RC7, this registry existed but only covered four UAE banks by exact
package name, and — confirmed by direct inspection, not assumed — two other
files (`AdcbParser`, the debug-only `DebugConsoleViewModel`) had independently
drifted, hardcoded copies of overlapping bank-identity data that disagreed
with this registry and with each other. RC7 corrected both to read from this
one source and added India's five largest retail banks (HDFC, ICICI, SBI,
Axis, Kotak). **Adding a new institution is exactly one new map entry here —
never a new parser, never a new regex elsewhere.**

## Account Resolver

`core/domain/service/account/DeterministicAccountIdentityResolver.kt`
implements `AccountIdentityResolver`, the sole authority for "which account
does this transaction belong to." Its decision is always one of:

- **`BOUND_EXISTING`** — high-confidence match (institution + tail, or better)
  against an already-established account.
- **`CREATED_NEW`** — a brand-new account, created only from near-certainty in
  a single observation or several independent observations of the same
  identity signature, never a single moderate-confidence guess.
- **`FALLBACK_DEFAULT`** — the institution IS recognized but there isn't
  enough evidence (usually a missing account/card tail) to bind or create a
  specific account; recorded against the default account, a visible and
  queryable state.
- **`CANDIDATE`** (RC7) — the institution is **not** recognized by
  InstitutionRegistry at all. This transaction is never merged into the
  default account or any other existing account, regardless of currency — it
  is parked in a dedicated Candidate Account (`Account.isCandidate = true`),
  correctly currency-tagged from the transaction's own extracted data,
  excluded from every balance/net-worth figure and from the ordinary Accounts
  screen until a user promotes or dismisses it from Developer Console's
  Balance Inspector.

This closes, at the architectural level, the exact class of bug behind the
confirmed HDFC Bank currency-mixing incident (RC6): an unrecognized
institution's transaction landing in an unrelated-currency account with no
visible trace. Before RC7, `FALLBACK_DEFAULT` was the only outcome for an
unrecognized institution — RC7 does not remove that outcome (it's still
correct when the institution IS known but a tail is missing), it adds a
strictly narrower one for the specific case of genuine non-recognition.

The resolver is `@Singleton` with an internal `Mutex` serializing every
resolve-or-create sequence app-wide (including the default-account
get-or-create inside `EnsureDefaultAccountUseCase`, which shares the identical
race shape) — this is a load-bearing detail, not incidental: without it,
concurrent notification/SMS processing can create duplicate accounts for the
same identity, since account identity has no database-level uniqueness
constraint (unlike transactions, which are protected by a fingerprint index).

## Currency Rules

- Money is always a `Long` in minor units — never `Double`/`Float`.
- Every transaction and every account carries its own `CurrencyCode`; there is
  no implicit "app currency."
- `Money.plus`/`minus`/`compareTo` already refuse to operate across
  currencies (`requireSameCurrency`, throws on mismatch) — this has always
  been true for two-operand arithmetic.
- `BalanceCalculator.effect()` (RC6) refuses to let a transaction whose
  currency doesn't match its account's currency contribute anything —
  logged as an error, contributing zero effect, rather than silently mixing
  units.
- **RC7 closes the same gap at the aggregation layer.** Three independent
  call sites (`AccountBalanceService.netWorth()`,
  `GetFinancialAnalyticsUseCase.computeNetWorth()`,
  `FinancialTraceCollector.buildReport()`) were each summing raw minor units
  across every account regardless of currency, then stamping the result with
  whichever currency the first account in the list happened to have — a real,
  confirmed bug of the exact shape the per-transaction guard above already
  closed, just never closed here. `core/domain/service/transaction/CurrencyGuard.kt`
  is now the single reusable "group by currency, pick a deterministic primary,
  never sum across the boundary" primitive all three use.
- **No exchange-rate conversion happens anywhere in this codebase.** A
  non-primary-currency balance is reported separately (Balance Inspector's
  "Other-Currency Accounts" section), in its own currency, never converted
  into or summed with the primary figure. See "Future Exchange Rate
  Architecture" below for what would need to change to add this.
- "Primary currency" is chosen deterministically — the currency shared by the
  most accounts/items in a given computation, not hardcoded to AED — so this
  works correctly for a user whose accounts are predominantly a different
  currency too.

## Financial Events (architecture prep, not migrated)

The long-term evidence pipeline this engine is growing toward:

```
SMS / Notification / CSV / Manual Entry
        -> Evidence
        -> Relationship Engine
        -> Financial Event
        -> Account Transactions
```

RC7 deliberately does **not** migrate the live pipeline to this shape — per
its own explicit constraint ("do not migrate everything, refactor only enough
to make future migration clean"). The good news, confirmed by reading the
current architecture rather than assumed: the seams this future migration
needs already exist and don't need new code to be ready for it:

- `IngestionSource` (`core/domain/model/IngestionSource.kt`) already
  enumerates `NOTIFICATION`, `SMS`, `CSV`, `MANUAL`, `BANK_API`, `OCR` — CSV
  and manual entry are modeled as first-class evidence sources today, they
  just have no producer yet.
- Every `SourceAdapter` (`feature/capture/source/*`) already wraps a raw
  source so downstream parsing/extraction/reconciliation is source-agnostic —
  a future `CsvSourceAdapter` or `ManualEntrySourceAdapter` would plug in at
  the exact same seam `SmsSourceAdapter`/`NotificationSourceAdapter` use
  today, with zero changes to `ParserRegistry`, `ExtractionRegistry`,
  `ReconciliationEngine`, or `AccountIdentityResolver`.
- `TransactionCandidate.source: IngestionSource` already carries provenance
  all the way through to the persisted `Transaction.source` — a CSV-imported
  or manually-entered transaction is already indistinguishable, structurally,
  from a captured one downstream of extraction.

**What does not exist yet, and is explicitly out of RC7's scope** (per its own
constraints: no CSV import, no manual entry): an actual `FinancialEvent`
domain type, a CSV file parser, a manual-entry screen/ViewModel, and a
`RelationshipEngine`-driven event classification step ahead of persistence
(today `RelationshipEngine` runs read-only, after the fact, for analytics and
liability-payment matching — never during ingestion; see
`ProcessNotificationUseCase`'s own `recordStageNotExecuted` comment). Building
these is real, sizable work for a dedicated RC — see "Suggested RC8."

## Balance Calculation

`AccountBalanceService` is the **only** source of current-balance truth. No
number shown to the user is ever read from a cached field. Every balance is
reproduced by replaying an account's persisted transactions through
`BalanceCalculator.effect()`, starting from `Account.openingBalance`
(itself set once, either at creation or via the one-time
`SeedOpeningBalanceUseCase` correction — never silently mutated afterward).

`RelationshipEngine.analyze()` runs exactly once per balance computation,
shared across every account, to identify credit-card-payment relationships so
a liability account's balance correctly reflects a payment recorded against a
different (paying) account — without ever persisting a cross-reference field.

RC7 adds `AccountBalanceService.candidateBalances()` (Candidate Accounts,
replayed the same way but never mixed into `currentBalances()`/`netWorth()`)
and `nonPrimaryCurrencyBalances()` (real accounts excluded from the primary
net-worth figure purely for currency-safety reasons) — both additive, neither
changes what `currentBalances()`/`netWorth()`/`currentBalance()` return for
any input that existed before RC7.

## Reconciliation

`ReconciliationEngine.reconcile()` decides New / Updated / Duplicate / Ignored
for an incoming candidate against recent existing transactions — exact
fingerprint match first, then fuzzy confidence scoring (amount + currency
hard-gated at 0 on mismatch, merchant text, account/card tail, time
proximity, type). This runs **before** account identity resolution — a
transaction's account is not yet known at reconciliation time, deliberately
(see the engine's own comment), so reconciliation never uses `accountId` as a
matching signal.

## AI Boundaries

Unchanged from RC5/RC6, restated here as canonical: AI is advisory-only. It
never writes to `TransactionRepository`/`AccountRepository`, never modifies a
balance, never creates or deletes a transaction or account, never bypasses
reconciliation or validation. `AIOrchestrator` returns a structured
`AISuggestion` (fields + confidence + reason) that a human-facing screen
(Review Inbox, once wired) or a future capability may act on — never an
automatic write. RC7 introduces no new AI capability and does not wire
`AIOrchestrator` into the live capture pipeline (still explicitly Phase C of
the RC5/RC6 roadmap, still not started).

## Future Exchange Rate Architecture (not built)

RC7 explicitly does not perform currency conversion anywhere. If this is
ever built, the correct seam is `CurrencyGuard`'s `otherCurrencyTotals`/
`GroupedTotals` output — a rate-lookup service could convert those into the
primary currency for *display only*, while the underlying per-currency
figures (and every persisted `Money` value) remain unconverted and exact.
Conversion must never happen before or during persistence, and never
silently — a converted figure shown to a user must always be labeled with
the rate and source used, or Financial Truth (a number that traces to a
documented, deterministic rule) is broken.

## Future Sync Considerations (not built)

Ledger is offline-first with no networking layer beyond RC5/RC6's optional,
disabled-by-default AI provider calls (which require `INTERNET` permission but
never touch account/transaction data). If cloud sync is ever added:

- `Account`/`Transaction` primary keys are local Room auto-increment `Long`s
  — not sync-safe identifiers. A sync-ready ID scheme (UUID, or a
  server-assigned ID reconciled post-insert) would need to be introduced
  before multi-device sync, not layered on top of the current scheme.
- The `Transaction.fingerprint` uniqueness mechanism (the real de-duplication
  guarantee today) would need an explicit statement of how it behaves across
  two devices independently capturing the same real-world SMS — likely
  unchanged (the fingerprint is derived from content, not device-local state),
  but this needs to be verified against real multi-device evidence, not
  assumed.
- Candidate Accounts (RC7) and their promote/dismiss actions are exactly the
  kind of user decision a sync layer would need to reconcile across devices —
  worth designing sync around this concept explicitly rather than retrofitting
  it later.

## Suggested RC8

See the RC7 delivery report (CLAUDE.md's RC7 section) for the full list —
summarized here as the canonical forward-looking pointer: CSV import +
manual entry (the two `IngestionSource` values that exist but have no
producer), a real `FinancialEvent` domain type and pre-persistence
`RelationshipEngine` classification step, structured parser-failure tracking,
and closing the transfer-direction misclassification bug found alongside the
original HDFC currency bug (RC6) — "credited" text still parses as
`OUTGOING` on at least one confirmed real message.

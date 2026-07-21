# ADR-0001 — The Financial Event model

- **Status:** Accepted (product owner authorised Milestone 2)
- **Date:** 2026-07-21
- **Supersedes / relates to:** [ADR-0000 — Brownfield Evolution Strategy](0000-brownfield-evolution-strategy.md); canonical decision **D2** (Financial Event is the canonical domain model, *not* a rename of Transaction) in `LEDGER_MASTER_SPECIFICATION.md`.

## Context

The specification names the **Financial Event** as the canonical domain object of the
platform: the immutable, source-agnostic record that "something financial happened,"
from which balances, story, forecast, relationships and every other engine are derived.

Today the codebase's central record is `Transaction` — a good, working model, but one
that conflates two ideas:

1. *what happened* (money of some amount moved, at some time, for some account), and
2. *the specific ingested record* (this SMS/notification, with this fingerprint,
   dedup rules and provenance).

The spec's Financial Event is the first idea, promoted to a first-class, immutable,
correctable concept that can eventually own more than one `Transaction`-style record
(a purchase and its later refund; a charge and its reversal; a split across people).

`Transaction` is not broken and holds **real user financial history**. The Balance
Engine (`BalanceCalculator` / `AccountBalanceService`) reconstructs balances by replay
over transactions and is the guarantor of **Financial Truth**. Rewriting either is
exactly the kind of big-bang the project has forbidden.

## Decision

Introduce `FinancialEvent` **additively, alongside `Transaction`**. Do **not** replace
`Transaction`, and do **not** change the Balance Engine in this step.

### The model

`FinancialEvent` is an immutable domain record:

| Field | Meaning |
|---|---|
| `id: String` | Stable UUID. Source-agnostic and portable (unlike Transaction's autoincrement `Long`). |
| `transactionId: Long?` | Link to the originating `Transaction` **during coexistence**. Nullable so a future event can be born event-first. |
| `accountId`, `brandId?`, `categoryId?` | Same references Transaction carries. |
| `amount: Money` | `Long` minor units + `CurrencyCode`. Money stays `Long`; currency travels with the event (no implicit app currency). |
| `type: TransactionType` | Reuses the existing money-direction enum for now; event-specific semantics can widen later without breaking this table. |
| `timestamp: Instant` | When the event occurred. |
| `source: IngestionSource` | Provenance (SMS / notification / …). |
| `confidence: Int` (0–100) | The canonical confidence ladder (**D5**). Events carry their own certainty. |
| `status: FinancialEventStatus` | `ACTIVE` / `SUPERSEDED` / `VOID`. |
| `supersedesEventId: String?` | Correction linkage. |
| `fingerprint: String` | Idempotency key (mirrors Transaction's unique fingerprint). |
| `rawText: String?`, `createdAt: Instant` | Evidence + audit. |

### Immutability & correction (Financial Truth)

Events are **never mutated**. A correction is a **new** event that `supersedes` a prior
one; the superseded event's `status` becomes `SUPERSEDED`. Balance/analytics, when they
eventually read events, consider only `ACTIVE` events. This is the same immutable-record
principle Financial Truth already applies to transactions (ADR-0001 §Money invariants of
the spec), expressed at the event layer.

### Coexistence & the incremental path (ADR-0000)

The migration proceeds in small, independently shippable steps. **This ADR authorises
only Step 1**; each later step is its own reviewable change and must independently
compile, pass tests, and preserve user data.

1. **Foundation (this step).** Add the `financial_events` table (additive Room migration
   v10→v11), the domain model, DAO, mapper and repository. **Nothing writes to it yet;
   nothing reads from it; the Balance Engine is untouched.** Pure, reversible scaffolding.
2. **Dual-write (later).** When a `Transaction` is inserted, also record a mirror
   `FinancialEvent` (1:1), idempotently by fingerprint. Balances still come from
   transactions. A backfill records events for pre-existing transactions.
3. **Read migration (later).** Point derived reads (balance, analytics, story) at events
   behind the existing service interfaces, verified equal to the transaction-based result
   before switching. The replay algorithm is preserved; only its input source moves.
4. **Event-first ingestion (later).** New captures create events directly; `Transaction`
   becomes a projection/record maintained for compatibility until it can retire.

### Persistence

`financial_events` is a new table. Enums persist natively as `TEXT` (as `CurrencyCode`
already does). The only foreign key is `transaction_id → transactions(id) ON DELETE
CASCADE`, matching the `splits` precedent. Room's schema is exported (`11.json`) and a
hand-written, additive `MIGRATION_10_11` (CREATE TABLE + indices only) is registered —
no destructive fallback, consistent with the existing migration policy.

## Consequences

**Positive**

- The canonical vocabulary of the spec now exists in code, without disturbing a working
  system or risking a byte of user history.
- Immutability/correction and confidence are first-class at the event layer.
- Every later step is small and independently verifiable.

**Negative / costs**

- Temporary duplication: during coexistence an event mirrors a transaction. Accepted as
  the price of a non-destructive migration; the mirror is idempotent and reconcilable.
- Two IDs (`String` event id, `Long` transaction id) coexist until Transaction retires.

**Guardrails (from ADR-0000)**

- If any step would require rewriting the Balance Engine, mutating history in place, or a
  destructive schema change to keep going, **stop and return to the product owner** —
  that is the ADR-0000 violation trigger.

## Alternatives considered

- **Rename `Transaction` → `FinancialEvent`.** Rejected by D2 and ADR-0000: a large,
  risky, semantics-losing rename.
- **Reuse the `transactions` table with new columns.** Rejected: conflates event and
  record, complicates the eventual one-event→many-records relationship, and cannot carry
  a source-agnostic string id cleanly.
- **Big-bang cutover to events.** Rejected: violates the brownfield strategy and puts
  real user data at risk.

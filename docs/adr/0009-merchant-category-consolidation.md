# ADR-0009 — Merchant / Category System Consolidation

- **Status:** Proposed (awaiting approval before any behaviour-changing step)
- **Date:** 2026-07-22
- **Context docs:** `MERCHANT_ARCHITECTURE.md` (the full System-A-vs-System-B investigation), `ENGINEERING_STATUS.md`

## Context

Two parallel merchant/category systems exist and are both live:

- **System A — `feature/merchant`** (`MerchantResolver`, `MerchantRegistry`): richer seed
  data and brand registry; **safe to extend**. Used by presentation/intelligence read
  paths.
- **System B — `core/domain/service/transaction`** (`MerchantResolver`,
  `MerchantRegistry`-adjacent): **wired into the live `InsertTransactionUseCase` write
  path** and therefore **frozen** — it decides the brand/category stamped onto a
  transaction at capture time. A change here changes what gets persisted.

The two classes share names (`MerchantResolver`, `MerchantRegistry`) across packages,
which is the single most confusing thing for a new maintainer (fresh-maintainer audit,
finding #1). They resolve merchants with **different data and different matching**, so a
naive merge would change which brand/category real transactions receive — i.e. it would
alter persisted financial data, not just refactor code.

## Decision

**Do not merge the two systems in a single step, and do not change System B's matching
behaviour without a demonstrated failing case.** Instead, consolidate in ordered,
individually-verifiable steps, each gated on the corpus regression suite staying green:

1. **Disambiguate names first (zero behaviour change).** Rename the System B pair to
   make the write-path authority unmistakable (e.g. `CaptureMerchantResolver` /
   `CaptureMerchantRegistry`). Pure rename; no logic change; corpus suite proves
   equivalence. This removes the foot-gun immediately.
2. **Unify the seed/brand *data* behind one registry, read-only.** Have System B read the
   same curated brand/keyword data System A already maintains, without changing System
   B's *matching algorithm*. Verified brand-by-brand against the corpus so no
   transaction's resolved brand/category changes unexpectedly.
3. **Then, and only then, converge the matching logic** — one resolver, one algorithm —
   behind ADR-approved, corpus-proven changes, with a migration note for any transaction
   whose historical categorisation would shift.

### Safe narrow first step (identified in MERCHANT_ARCHITECTURE.md)

Normalising System B's **exact-text Brand lookup** (so trivial formatting differences
don't miss a known brand) is the smallest useful improvement. Because it touches the
frozen write path, it is **still gated**: it requires (a) a concrete captured message
that it fixes, added as a corpus fixture asserting the corrected brand/category, and
(b) the full corpus suite green before and after. It is **not** done in this ADR.

## Consequences

- **Positive:** the naming foot-gun is removed early (step 1, safe); data is unified
  before logic (step 2), so behaviour changes are isolated and reviewable (step 3);
  every step is corpus-verified, honouring the "frozen scoring/matching changes need a
  demonstrated bug" rule.
- **Negative / cost:** slower than a one-shot merge; requires corpus fixtures for any
  matching change; historical re-categorisation (step 3) needs a data-migration note.
- **Risk if ignored:** two divergent resolvers keep drifting, and a future contributor
  edits the wrong `MerchantResolver` and silently changes captured data.

## Not doing (and why)

- A blind single-commit merge — it would change persisted brand/category on real
  transactions with no regression proof. Explicitly rejected.

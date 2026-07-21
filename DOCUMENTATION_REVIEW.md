# Documentation Review Report

> **Resolution status (2026-07-21):** The 10 clarification questions in §4 were
> answered by the product owner and adopted as canonical. The decisions are
> recorded in `LEDGER_CONSTITUTION.md` §13 and applied throughout
> `LEDGER_MASTER_SPECIFICATION.md` v1.0 (the merged, deduplicated, contradiction-
> resolved specification). This review is retained as the historical record of
> what was found and why; the contradictions in §2 are now resolved in the merged
> spec. The `part1`–`part11` drafts have been archived under `docs/spec-archive/`.

Principal-architect review of the canonical documentation set, conducted before
any implementation. Scope: all 17 documents (the master-spec skeleton, its 6
body parts, 5 extended "Bibles", and the 5 named governance/engineering/design
docs). No code was written or changed. Implementation is NOT started — this
report exists to surface everything that would cause ambiguity, rework, or
incorrectness if implementation began from these documents as-is.

Cross-referenced throughout against the **existing RC4–RC9 codebase** and its
own audit docs (`ENGINEERING_STATUS.md`, `FINANCIAL_ENGINE.md`,
`INTELLIGENCE_ENGINE.md`, `MERCHANT_ARCHITECTURE.md`, `PIPELINE_ARCHITECTURE.md`),
because the single largest risk in this document set is the gap between what the
spec asserts is built and what actually exists.

---

## Document map (what these files actually are)

| File | Role | Body content? |
|---|---|---|
| `LEDGER_MASTER_SPECIFICATION.md` | Table of contents only — PARTs I–VI, sections 01–36 | **No** — 49-line skeleton |
| `part1`–`part6` | The real body of the master spec (I–VI) | Yes; `part6` ends "END OF LEDGER_MASTER_SPECIFICATION.md" |
| `part7`–`part11` | Extended Bibles: Component (VII), Screen (VIII), Interaction (IX), AI (X), Universal Data Ingestion (XI) | Yes — **not listed in the master-spec TOC** |
| `ENGINEERING_HANDBOOK.md` (02) | Engineering reference | Yes |
| `ECOSYSTEM_SPECIFICATION.md` (03) | Multi-platform ecosystem | Yes |
| `GOVERNANCE.md` (04) | Governance, precedence, lifecycle | Yes |
| `DESIGN_RATIONALE.md` (05) | Why decisions were made | Yes |
| `FUTURE_RESEARCH.md` (06) | Research portfolio (explicitly non-committal) | Yes |

**Structural gap #1:** The master-spec TOC stops at PART VI / section 36, but the
source material contains PARTs VII–XI (the Bibles). When the parts are merged into
the master spec (a later, separate instruction), a decision is required: do the
five Bibles become PART VII–XI of the master spec, or remain standalone canonical
documents? Right now they are canonical content with no home in the master TOC.

---

## 1. Overall Architecture Assessment

The documentation describes a **coherent, unusually disciplined product vision**:
a deterministic-first "Financial Intelligence Platform" whose non-negotiable core
is *Financial Truth* (immutable, reconstructable, auditable, explainable), with an
*Intelligence Layer* that is strictly advisory and always exposes
confidence + evidence + reasoning + override, presented through a *Financial
Story* rather than a transaction feed. The philosophical layer is internally
consistent and genuinely well thought out. Ten "architectural invariants"
(part6 Appendix B) form a clear spine, and every document reinforces the same
values (Financial Truth, Explainability, User Trust, Determinism, Privacy).

The **existing RC4–RC9 codebase already embodies most of these principles** at
the engine level: deterministic capture pipeline, single balance source of truth,
AI-strictly-advisory with a cache/retry/validate/audit pipeline, confidence +
reason on every intelligence decision, frozen-file discipline. In that sense the
spec is not describing a fresh start — much of its "Intelligence Layer" and
"Financial Engine" philosophy is *already implemented and audited*.

The critical assessment, however, is this: **the documents are philosophy-complete
but engineering-incomplete, and in several places they assert a system state that
contradicts the real codebase.** The specs are strong on *what* and *why* and
almost silent on *how* — no data models, no interface contracts, no algorithms
for the flagship capabilities (Safe-to-Spend, Financial Story generation,
Forecast, deduplication hashing, correction/immutability mechanics). The gap
between the aspirational multi-platform ecosystem and the current single-module
Android app is unbridged and unacknowledged in any document.

**Bottom line:** This is an excellent constitution and a poor blueprint. It is
ready to govern implementation decisions; it is not yet ready to *drive* them.
The most important architectural decision — **is this a brownfield evolution of
the RC9 codebase, or a greenfield rebuild?** — is never stated, and different
documents imply different answers (see §2.1).

---

## 2. Contradictions

### 2.1 Brownfield vs greenfield — the unstated, foundational contradiction
- part3 Ch17 ("Backend Capability Matrix") marks **Financial Events, Merchant
  Intelligence, Learning, Explainability, Forecasting as "Complete"** — implying
  build-on-existing.
- But the described **Financial Event model does not exist** in the codebase. The
  real domain core is `Transaction` (an immutable record, yes, but NOT the richer
  "one event → multiple accounting records" model the spec mandates; RC7's
  "Financial Event architecture prep" was deliberately documentation-only).
- The described **module structure** (`app/core/domain/feature/shared` multi-module,
  or the 4-layer Clean Architecture, or the per-domain module list) does not match
  the current **single Gradle module** with package-based layering.
- **This is the highest-priority contradiction.** Everything downstream depends on
  whether we evolve RC9 or rebuild. The documents must not be implemented until
  this is decided.

### 2.2 The Backend Capability Matrix contradicts the real codebase
Beyond §2.1: per `ENGINEERING_STATUS.md` (authored during RC9's audit), several
matrix "Complete" claims are false or partial:
- **Explainability "Engine"** — no distinct engine exists; explainability is a
  property threaded through other components, not a standalone service.
- **Forecasting "Complete"** — `ForecastEngine` exists but is *debug-only-reachable*,
  wired to no user-facing screen.
- **Learning "Complete"** — partial; two separate learned-memory stores, one
  wired into a single flow.
- **Financial Events "Complete"** — does not exist as specified.
- Relying on this matrix to decide "backend is ready, build the UI" (which part3
  Ch17 explicitly instructs — "implemented only after confirming backend readiness")
  would build product surfaces on capabilities that aren't actually there.

### 2.3 Confidence thresholds disagree (financial-critical)
- **part2 Ch8:** 100 confirmed / 95 strong-deterministic / 80 high-confidence /
  60 useful-suggestion / **below 60 do not automate**.
- **part10 Ch118:** 100 confirmed / 95 deterministic / 85 very-high / 70 high /
  55 needs-review / **below 50 never automate**.
- These are two different numeric ladders with two different automation cutoffs
  (60 vs 50). The existing codebase uses a *user-configurable per-capability
  threshold defaulting to 70* (`ConfidenceGate`), which matches neither. A single
  authoritative confidence model must be chosen — this governs when AI acts vs asks.

### 2.4 Dashboard section ordering disagrees across three documents
- **part2 Ch11:** Story, Urgent, **Review(3)**, Upcoming(4), Insights(5), Forecast(6), Accounts, Recent.
- **part5 Ch35:** Story, Urgent, **Review(3)**, Upcoming(4), **Safe-to-Spend(5)**, Forecast(6), Insights(7), Accounts, Recent.
- **part8 Ch79:** Story, Urgent, **Safe-to-Spend(3)**, Upcoming(4), Insights(5), **Review(6)**, Accounts, Recent.
- Review Queue is #3 in two documents and #6 in the third; Safe-to-Spend's rank
  and even presence varies. The Dashboard is the flagship screen — its section
  order needs one canonical answer.

### 2.5 Module / layer architecture has four incompatible framings
1. **ENGINEERING_HANDBOOK Ch2:** top level = `app/core/domain/feature/shared/docs/scripts/buildSrc/gradle` — **no `data/`**.
2. **ENGINEERING_HANDBOOK Ch3:** names Core / Domain / **Data** / Presentation as the module responsibilities — but "Data" has no home in Ch2's tree (internal contradiction *within one document*).
3. **part6 Ch46:** Clean Architecture = Presentation → **Application** → Domain → **Infrastructure** (different vocabulary — "Application"/"Infrastructure" appear nowhere else).
4. **part3 Ch13:** 5 runtime layers = UI / Product Experience / Intelligence / Financial Engine / Universal Ingestion.
5. **part6 Ch47 & ENGINEERING_HANDBOOK feature list:** per-domain feature modules (merchant, forecast, review, story, …).
- These are not reconcilable as written. An implementer cannot know which module/layer taxonomy is canonical.

### 2.6 Intelligence sub-engine roster is inconsistent
- **DESIGN_RATIONALE Decision 11:** 5 sub-engines (Merchant, Forecast, Relationship, Learning, Explainability).
- **part3 Ch16:** ~9 (adds Subscription Detection, Recurring Payment Detection, Anomaly, Recommendation).
- **part10:** treats Anomaly and Recommendation as first-class, plus Personalization.
- The number and names of the intelligence sub-engines differ by document. Appendix A's "canonical vocabulary" lists neither "Recommendation Engine" nor "Anomaly Engine", yet both appear as named engines elsewhere.

### 2.7 "Financial Event" vs "Transaction" (terminology + model)
- Appendix A declares **Financial Event** canonical and lists **Expense List / History Feed / Unknown Transaction** as forbidden. Yet the documents themselves, and the entire existing codebase, are built on **Transaction**. The spec says "Transactions are implementation details" (part2 Ch7) — but they are the *implemented* core. The rename is not cosmetic: Financial Event is a genuinely different, richer model (one event → multiple accounting records) that would require real domain work.

### 2.8 Release-numbering schemes conflict
- part3 Ch17 uses **"RC10"** for Universal Data Ingestion (continuing the RC-series the codebase used through RC9).
- GOVERNANCE Ch8 uses **semantic versioning** (1.0.0, 1.1.0, 2.0.0).
- Two parallel numbering systems with no stated relationship.

### 2.9 Governance Level-1 authority is undefined
- GOVERNANCE Ch3 sets precedence: **Level 1 = "Product Vision"** → Level 2 = Master Spec → ADRs → Handbook → Implementation. But **no `PRODUCT_VISION.md` exists** in the set. `LEDGER_MANIFESTO.md` may be it, but this is unconfirmed. The single highest source of authority in the governance model is unnamed among the delivered files.

---

## 3. Missing Requirements (engineering detail the specs never provide)

The specs are philosophy-heavy and implementation-light. The following are needed
before implementation of the corresponding capability could begin:

1. **Financial Event data model.** Fields are listed (part11 Ch145) but no types,
   no relationships to Account/Institution/Merchant, and — critically — no
   mechanism for "one event → multiple accounting records" or how balances derive
   from events.
2. **Immutability + correction mechanics.** "Corrections generate new history"
   (append-only? event-sourcing? a corrections table?) is asserted everywhere and
   specified nowhere. The reconstruction algorithm (how current balance is derived
   from immutable events + corrections) is undefined.
3. **Safe-to-Spend algorithm.** Named a "flagship capability" (part7 Ch70); inputs
   listed, computation never given.
4. **Financial Story generation.** The defining UX; how narratives are produced
   *deterministically* (the product's own core promise) is unspecified.
5. **Forecast algorithm & confidence computation.** Inputs listed; method, and how
   forecast confidence is derived, absent.
6. **Deduplication hashing.** "Hash" is a dedup signal (part11 Ch141); its
   composition is undefined.
7. **Engine interface contracts.** Every engine is "one responsibility," but no
   input/output interfaces are specified for Merchant/Forecast/Relationship/
   Learning/Explainability/Anomaly/Recommendation.
8. **Learning Rule model + storage.** The learning system is central; its data
   model, persistence, and inspection/edit/reset surface are unspecified.
9. **Synchronization & multi-device.** "Idempotent, conflict-aware, versioned,
   encrypted" stated; no algorithm, and no sync-safe ID scheme (the existing
   Room auto-increment `Long` IDs are *not* sync-safe — already flagged in
   `ENGINEERING_STATUS.md`).
10. **Shared-core extraction plan for the ecosystem.** The multi-platform vision
    (desktop/web/wear consuming "one Financial Core") has no plan for how the
    current single-module Android app becomes a shareable core.
11. **Brownfield migration/refactor plan.** No document maps the target module
    structure onto the existing codebase, or states what is kept/rebuilt/migrated.
12. **Currency policy.** part11 Ch139 normalizes "AED → ISO Currency" (code
    normalization) and part2 Ch7 lists "Currency conversion" as an event type,
    but `FINANCIAL_ENGINE.md` (existing) explicitly performs **no FX conversion**.
    Whether multi-currency net worth requires real conversion — and where rates
    come from in a local-first app — is unresolved.
13. **Testing/corpus requirements for the new engines.** The handbook mandates
    "no financial calculation ships untested," but no corpus/fixture requirements
    are tied to Financial Story, Forecast, or Safe-to-Spend.

---

## 4. Questions Requiring Clarification (need your decision before implementation)

1. **Brownfield or greenfield?** Do we evolve the RC4–RC9 codebase, or rebuild
   against this spec? This single answer changes everything downstream. *(My
   recommendation: brownfield — the existing engines already satisfy most
   invariants; see §5.)*
2. **Is "Financial Event" a rename of `Transaction`, or a genuinely new richer
   model** (one event → multiple records)? If the latter, it is a major domain
   rework and should be an early ADR.
3. **Which module/layer taxonomy is canonical** (§2.5)? Feature-modules?
   Clean-Architecture 4-layer? The 5 runtime layers? And is the app going
   multi-module, or staying single-module with enforced package boundaries?
4. **Which confidence model is authoritative** (§2.3) — part2's, part10's, or the
   codebase's configurable-default-70? This gates all AI-acts-vs-asks behavior.
5. **What is the canonical Dashboard section order** (§2.4)?
6. **How many intelligence sub-engines, and which** (§2.6)? Is Anomaly/
   Recommendation in scope now or deferred to research (FUTURE_RESEARCH lists
   overlapping items)?
7. **What is the Level-1 "Product Vision" authority** (§2.9)? Is it
   `LEDGER_MANIFESTO.md`, or a document not yet in the set?
8. **Do the five Bibles (VII–XI) merge into the master spec, or stay standalone?**
   (Relevant to your stated next step of merging the parts.)
9. **Scope of the current phase.** The ecosystem doc describes 6 client platforms,
   plugins, family mode, business edition, cloud, public API. Is the current
   implementation phase **Android-only** (with the others as horizon), consistent
   with ECOSYSTEM Ch3 "Android is the reference implementation"?
10. **Currency:** is real FX conversion in scope, or is multi-currency handled by
    per-currency isolation (as the existing `CurrencyGuard` does) with no
    conversion?

---

## 5. Recommended Improvements

1. **Adopt brownfield explicitly**, and add a "Current State vs Target State"
   section to the master spec that honestly reconciles the Backend Capability
   Matrix (§2.2) with `ENGINEERING_STATUS.md`. Mark each engine's *real* status
   (built / debug-only / not-built), not an aspirational "Complete."
2. **Designate one document as the single source for each cross-cutting concern**,
   and make the others reference it rather than restate it:
   - Confidence model → one canonical table (kill the §2.3 duplication).
   - Dashboard spec → one canonical order (kill §2.4).
   - Module/layer taxonomy → one canonical diagram (kill §2.5).
   - Intelligence sub-engine roster → one canonical list (kill §2.6).
   - Universal Data Ingestion pipeline → part11 is authoritative; part3 Ch15 and
     DESIGN_RATIONALE Decision 5 should reference it.
3. **Promote the missing engineering detail (§3) into per-capability
   specifications** before those capabilities are scheduled. Governance Ch5's own
   Feature Lifecycle already requires "Specification → Architecture Review" before
   implementation — enforce it for Safe-to-Spend, Financial Story, Forecast, and
   the Financial Event model first.
4. **Resolve the Financial Event vs Transaction question with an ADR** and, if it's
   a real model change, a migration plan (Room schema is at version 10 with real
   FKs and history — this is not a rename).
5. **Fix the internal `ENGINEERING_HANDBOOK` Ch2/Ch3 contradiction** (Data module
   with no home) — a one-line fix that currently makes the canonical engineering
   reference self-contradictory.
6. **Reconcile the two release-numbering schemes** (§2.8): pick RC-series or
   semver, or explicitly map one onto the other.
7. **Lock the canonical vocabulary early** (Appendix A) and run it against the
   codebase — decide whether "Transaction" is retired in favor of "Financial
   Event" at the domain layer, or kept as the persistence-level term with
   "Financial Event" as the domain concept above it.

---

## 6. Suggested ADRs

The handbook/governance already name ADR-001..004. I recommend this initial ADR
backlog (the first three are gating — nothing should be built before them):

- **ADR-000 — Brownfield evolution of RC4–RC9 (not greenfield rebuild).** *(gating)*
- **ADR-001 — Financial Events are immutable** (named in the docs). Define the
  concrete event model, the one-event→many-records mechanism, and the correction/
  reconstruction algorithm. *(gating)*
- **ADR-002 — Universal Data Ingestion is the sole external-data gateway** (named).
  Ratify part11 as the canonical pipeline; define the parser/connector contract.
- **ADR-003 — Explainable Intelligence** (named). Ratify the single confidence
  model (resolving §2.3) and the mandatory evidence/reasoning/override contract.
- **ADR-004 — Financial Story replaces transaction-first navigation** (named).
  Requires the Story-generation algorithm (§3.4) to be specified first.
- **ADR-005 — Module & layer taxonomy** (resolving §2.5): single-module-with-
  enforced-boundaries vs multi-module, and the canonical layer names.
- **ADR-006 — Confidence model & automation thresholds** (resolving §2.3).
- **ADR-007 — Currency policy: isolation vs conversion** (resolving §3.12/Q10).
- **ADR-008 — Sync-safe identity scheme** (prerequisite for any multi-device/
  ecosystem work; the current `Long` auto-increment IDs are not sync-safe).
- **ADR-009 — Merchant/Category system consolidation** (the two-system split from
  `MERCHANT_ARCHITECTURE.md` intersects the spec's clean single-category model).

---

## 7. Implementation Risks

Ranked by severity.

1. **CRITICAL — Building on a Backend Capability Matrix that overstates readiness
   (§2.2).** If the team takes "Complete" at face value and starts building UI on
   top, it will build on Forecasting/Explainability/Financial-Event capabilities
   that are debug-only or nonexistent. This is the most likely source of large,
   late rework. Mitigation: reconcile the matrix with `ENGINEERING_STATUS.md`
   before scheduling any product-surface work.
2. **CRITICAL — Undecided brownfield vs greenfield (§2.1).** Ambiguity here means
   two engineers could reasonably build in opposite directions. Mitigation:
   ADR-000, first.
3. **HIGH — Financial Event model rework (§2.7, §3.1–3.2).** If Financial Event is
   a real model change, it touches the frozen `BalanceCalculator`/
   `AccountBalanceService` core, the Room schema (v10, real FKs + history), and
   every downstream consumer. High blast radius; must be an early, deliberate ADR
   with a migration plan, not an incidental refactor.
4. **HIGH — Flagship capabilities have no algorithm (§3.3–3.5).** Safe-to-Spend,
   Financial Story, and Forecast are the product's differentiators and are
   specified only by inputs and intent. They cannot be implemented deterministically
   (as the spec demands) from the current text. Risk of divergent, non-deterministic
   implementations.
5. **MEDIUM — Architecture taxonomy ambiguity (§2.5).** Four framings will produce
   inconsistent package/module structure across features unless resolved up front.
6. **MEDIUM — Confidence-threshold ambiguity (§2.3).** Two ladders + a third in
   code means AI automation behavior (act vs ask) is undefined; directly affects
   user trust, the product's stated most-valuable asset.
7. **MEDIUM — Ecosystem scope creep.** The ecosystem/future-research docs describe
   an enormous surface (6 platforms, plugins, family, business, cloud, API). Without
   an explicit "Android-only now" boundary, there is real risk of premature
   abstraction for platforms that don't exist yet — which the specs' own principles
   (Deletion before abstraction; architecture simpler over time) would forbid.
8. **LOW — Documentation duplication drift.** The same concept restated in 3–4
   places (confidence, dashboard, ingestion, engineering principles) will drift out
   of sync over time (§2.3/2.4/2.5 are early symptoms). Mitigation: single-source
   each concern (§5.2).
9. **LOW — Missing Level-1 authority (§2.9).** Governance can't function as written
   until "Product Vision" is identified.

---

## Closing

The documentation is a genuinely strong *constitution* and a genuinely incomplete
*blueprint*. Its principles are sound, internally near-consistent at the
philosophy level, and already substantially realized in the RC9 codebase. The work
before implementation is not to add more philosophy — it is to (a) decide
brownfield vs greenfield, (b) reconcile the Backend Capability Matrix with reality,
(c) single-source the four duplicated cross-cutting concerns, and (d) specify the
handful of flagship algorithms and the Financial Event model. The ~9 ADRs above,
with the first three gating, are the path from constitution to buildable plan.

**No implementation performed. Awaiting approval and the clarification answers in §4.**

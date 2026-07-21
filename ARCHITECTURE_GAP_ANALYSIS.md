# Architecture Gap Analysis — RC9 Codebase vs. Master Specification v1.0

**Status:** Implementation-phase analysis (not a frozen document).
**Baseline:** the codebase as it stands after RC9 (branch `feature/ldl-foundation`,
commit `4908a09`), verified directly against source, not from memory.
**Measured against:** `LEDGER_MASTER_SPECIFICATION.md` v1.0 (frozen) and the ten
canonical decisions in `LEDGER_CONSTITUTION.md` §13.
**Guiding principle:** brownfield evolution — preserve working code and Financial
Truth; refactor incrementally; replace only with clear architectural justification.

This document does not implement anything. It classifies the current state,
identifies what to preserve/refactor/replace, and proposes a prioritized plan with
effort and dependencies. Approval is requested before any implementation begins.

---

## 1. Executive Summary

The RC9 codebase is a **healthy foundation, not a blocker.** Its engine layer
already satisfies most of the specification's hardest invariants: Financial Truth
(balance replay from immutable records, currency isolation, deterministic
reconciliation), an advisory-only AI layer with confidence + evidence, and a
well-audited deterministic capture pipeline. The design system (LDL) exists and is
mature. This is exactly the situation the brownfield decision anticipated — there
is real, working, spec-aligned code to build on.

The gaps cluster in three areas, in rough order of architectural weight:

1. **The domain model is `Transaction`-shaped, not `Financial Event`-shaped**
   (Decision D2). This is the single largest architectural migration and gates
   several downstream items. It is *additive and incremental* (the spec and D2 both
   say `Transaction` may coexist), not a rewrite.
2. **The entire Product-Experience layer is largely unbuilt to spec.** The defining
   experience — the Financial Story — does not exist. Navigation is transaction-first
   (Home/Accounts/Transactions/Insights), not the spec's intent-first
   Dashboard/Story/Review/Search/Settings. Merchant, Institution, and Forecast have
   engines but no user-facing screens.
3. **The Intelligence Layer is ~6 of 9 subsystems**, and the built ones are mostly
   wired only to the debug-only Intelligence Inspector, not to product surfaces.
   Three canonical engines — Explainability (as a unified engine), Recommendation,
   Anomaly Detection — do not exist yet.

Universal Data Ingestion exists in spirit (one normalization pipeline) but only for
SMS + notifications; CSV, Manual, PDF, OCR, Email, and Open Banking are unbuilt.

**Net assessment:** roughly the backend/engine half of the product is 60–80%
present and spec-aligned; the product-experience half is 10–20% present. Nothing
requires discarding working code. The critical path runs through one gating ADR
(Financial Event model) and then a large but well-understood UI build (the
"Renaissance") that mostly *wires up engines that already exist*.

---

## 2. Classification method

Each module/capability is rated:

- **Compliant** — exists and matches the spec's intent (naming/vocabulary aside).
- **Partial** — exists but incomplete, mis-scoped, or reachable only from debug.
- **Non-compliant / Missing** — required by spec, does not exist (or exists only as
  an empty placeholder / dead stub).
- **Obsolete** — present but superseded/dead; a cleanup candidate.

"Effort" uses T-shirt sizes: **S** ≈ ≤2 days, **M** ≈ 3–7 days, **L** ≈ 1.5–3
weeks, **XL** ≈ 1–2 months. These are rough, single-engineer estimates for a
codebase this size and assume the gating ADRs are resolved.

---

## 3. Compliance Matrix — the Nine Canonical Subsystems

| # | Subsystem | Status | Where it lives today | Gap |
|---|---|---|---|---|
| 1 | **Financial Engine** | **Partial (Compliant on Transaction model)** | `core/domain/service/transaction` (`BalanceCalculator`, `AccountBalanceService`, `CurrencyGuard`), `core/domain/service/account` (identity/institution), `feature/capture/reconciliation` | Works and is spec-aligned on Financial Truth — but on `Transaction`, not `Financial Event`. No `Institution` domain entity (only a resolver service). Budget model absent. |
| 2 | **Merchant Intelligence Engine** | **Partial** | `feature/merchant` (System A: registry, resolver, learned category) + `core/domain/service/transaction/MerchantResolver` (System B) | Two unrelated systems (`MERCHANT_ARCHITECTURE.md`). No Merchant *entity page*. Merchant memory/aliases exist but crude (System B exact-text). |
| 3 | **Relationship Engine** | **Compliant** | `feature/relationship` (9 resolvers, confidence + reasoning + diagnostics) | Frozen, well-built, read-only. Subscription/recurring detection lives here per D6. Not surfaced in product UI. |
| 4 | **Forecast Engine** | **Partial** | `core/domain/service/intelligence/ForecastEngine` + `RecurringScheduleAnalyzer` | Deterministic, working — but reachable only from the debug Intelligence Inspector; no Forecast screen, no Safe-to-Spend surface. |
| 5 | **Learning Engine** | **Partial** | `feature/merchant/LearnedMerchantCategoryStore` + `core/domain/service/intelligence/LearnedDecisionStore` | Two stores, one real wired flow (institution promotion). No unified "learning history" surface; Review Queue teaches only categories. |
| 6 | **Explainability Engine** | **Non-compliant (as a unified engine)** | Explainability is *threaded through* other components (confidence + reason on merchant/category/relationship; Balance Inspector; Intelligence Inspector) | No single Explainability Engine producing evidence chains/decision history/correction paths as the spec (Ch 120, Decision D6) describes. The pieces exist; the unifying engine does not. |
| 7 | **Recommendation Engine** | **Missing** | — | Does not exist. No subscription-optimization / duplicate-service / savings recommendations. |
| 8 | **Anomaly Detection Engine** | **Missing** | — | Does not exist. No unusual-merchant / missing-salary / rapid-spend detection. |
| 9 | **Universal Data Ingestion** | **Partial** | `feature/capture` (`SmsReceiver`, `LedgerNotificationListener`, `ParserRegistry`, `ExtractionRegistry`, `ReconciliationEngine`, source adapters) | One real normalization pipeline exists and is source-agnostic *by design*, but only SMS + Notification producers exist. `IngestionSource.{CSV,MANUAL,OCR,BANK_API}` are unused enum placeholders. No import report, no import UX. |

---

## 4. Compliance Matrix — Product Experience (5 primary destinations + secondary)

| Screen / IA | Spec | Status | Today |
|---|---|---|---|
| **Navigation / IA** | 5 intent-first destinations: Dashboard, Story, Review, Search, Settings (Ch 34) | **Non-compliant** | Transaction-first: Home, Accounts, Transactions, Insights, Profile, Settings. Different mental model. |
| **Dashboard** | Command center, canonical section order Hero→Urgent→Safe-to-Spend→Story→Upcoming→Insights→Review→Accounts→Recent (D4, Ch 35/79) | **Partial** | `DashboardScreen` exists (collapsing hero, net worth, recent activity) but not the canonical order, no Safe-to-Spend, no Story summary, no Upcoming timeline, no Urgent Actions. |
| **Financial Story** | The defining experience; Past/Present/Future timeline of Financial Events (Ch 36/80) | **Missing** | No Story screen. `TransactionStory.kt` is a tiny explanation+category model, not the narrative timeline. |
| **Review Queue** | All suggestion types with confidence/evidence/Accept-Modify-Reject-Explain (Ch 37/81) | **Partial** | `ReviewInboxScreen` reachable, but only surfaces uncategorized transactions and teaches categories — not duplicates/subscriptions/relationships/forecast corrections, no evidence panel. |
| **Universal Search** | Spotlight-like across all entities (Ch 41/85) | **Partial / Non-compliant** | `SearchFilterScreen` is a transaction filter with mock date-range, not universal search. |
| **Settings** | Transparency hub incl. Learning, AI, Import, Privacy (Ch 86) | **Partial** | `SettingsScreen` + `AiSettingsScreen` exist; missing Learning/Import/Privacy/Backup sections. |
| **Merchant page** | Merchant as living entity (Ch 38/82) | **Missing** | No screen (engine data exists). |
| **Institution page** | Institution as financial home (Ch 39/83) | **Missing** | No screen, no `Institution` entity. |
| **Forecast screen** | Tomorrow→1yr, Safe-to-Spend, confidence (Ch 40/84) | **Missing** | No screen (engine exists, debug-only). |
| **Onboarding** | Trust-first 7 steps incl. Import + Normalization (Ch 87) | **Partial** | Profile setup + SMS onboarding + notification-access exist; not the spec's full trust-building flow. |
| **Widgets / Notifications** | Ch 42/73/89 | **Partial** | `TransactionNotifier` (capture notifications) exists; no home-screen widgets, no forecast/review notifications. |
| **Goals / Subscriptions / Budgets** | Secondary experiences (Ch 34) | **Missing** | No `Goal`/`Subscription` types; `feature/budgets/` is an empty directory; `BudgetRepository` is a backend stub with no model/UI. |

---

## 5. Compliance Matrix — Architectural Invariants (Appendix B)

| Invariant | Status | Note |
|---|---|---|
| 1. Financial Events immutable; corrections append | **Partial** | `Transaction` is immutable, but the Financial *Event* model + correction-as-new-history mechanism is not implemented. |
| 2. Intelligence never rewrites Financial Truth | **Compliant** | AI is advisory-only; no engine writes balances. |
| 3. Every AI decision exposes evidence | **Compliant** | Confidence + reason on merchant/category/relationship; validated via `AISuggestionValidator`. |
| 4. Automated actions reversible; user overrides win | **Compliant** | Learned stores editable; user corrections authoritative. |
| 5. UDI is the only external-data gateway | **Compliant (for existing sources)** | All external data goes through the capture pipeline; no back doors. |
| 6. Domain independent of frameworks | **Mostly compliant** | Domain is framework-free, BUT `core/domain` imports from `feature/*` (relationship, merchant, capture) — a real, documented coupling (`ENGINEERING_STATUS.md`). Not an Android leak; a layering imperfection. |
| 7. UI never performs financial calculations | **Compliant** | Verified in RC7–RC9; balances flow from `AccountBalanceService`. |
| 8. Predictions visually distinct from facts | **Partial** | Principle honored where forecasts appear; no forecast UI yet to enforce it broadly. |
| 9. Every balance reconstructable; no cached truth | **Compliant** | `AccountBalanceService` replays every call. |
| 10. Explainable by design | **Compliant** | Strongest dimension of the codebase. |

---

## 6. Obsolete Code (cleanup candidates)

- **Dead `categories` table + `CategoryEntity`/`CategoryDao`** — kept only because
  `transactions.category_id` FK references it, but never seeded or read
  (`MERCHANT_ARCHITECTURE.md`). Obsolete once the Financial Event / category
  migration lands. **S.**
- **System B merchant `MerchantResolver`/`Brand` exact-text grouping** — functional
  but crude; a consolidation candidate, not deletable in isolation (a live screen
  reads `brandId`). **See Refactor.**
- **`feature/budgets/` empty package + `BudgetRepository` stub** — either build
  Budget or remove the stub; currently dead weight. **S.**
- **Stale doc comment** `BrandEntity.brandKey` "matches LedgerBrandRegistry"
  (false). **S.**
- **Ledger Split** (`Split*`, `Participant`, `feature/.../split`) — full backend,
  zero UI/route (unreachable). Not obsolete per se, but **unreachable dead weight**
  until a product decision includes it (Split is not in the v1.0 spec's primary
  scope). Decide: wire, defer, or remove.
- Debug-only Compose lists missing `key = {}` (perf, low impact) — noted in
  `ENGINEERING_STATUS.md`.

(RC9 already removed the larger dead code: `core/model/*`, `DeveloperConsoleScreen`,
`UNKNOWN_RELATIONSHIP`, the dead category wrapper.)

---

## 7. Technical Debt (carried, with spec implications)

1. **Two merchant/category systems** (`feature/merchant` vs `core/domain/service/transaction`).
   The Financial Event migration is the natural moment to converge them (both feed
   the new event's merchant/category). **M–L.**
2. **`core/domain` → `feature/*` coupling** (15+ files). The spec wants a
   framework-independent domain (invariant 6). This is not an Android leak but a
   layering imperfection; relevant if/when modularizing. **L (deferred).**
3. **Confidence thresholds not yet aligned to the canonical D5 ladder**
   (`ConfidenceGate` default 70 is compatible but per-capability thresholds should
   be reconciled to 95/85/70/50). **S.**
4. **Scalability:** balance replay + several services call `observeAllTransactions()`
   (full history every call, no pagination). Fine at hundreds–low-thousands;
   revisit before a multi-year power user or any home-screen widget. **M.**
5. **No CI, no Robolectric** — governance/testing gaps that will bite during a
   large UI build. **CI: S. Robolectric: M.**

---

## 8. Required Migrations

| Migration | Trigger | Nature | Effort | Gating ADR |
|---|---|---|---|---|
| **Financial Event domain model** | D2 / spec Ch 7, 145 | Additive: introduce `FinancialEvent` as the domain concept above `Transaction`; keep `Transaction` as persistence/implementation detail during transition; balances derive from events. Non-destructive; Room schema at v10 with real FKs + history. | **XL** | **ADR-001** (gating) |
| **Immutability + correction model** | Invariant 1 | Define how corrections generate new history (append/event-sourced vs corrections table) and how reconstruction works. | **L** (part of ADR-001) | ADR-001 |
| **Universal ingestion generalization** | Spec Part XI | Generalize the existing capture pipeline to the full connector/parser/normalizer contract; add CSV + Manual first (the two with existing enum slots and clearest value). | **L** per source | ADR-002 |
| **Merchant/category consolidation** | Debt #1 | Converge System A/B behind the event's merchant/category resolution. | **M–L** | ADR-009 |
| **Navigation / IA migration** | Ch 34 | Move from transaction-first to Dashboard/Story/Review/Search/Settings. Additive (add Story/Search, restructure bottom nav); preserve existing screens as secondary. | **M** | — |
| **Sync-safe identity** | Ecosystem (future) | Room `Long` auto-increment IDs are not sync-safe. Only needed if multi-device is ever pursued (out of Phase-1 scope). | **L** (deferred) | ADR-008 |

---

## 9. Preserve / Refactor / Replace

**Preserve (working, spec-aligned — do not rewrite):**
- `BalanceCalculator`, `AccountBalanceService`, `CurrencyGuard` (Financial Truth core).
- `ReconciliationEngine` (frozen scoring).
- `RelationshipEngine` + resolvers (frozen; already produces confidence/reasoning).
- The deterministic capture pipeline (`NotificationFilter` → `ExtractionRegistry` →
  `FinancialIntentClassifier` → `ReconciliationEngine` → `AccountIdentityResolver` →
  `InsertTransactionUseCase`).
- The entire AI infrastructure (`feature/ai/**`) — orchestrator, providers,
  confidence gate, validator, audit, cache. Ready to serve Explainability/
  Recommendation when those are built.
- LDL design system (`core/designsystem`).
- The diagnostic/Developer-Console suite (Balance Inspector, Intelligence Inspector,
  Pipeline/Ledger Diagnostics) — invaluable during the build.
- `ForecastEngine`, `RecurringScheduleAnalyzer`, `CategoryIntelligenceEngine`,
  `LearnedDecisionStore` — built, tested; just need UI wiring.

**Refactor (evolve, don't replace):**
- Domain model → introduce `FinancialEvent` above `Transaction` (additive).
- Merchant systems → converge A/B during the event migration.
- Dashboard → reorder to canonical sections; add Story summary / Safe-to-Spend /
  Upcoming / Urgent.
- Review Inbox → generalize to the full Review Queue (all suggestion types +
  evidence).
- Navigation → intent-first IA (existing screens become secondary destinations).
- `core/domain`↔`feature` coupling → address opportunistically, not as a big-bang.

**Replace (few, and only with justification):**
- `SearchFilterScreen` (transaction filter with mock data) → Universal Search.
  Justification: it's a stub with hardcoded values, not evolvable to spec.
- Nothing else warrants replacement today.

---

## 10. Prioritized Implementation Plan

Sequenced by dependency and value, respecting the spec's phases and the brownfield
principle. **Gate 0 items must precede coding.** Each item lists effort and
dependencies. This is a proposal for approval, not a commitment.

### Gate 0 — Decisions before code (no implementation; ADRs only)
- **ADR-001 — Financial Event model** (schema, one-event→many-records, immutability/
  correction, `Transaction` coexistence & migration). *Gating for most of Phase A.*
- **ADR-002 — Universal Ingestion connector/parser contract** (ratify Part XI; pick
  CSV + Manual as first new sources).
- **ADR-003 — Confidence model** adoption (align code to the D5 ladder).
- **ADR-009 — Merchant/category consolidation** approach.
- Optional now: **ADR-006** (Safe-to-Spend algorithm), **ADR-00X** (Financial Story
  generation algorithm) — required before those features, can be drafted in parallel.
*Effort: analysis/writing, ~1 week total. Output: approved ADRs.*

### Phase A — Financial Event foundation (spec Phase 1 completion)
- **A1. Introduce `FinancialEvent` domain model** additively; map existing
  `Transaction` → events; balances derive from events. **XL.** *Dep: ADR-001.*
- **A2. Correction/immutability mechanism** + reconstruction. **L.** *Dep: A1.*
- **A3. Merchant/category consolidation** behind event resolution; retire dead
  `categories` wrapper. **M–L.** *Dep: A1, ADR-009.*
- **A4. Confidence ladder alignment.** **S.** *Dep: ADR-003.*
*Outcome: the domain speaks Financial Events; Financial Truth invariants fully met.*

### Phase B — Wire the existing Intelligence to product surfaces (spec Phase 2)
*Highest value-per-effort: most engines already exist and only need UI.*
- **B1. Forecast screen + Safe-to-Spend surface** (wire `ForecastEngine`). **M.**
  *Dep: A1 (events) or can proxy on transactions first; ADR-006 for Safe-to-Spend.*
- **B2. Merchant page** (wire Merchant Intelligence). **M.** *Dep: A3.*
- **B3. Institution page** + introduce `Institution` entity. **M.** *Dep: A1.*
- **B4. Full Review Queue** (all suggestion types + evidence; generalize the inbox).
  **L.** *Dep: A1, Relationship/Forecast engines (exist).*
*Outcome: the built intelligence becomes visible and usable.*

### Phase C — The Financial Story & IA (spec Phase 4 — the "Renaissance")
- **C1. Navigation/IA migration** to Dashboard/Story/Review/Search/Settings. **M.**
- **C2. Financial Story screen + story generation** (deterministic). **XL.**
  *Dep: A1, engines, ADR for story generation.*
- **C3. Dashboard reorder** to canonical sections (Story summary, Safe-to-Spend,
  Upcoming, Urgent). **M.** *Dep: B1, C2.*
- **C4. Universal Search** (replace SearchFilter). **L.**
- **C5. Onboarding + Settings** completion (Learning/Import/Privacy). **M.**
*Outcome: the product feels like the spec.*

### Phase D — Universal Ingestion expansion (spec Phase 3)
- **D1. Generalize pipeline to the full connector/parser contract.** **M.** *Dep: A1, ADR-002.*
- **D2. CSV import** + import report + import UX. **L.** *Dep: D1.*
- **D3. Manual entry.** **M.** *Dep: D1.*
- **D4. (Later) Email/PDF/OCR/Open Banking** — each **L–XL**, deferred.

### Phase E — Missing engines (spec Phase 2 completion)
- **E1. Explainability Engine** (unify the existing evidence/confidence pieces into
  one engine + evidence-chain surface). **M.** *Dep: engines exist.*
- **E2. Recommendation Engine.** **M–L.** *Dep: B1/B4.*
- **E3. Anomaly Detection Engine.** **M–L.** *Dep: A1.*

### Cross-cutting (do early, small)
- **CI workflow** (`./gradlew test`) — **S**, before the UI build scales PR volume.
- **Budget decision** (build or remove the stub) — **S** to remove, **L** to build.
- **Split decision** (wire, defer, or remove) — product call.

### Suggested near-term sequence (my recommendation)
Gate 0 (ADRs) → A1/A2 (event foundation) → **B1 Forecast + B2 Merchant** (fast wins
that make the intelligence visible) → C1 IA + C2 Story (the Renaissance core) →
everything else. This front-loads the gating architecture, then delivers visible
product value by wiring engines that already work, then tackles the big Story build.

---

## 11. Risks

1. **Financial Event migration touching frozen Financial-Truth code.** A1/A2 will
   approach `BalanceCalculator`/`AccountBalanceService`. Mitigation: additive
   layering (events above transactions), migrate reads before writes, keep the
   existing replay as the reconstruction engine, extensive tests (this area already
   has strong coverage).
2. **Story generation is under-specified.** The defining feature has no algorithm in
   the spec (`DOCUMENTATION_REVIEW.md` §3). Needs an ADR before C2 or it will be
   built inconsistently/non-deterministically.
3. **Scope of the Renaissance.** Phase C is genuinely large (XL Story + L Search +
   multiple screens). Realistic multi-month effort; should be milestoned.
4. **No CI during a high-PR-volume UI phase** risks silent regressions in the frozen
   engine core. Add CI first.
5. **Two-system merchant refactor during event migration** — doing both at once (A1
   + A3) concentrates risk; sequence A1 stable before A3.

---

## 12. Bottom line

Preserve the engine layer and AI infrastructure — they are the spec's hardest parts
and they largely exist. The work ahead is (a) one gating architectural migration
(Financial Event), (b) wiring already-built intelligence to real screens, and (c) a
large but well-scoped product-experience build centered on the Financial Story and
intent-first IA. No working code needs to be discarded. The recommended first move
is Gate 0 (ADRs), not code — starting with **ADR-001 (Financial Event model)**,
which unblocks the most.

**No implementation performed. Awaiting approval of this analysis and the Gate-0
ADR direction before proceeding.**

# Ledger Constitution

The distilled, long-lived principles of the Ledger project. This is the layer a
new session reads *instead of* re-reading the ~10,700 lines of source
specification. It contains only durable principles and non-negotiable
constraints — never point-in-time implementation detail. When more detail is
needed, the source documents remain canonical (see "Source hierarchy" below).

Written 2026-07-21 after a full review of all 17 canonical documents, and updated
the same day once the product owner answered the review's open questions. The
companion `DOCUMENTATION_REVIEW.md` records the contradictions and gaps found in
that review; the resolved decisions are now in §13 ("Resolved canonical
decisions") and are reflected in `LEDGER_MASTER_SPECIFICATION.md` v1.0.

---

## 1. Product Identity

Ledger is a **deterministic Financial Intelligence Platform** — not an expense
tracker, not budgeting software, not accounting software. Its purpose is
**understanding, not recording**: "People should understand their money without
needing to become accountants."

It is built on four permanent pillars: **Financial Truth, Explainable
Intelligence, User Trust, Premium Experience.** Every feature, algorithm,
interface, and interaction must reinforce all four.

Ledger is defined as much by what it refuses to be: no black-box AI, no dark
patterns, no engagement optimization, no gamification, no hidden automation, no
platform lock-in. Time-in-app is not a success metric; understanding is. The
product should feel like a precision instrument, and the user should feel calmer
after using it than before.

Android is the first and reference client — **not the whole product.** The
long-term vision is one Financial Core consumed by many interfaces (tablet,
desktop, foldable, wear, widgets, web). Consistency means identical *principles*,
never identical interfaces.

---

## 2. Financial Truth (the foundational invariant)

Everything else is built on Financial Truth. If it is compromised, the platform
loses its reason to exist. Financial Truth means:

- Every financial event exists **exactly once**.
- Every balance can be **reconstructed** from persisted events.
- Every calculation is **deterministic**.
- Every transformation is **traceable** to its source.
- Every correction is **auditable**.

**Ledger never invents money, never hides money, never silently modifies
financial history.** When uncertainty exists, it is represented *explicitly*,
never implicitly.

**Immutability.** Financial history behaves like Git: events are never
overwritten. Corrections generate *new* records; original evidence is preserved;
history stays reconstructable. This enables auditing, debugging, future sync, and
conflict resolution.

**The Financial Event model** is the intended domain core: Ledger operates on
immutable Financial Events (salary, purchase, refund, transfer, subscription
renewal, interest, fee, cashback, currency conversion, manual adjustment, …), not
on bank-specific transaction formats. One event may generate multiple accounting
records. *(Decision D2: Financial Event is the canonical domain model, not a
rename of `Transaction`. The codebase currently implements `Transaction`;
migration toward Financial Events is incremental and non-destructive — §13.)*

**Currency.** Every value carries its own currency; there is no implicit "app
currency." Money is always integer minor units, never floating point. The
existing engine performs **no FX conversion** — non-primary currencies are
isolated, never silently summed or converted. *(Decision D10: FX conversion is
future work; when added it is always explicit, never silent — §13.)*

---

## 3. Explainable Intelligence (AI commandments)

The Intelligence Layer turns Financial Truth into financial *meaning*. It is
**strictly advisory** and must never become a black box.

**Every intelligent output must expose all five, without exception:** what
happened, why Ledger reached this conclusion, which evidence supports it, how
confident it is, and how the user can change it. If any of the five cannot be
answered, the feature is incomplete.

The AI commandments (canonical ordering):
1. Truth before intelligence.
2. Explain before automate.
3. Evidence before confidence.
4. Confidence before recommendations.
5. Recommendations before decisions.
6. Users always override AI.
7. Learning remains reversible.
8. Unknown is acceptable; false certainty is not.
9. Every recommendation teaches.
10. AI exists to reduce uncertainty, never to create it.

**Determinism first, probability only when necessary.** Prefer deterministic
algorithms (merchant aliases, reconciliation); use probabilistic reasoning only
where deterministic rules are insufficient (forecast confidence). Deterministic
memory (a decision the user confirmed once) is always consulted *before* any AI
call.

**AI never writes Financial Truth.** No intelligence engine mutates balances,
accounts, or financial history. It produces opinions; the deterministic engine
(or a human) decides what happens next. Predictions must always be visually and
structurally distinguishable from confirmed facts.

**Confidence is mandatory and visible** on every intelligent output, and should
increase as Ledger *learns*, not merely as time passes. Below the automation
threshold, Ledger asks rather than acts. Canonical confidence ladder (Decision D5):
**95–100 Deterministic · 85–94 Very High · 70–84 High · 50–69 Needs Review ·
below 50 Never automate.**

**AI personality:** professional, calm, honest, transparent — never playful,
overconfident, dramatic, manipulative, or anthropomorphic. It says "Ledger
found…" and "Confidence: 93%", never "I think…" or "Trust me." It can comfortably
say "I don't know."

---

## 4. Trust & Learning

Trust is Ledger's most valuable asset, created through **transparency and
consistency**, not branding. Consistency comes from deterministic behavior;
deterministic behavior comes from explicit rules.

- **Every automatic action is visible and inspectable** (what changed, why, when).
- **Learning is collaborative and reversible:** Ledger proposes, the user
  confirms, the system improves. Every learned rule can be accepted, rejected,
  modified, inspected, or reset. Learning belongs to the user, not the app.
- **The Review Queue reduces future work, never creates it.** Every confirmation
  should teach the system and visibly shrink future review effort. If the Review
  Queue grows over time, the Learning Engine has failed.
- **Users own their data.** Import and export are first-class, permanent
  capabilities. Portability increases trust.

---

## 5. Universal Data Ingestion

**One gateway for all external financial data.** Every source (Open Banking, CSV,
SMS, notifications, email, PDF, OCR, manual entry, future connectors) enters
through the same pipeline and exits as Financial Events. After normalization,
source identity is metadata; the financial model is identical regardless of
origin.

Canonical pipeline (part11 is authoritative):
`Connector → Parser → Normalizer → Validator → Deduplicator → Institution
Resolver → Merchant Resolver → Category Suggestion → Financial Event Builder →
Financial Engine`.

Non-negotiable ingestion rules:
- External sources are **never trusted**; always normalized, validated, versioned.
- **No AI inside parsers or normalization** — parsing and normalization are
  deterministic.
- Validation **never silently fixes or discards** data; problems become
  reviewable.
- Deduplication is **deterministic** and exposes evidence; duplicates are never
  silently dropped.
- Every event is **traceable to its origin** (source, connector, parser version,
  import time). Every import is reversible and produces a permanent report.
- **No connector writes directly into Financial Truth**, and none bypasses
  normalization or validation.
- Adding a source should require only a connector + parser + normalizer mapping —
  nothing downstream changes.

---

## 6. Clean Architecture rules

Dependencies point **inward**. Business logic never depends on frameworks;
frameworks are replaceable, business logic is not.

**The Domain layer is sacred.** It must compile without Android. Forbidden in
Domain: Android `Context`, Compose, Room, Retrofit, DataStore, Navigation, any
framework class. Allowed: entities, interfaces, value objects, business rules,
use cases.

Layer responsibilities (direction of dependency, outer → inner):
- **Presentation** — Compose, navigation, ViewModels, UI models, animations.
  **Never performs financial calculations.**
- **Application / Use Cases** — orchestration of domain operations.
- **Domain** — entities, interfaces, use cases, business rules. Framework-free.
- **Infrastructure / Data** — repositories, data sources, database, network,
  parsers. Never exposes implementation details upward.

Runtime layering of the platform (part3): `UI → Product Experience →
Intelligence Layer → Financial Engine → Universal Ingestion → External Sources`.
The UI never owns financial logic; the Intelligence Layer never mutates Financial
Truth; the Financial Engine never depends on presentation; ingestion contains no
business rules. **Financial Truth precedes Intelligence; Intelligence precedes
Presentation.**

The canonical Clean Architecture layering is `Presentation → Application → Domain
→ Infrastructure` (master spec Chapter 46), with the platform runtime layers
`UI → Product Experience → Intelligence → Financial Engine → Universal Ingestion`
(Chapter 13). *(Per Decision D1/brownfield: the codebase is a single Gradle module
with package-based layering today; convergence toward the module structure in
master-spec Chapter 47 is incremental — modules are introduced only where they add
real boundary value, never as a speculative rewrite.)*

---

## 7. Module & repository conventions

- **Every directory communicates ownership.** No `misc`, `helpers`, `utils2`,
  `temp`, `newcode`, `old`. No module becomes a miscellaneous bucket.
- **Package names describe capability, not implementation**: `merchant`,
  `forecast`, `institution`, `review`, `story`, `learning`. Package names are
  architecture.
- **Naming:** classes are nouns; use cases are verbs; interfaces name
  capabilities; repositories name responsibilities. Avoid `Manager`, `Processor`,
  `Controller`, `Handler`, `Utility` — generic names signal unclear ownership.
- Each feature owns its UI, navigation, ViewModel, use cases, tests, and DI. **No
  feature depends on another feature's UI.**
- **Dependency injection** injects interfaces, never concretes; dependencies point
  inward, never upward.
- **Git:** `main` stable, `develop` integration, feature branches one-purpose.
  Commit messages describe *intent*, not implementation.
- **Documentation is code:** every public API documented, every module has a
  README, every architecture decision an ADR, every feature a specification.

---

## 8. Engineering commandments & coding philosophy

Engineering exists to preserve product quality, not merely to ship features.
Every decision should improve at least one of: simplicity, maintainability,
determinism, performance, testability, explainability — never trade long-term
architecture for short-term convenience. Ledger is expected to exist for many
years; write code accordingly.

Ordering principles (when a decision violates several, it is almost certainly
wrong): **Correctness before speed. Architecture before shortcuts. Readability
before cleverness. Deletion before abstraction. Determinism before automation.
Consistency before novelty. Composition before inheritance. Explicit before
hidden.** Every abstraction must justify its existence.

The ten engineering commandments:
1. Architecture before implementation.
2. Domain before framework.
3. Financial Truth before AI.
4. Determinism before convenience.
5. Readable before clever.
6. Delete complexity.
7. Every feature deserves tests.
8. Documentation evolves with code.
9. Every architecture decision is recorded.
10. Future engineers should thank you.

**Errors are domain concepts, not exceptions** — represent failures explicitly
(cause, recovery, user message, technical details); never swallow exceptions.
**State is a single source of truth per screen** — immutable, observable,
predictable, recoverable; no hidden caches or shared mutable global state.
**Logging never records** passwords, tokens, personal financial data, or secrets.

**Testing:** the Financial Engine, Forecast, Merchant, Learning, Review Queue,
Import, Currency, and Evidence are the highest-coverage areas. **No financial
calculation ships untested; an incorrect financial calculation is a release
blocker.** Definition of Done includes: functional, tested, accessible,
explainable, within performance budget, uses the design system, documented,
privacy-reviewed, security-reviewed.

**Performance budgets:** cold start < 2s, warm start < 500ms, navigation < 150ms,
search < 100ms, animations 60fps, heavy work off the main thread. Measure first,
optimize second — no premature optimization.

---

## 9. Design philosophy

Design is the visual expression of Financial Truth, not decoration. If an element
does not improve comprehension, reduce cognitive effort, or communicate trust, it
should not exist. **Premium comes from precision and consistency, not
ornamentation.** Every screen should feel calm, precise, intentional, confident,
trustworthy, fast — never urgent, stressful, fearful, addictive, or noisy.

Durable design rules:
- **Every screen answers one primary question**; if it answers several unrelated
  ones, redesign it. Screen hierarchy: primary question → primary answer →
  supporting context → suggested action → supporting evidence.
- **Facts and predictions must always look different.** Users must instantly
  distinguish confirmed Financial Truth from forecasts. AI output is always
  visually distinguishable from Financial Truth.
- **Story over spreadsheet.** The Financial Story replaces the transaction feed as
  the primary experience; transactions are supporting evidence, not the headline.
- **Components are financial concepts, not generic widgets** (Financial Story
  Card, Merchant Card, Forecast Card, Review Card, Evidence Timeline, Confidence
  Indicator). Every reusable component is a documented contract defining purpose,
  states, accessibility, motion, and failure/loading/empty states.
- **Motion communicates cause and effect**, never entertainment; it preserves
  object permanence. Ordinary interactions never exceed 500ms.
- **Accessibility is a core requirement, not an enhancement**: screen readers,
  dynamic text, high contrast, color-blind support, reduced motion, keyboard
  navigation; nothing usable by color alone; minimum 48dp touch targets.
- **Empty states educate, never apologize** (what happened, why, what next).
  **Errors explain, never accuse** (what, why, can we recover, can you recover,
  next action). **Loading communicates progress** (skeletons, not infinite
  spinners).
- **Navigation is stable** so users build spatial memory; new features integrate
  into existing destinations rather than multiplying them.

---

## 10. Governance rules

- **The specification is the product's constitution; implementation is one
  realization of it.** If implementation contradicts the specification,
  implementation is considered incorrect until the spec is formally updated.
- **Precedence for resolving conflicts** (Decision D7): `LEDGER_MASTER_SPECIFICATION.md`
  → ADRs → Engineering Handbook → Implementation. The master specification is the
  Level-1 authority; the product vision lives inside it and there is no separate
  vision document.
- **Every major architectural decision requires an ADR** (title, status, context,
  problem, decision, alternatives, consequences, migration plan, related docs).
  ADRs are permanent historical records.
- **Features follow the lifecycle** (idea → research → specification → architecture
  review → prototype → validation → implementation → testing → beta → stable →
  maintenance → deprecation → removal) and never bypass specification.
- **Quality gates carry equal weight:** architecture, design, accessibility,
  performance, security, privacy, testing, documentation, product validation.
- **Technical debt is always visible** (reason, owner, impact, priority, target
  resolution) — hidden debt becomes architectural debt.
- **Backward compatibility is respected**; users never lose financial history; a
  breaking change requires a migration plan, deprecation notice, and version bump.
- **Metrics measure outcomes, not engagement** (forecast accuracy, merchant
  accuracy, Review Queue reduction, import success, correction rate, crash-free
  sessions) — never metrics that encourage addictive behavior.
- **The product grows by refinement, not accumulation.** Before adding a
  capability, ask whether an existing feature can solve it and whether it will
  still make sense in five years. Architecture should get *simpler* over time.

---

## 11. Non-negotiable constraints (architectural invariants)

These must never be violated. Changing any one requires a deliberate ADR and a
specification update.

1. Financial Events are immutable; corrections generate new history, never
   overwrite.
2. Intelligence never rewrites Financial Truth.
3. Every AI decision exposes evidence, reasoning, and confidence.
4. Every automated action is reversible; user corrections always override learned
   behavior.
5. Universal Data Ingestion is the only gateway for external financial data; no
   connector bypasses normalization or validation.
6. Business logic remains independent of frameworks; the Domain layer compiles
   without Android.
7. UI never performs financial calculations.
8. Predictions are visually distinct from confirmed facts.
9. Every balance is reconstructable; balances are never a cached independent
   source of truth.
10. The product remains explainable by design; if it cannot explain a conclusion,
    it does not present that conclusion as fact.

---

## 12. Canonical vocabulary

Use: **Financial Event, Financial Story, Financial Truth, Review Queue, Merchant
Intelligence, Institution, Forecast, Insight, Evidence, Confidence, Learning
Rule, Universal Data Ingestion, Safe to Spend, Explainability.**

Avoid: Expense List, Magic AI, Smart Guess, Auto Fix, History Feed, Prediction
Engine, Unknown Transaction. Consistent language reinforces a consistent mental
model.

---

## 13. Resolved canonical decisions

The questions raised in the documentation review were answered by the product
owner (2026-07-21) and are now canonical. They are reflected throughout
`LEDGER_MASTER_SPECIFICATION.md` v1.0.

- **D1 — Brownfield, not greenfield.** The existing RC9 codebase is the
  foundation. Do not rebuild; do not discard working implementations. Architecture
  evolves incrementally; components are replaced only with clear architectural
  justification. Financial Truth and working business logic take precedence over
  rewriting code. *(The single most important decision.)*
- **D2 — Financial Event is the canonical domain model, NOT a rename of
  Transaction.** Transaction is an implementation detail. Existing `Transaction`
  entities may temporarily coexist during migration, but the architecture
  converges toward Financial Events. Migration is incremental and non-destructive.
- **D3 — The Backend Capability Matrix is the architectural *target*, not current
  status.** The specification is the source of truth for architectural intent; the
  codebase (`ENGINEERING_STATUS.md`) is the source of truth for implementation
  status. Never assume a capability exists — verify against the code.
- **D4 — Canonical Dashboard order:** Hero → Urgent Actions → Safe to Spend →
  Financial Story → Upcoming Timeline → Insights → Review Queue → Accounts →
  Recent Activity.
- **D5 — Canonical confidence model:** 95–100 Deterministic · 85–94 Very High ·
  70–84 High · 50–69 Needs Review · below 50 Never automate. *(The existing
  `ConfidenceGate` default of 70 sits at the High/Needs-Review boundary and is
  compatible; align per-capability thresholds to this ladder over time.)*
- **D6 — Nine canonical subsystems:** Financial Engine · Merchant Intelligence
  Engine · Relationship Engine · Forecast Engine · Learning Engine · Explainability
  Engine · Recommendation Engine · Anomaly Detection Engine · Universal Data
  Ingestion. Subscription and recurring-payment detection live inside the
  Relationship Engine; everything else belongs inside one of the nine.
- **D7 — Level-1 governance authority is `LEDGER_MASTER_SPECIFICATION.md`.** No
  separate Product Vision document; the vision lives in the master spec.
- **D8 — The Bibles (Parts VII–XI) are merged into the master spec** (done, v1.0).
- **D9 — Android only for Phase 1.** Ecosystem/multi-platform material is retained
  as future architectural guidance; only Android is implemented now.
- **D10 — Currency:** multiple currencies supported; original currency always
  preserved; conversions always explicit, never silent; FX conversion is future
  work (matches the existing `CurrencyGuard` isolation approach).

---

## Source hierarchy (when this constitution is not enough)

1. `LEDGER_MASTER_SPECIFICATION.md` v1.0 — the single canonical specification
   (Parts I–XI, Chapters 1–151). This is the Level-1 authority (Decision D7) and
   supersedes the `part*` drafts.
2. `ENGINEERING_HANDBOOK.md`, `GOVERNANCE.md`, `ECOSYSTEM_SPECIFICATION.md`,
   `DESIGN_RATIONALE.md`, `FUTURE_RESEARCH.md` — named canonical docs, governed by
   the master spec.
3. `DOCUMENTATION_REVIEW.md` — the review that produced this constitution and the
   §13 decisions.
4. `ENGINEERING_STATUS.md` and the RC-era engine docs (`FINANCIAL_ENGINE.md`,
   `INTELLIGENCE_ENGINE.md`, `MERCHANT_ARCHITECTURE.md`,
   `PIPELINE_ARCHITECTURE.md`) — ground truth for what the code *actually* does
   today (the specification is the source of truth for architectural *intent*; the
   codebase is the source of truth for implementation *status* — Decision D3).
5. `docs/spec-archive/part1.md`–`part11.md` — historical drafting artifacts,
   superseded by the merged master spec. Retained, not deleted.

This constitution distills 1–3; when precise wording, screen layouts, or detail
is required, read the source. This file changes only when a principle changes —
not when implementation changes.

# Ledger Master Specification

**Version:** 1.0
**Status:** Canonical Product Specification
**Audience:** Product · Engineering · Design · AI Contributors · QA · Future Maintainers

This is the single canonical specification for Ledger. It was produced (2026-07-21)
by merging the eleven drafting documents `part1`–`part11` into one document,
removing duplication and resolving the inconsistencies identified in
`DOCUMENTATION_REVIEW.md`. The `part*` files are retained as historical drafting
artifacts under `docs/spec-archive/`; this document supersedes them.

Chapters are numbered continuously (1–151) across Parts I–XI, preserving the
chapter numbers used in the source drafts (Chapters 7–151) and assigning the
Part I front matter Chapters 1–6.

### Canonical decisions applied in this version

The following product/architecture decisions are canonical and are reflected
throughout this document. They resolve the contradictions found during review.

1. **Brownfield, not greenfield.** The existing RC9 codebase is the foundation.
   The architecture evolves incrementally; working implementations and Financial
   Truth logic are preserved. Components are replaced only with clear
   architectural justification.
2. **Financial Event is the canonical domain model — not a rename of Transaction.**
   Transaction is an implementation detail. During migration, existing
   `Transaction` entities may temporarily coexist, but the architecture converges
   toward Financial Events. Migration is incremental and non-destructive.
3. **The Backend Capability Matrix (Chapter 17) describes the architectural
   *target* state, not current implementation status.** The specification is the
   source of truth for architectural intent; the codebase (`ENGINEERING_STATUS.md`)
   is the source of truth for what is actually implemented. Never assume a
   capability exists — verify against the code.
4. **Canonical Dashboard order:** Hero → Urgent Actions → Safe to Spend →
   Financial Story → Upcoming Timeline → Insights → Review Queue → Accounts →
   Recent Activity.
5. **Canonical confidence model:** 95–100 Deterministic · 85–94 Very High ·
   70–84 High · 50–69 Needs Review · below 50 Never automate.
6. **Nine canonical subsystems:** Financial Engine · Merchant Intelligence Engine
   · Relationship Engine · Forecast Engine · Learning Engine · Explainability
   Engine · Recommendation Engine · Anomaly Detection Engine · Universal Data
   Ingestion. Everything else belongs inside one of these.
7. **Level-1 governance authority is this document** (`LEDGER_MASTER_SPECIFICATION.md`).
   The product vision lives here; no separate vision document is required.
8. **Android only for Phase 1.** The ecosystem/multi-platform material is retained
   as architectural guidance for the future; only Android is implemented now.
9. **Currency:** multiple currencies are supported; original currency is always
   preserved; conversions are always explicit and never silent; FX conversion is
   future work.

---

# PART I — PRODUCT FOUNDATION

# Chapter 1 — Executive Summary

Ledger is a deterministic financial intelligence platform built around one belief:
people should understand their money without needing to become accountants.

Most finance applications organize information around transactions. Ledger
organizes information around understanding. Transactions are raw events;
understanding is the product. Ledger combines deterministic financial computation
with explainable intelligence to transform fragmented financial records into a
coherent financial story, while ensuring every conclusion remains inspectable,
auditable, and reversible.

The platform rests on four permanent pillars: **Financial Truth, Explainable
Intelligence, User Trust, Premium Experience.** Every feature, algorithm,
interface, and interaction must reinforce all four.

Ledger is not an expense tracker, not budgeting software, not accounting software.
Ledger is a Financial Intelligence Platform.

# Chapter 2 — Vision & Mission

**Vision.** Create the most trustworthy personal financial operating system — a
system users rely on not merely to record money, but to understand it. Ledger
should become the place where financial reality is represented with complete
accuracy, enriched through deterministic intelligence, and presented with
exceptional clarity. The ultimate goal is simple: every financial decision becomes
easier because Ledger already provides the necessary context.

**Mission.** Transform raw financial data into understandable financial knowledge.
Ledger exists to reduce uncertainty. Instead of asking users to manually organize
transactions, categories, merchants, recurring payments, subscriptions, budgets,
and forecasts, Ledger continuously builds a living financial model that becomes
increasingly accurate over time while remaining fully explainable.

# Chapter 3 — Product Identity

Ledger is defined by what it refuses to become. It is not designed for
entertainment, engagement metrics, or maximizing screen time. It is designed to
maximize understanding. The product should feel like a precision instrument rather
than a social application. Every interaction should communicate confidence,
competence, and respect for the user's attention.

# Chapter 4 — Product Manifesto

Money is one of the most important sources of stress in modern life. Ironically,
the software intended to simplify finances often increases complexity: users are
expected to become bookkeepers, categories require maintenance, budgets require
constant adjustment, rules grow fragile, and automation behaves like magic.

Ledger rejects this model. Software should perform the difficult work; users
should make decisions. Artificial intelligence should never replace financial
truth — instead, intelligence should expose relationships, explain reasoning,
identify patterns, and reduce manual effort while preserving transparency. If
Ledger cannot explain why it reached a conclusion, it must not present that
conclusion as fact.

# Chapter 5 — Product Principles & Anti-Principles

## Immutable Product Principles

Every future feature must satisfy all of these.

1. **Financial Truth above everything.** Data accuracy is never sacrificed for
   aesthetics. The interface may simplify; the underlying model never lies.
2. **Intelligence must be explainable.** Every recommendation answers: what
   happened, why, how certain, what evidence, how the user can correct it.
3. **Users own their data.** Ledger organizes information; it never owns it.
   Import and export are first-class capabilities.
4. **Every screen must reduce cognitive load.** Complexity belongs inside the
   software, never inside the user's mind.
5. **Story over spreadsheet.** Humans remember stories, not tables. The interface
   explains financial life as a sequence of meaningful events.
6. **Trust is earned.** Trust comes from consistency; consistency from
   deterministic behavior; deterministic behavior from explicit rules.
7. **Learning must be reversible.** The application may learn; the user remains in
   control. Every learned behavior can be inspected, edited, or removed.
8. **Premium means respect.** Premium is not expensive animation. Premium means
   fast, predictable, polished, accessible, respectful.
9. **AI is an assistant, never an authority.** Ledger suggests; users decide.
10. **Simplicity is an engineering responsibility.** If something appears simple,
    engineering absorbed the complexity. Never ask users to compensate for
    technical limitations.

## Product Anti-Principles

Ledger will never intentionally become any of the following:

- **Black-box AI** — no hidden reasoning.
- **Dark patterns** — no manipulative subscription flows, deceptive
  confirmations, or artificial urgency.
- **Engagement optimization** — time spent in Ledger is not a success metric;
  understanding is.
- **Visual noise** — no meaningless badges, charts, or decorative dashboards;
  everything must communicate information.
- **Financial gamification** — money is not a game; no points, streaks, confetti,
  gambling psychology, or manipulative reward loops.
- **Hidden automation** — every automatic action must be inspectable.
- **Platform lock-in** — users can always export their financial history;
  portability increases trust.

# Chapter 6 — Design Philosophy (Product Level)

The interface should feel like Apple Wallet in clarity, Linear in precision,
Things 3 in calmness, and GitHub in transparency — without copying any of them.
Ledger should develop its own recognizable identity through consistency. (The
detailed design language is Part IV; the component and screen specifications are
Parts VII and VIII.)

---

# PART II — FINANCIAL PHILOSOPHY & INTELLIGENCE MODEL

# Chapter 7 — Financial Truth

Financial Truth is Ledger's foundational invariant. Everything else — AI,
forecasts, categorization, recommendations, dashboards, budgets, subscriptions,
notifications — is built on top of it. If Financial Truth is compromised, the
entire platform loses credibility.

Financial Truth means:

- Every financial event exists exactly once.
- Every balance can be reconstructed.
- Every calculation is deterministic.
- Every transformation is traceable.
- Every correction is auditable.

Ledger never invents money, never hides money, and never silently modifies
financial history. When uncertainty exists, Ledger represents it explicitly, never
implicitly.

## The Financial Event Model

Ledger does not fundamentally operate on transactions. **Transactions are an
implementation detail. Ledger operates on Financial Events**, the canonical domain
model. Examples of Financial Events:

Card purchase · Cash withdrawal · Salary payment · Internal transfer · Refund ·
Subscription renewal · Loan repayment · Interest payment · ATM fee · Cashback ·
Currency conversion · Manual adjustment.

Each event becomes part of the user's financial story. A single event may generate
multiple accounting records. Every feature in Ledger ultimately consumes Financial
Events — never bank-specific transaction formats. This abstraction lets every
ingestion source produce the same deterministic model.

> **Migration note (brownfield).** The current implementation persists
> `Transaction` entities. These may temporarily coexist with Financial Events
> during migration. The long-term architecture converges toward Financial Events
> as the domain model; migration is incremental and non-destructive, and never
> discards working balance logic. Implementation status is tracked in
> `ENGINEERING_STATUS.md`, not assumed from this specification.

## Currency

Every Financial Event carries its own currency; there is no implicit "app
currency." Money is represented in integer minor units — never floating point.
Multiple currencies are supported. Original currency is always preserved. **FX
conversion is future work**; when it exists, conversion will always be explicit and
labeled — Ledger never silently converts.

## Source Independence

Ledger must never depend on a specific data source. Possible sources include Open
Banking APIs, CSV imports, email receipts, SMS, notifications, OCR, manual entry,
PDFs, and future integrations. Every source enters through the Universal Data
Ingestion Framework (Part XI). After normalization, source identity becomes
implementation metadata and the financial model is identical.

# Chapter 8 — Explainable Intelligence

Artificial Intelligence exists to reduce manual effort — never to replace
financial truth. Ledger AI performs five categories of work: classification,
relationship discovery, prediction, anomaly detection, and knowledge accumulation.

**Every AI output must include, without exception:** confidence, evidence,
reasoning, corrections, history.

## Confidence Model (canonical)

Confidence communicates certainty and is never hidden. The canonical scale:

| Confidence | Meaning |
|---|---|
| 95–100 | Deterministic |
| 85–94 | Very High |
| 70–84 | High |
| 50–69 | Needs Review |
| Below 50 | Never automate |

Confidence should increase as Ledger *learns*, not merely as time passes. Below
the automation threshold, Ledger asks rather than acts. This same table is
authoritative for all intelligence subsystems (see Chapter 118).

## Evidence Chain

Every AI decision exposes an evidence chain. Example — Merchant: Starbucks;
Evidence: MCC = Coffee Shop, merchant-name similarity 99%, location, previous
confirmations, time-of-day consistency, spending-pattern similarity; Confidence:
98%. This transforms AI from magic into reasoning.

## Explainability Principle

Every AI feature must answer five questions: what happened; why Ledger reached this
conclusion; which evidence supports it; how confident Ledger is; how the user can
change it. If any question cannot be answered, the feature is incomplete.

# Chapter 9 — Trust Architecture

Trust is Ledger's most valuable asset, created through transparency, not branding.
Users trust software that behaves consistently.

- **Every automatic action is visible.** Category assignment, merchant
  recognition, recurring/subscription detection, forecast updates, relationship
  creation — nothing occurs invisibly. Users can always inspect what changed, why,
  and when.
- **Learning is never permanent.** Every learned behavior remains editable: users
  can accept, reject, modify, reset, or inspect every learned rule. Learning
  belongs to the user, not the application.
- **Financial history is immutable.** History resembles Git: events are never
  overwritten. Ledger records corrections; history remains reconstructable. This
  enables auditing, debugging, future synchronization, conflict resolution, and
  explainability.

# Chapter 10 — Financial Story

Traditional finance software answers "What transactions occurred?" Ledger answers
"What happened in my financial life?" This distinction changes the entire product.

Traditional view: `AED -45 · Starbucks · 12:43 PM`. Ledger view: "You bought
coffee. This is your fourth Starbucks purchase this week. Coffee spending has
increased 18% compared to last month. You remain within budget. No action
required." The transaction still exists — but it becomes supporting evidence, not
the primary experience.

**Story timeline.** The Home experience presents Past, Present, and Future in one
continuous timeline: salary received; Netflix renewed; electricity bill expected
tomorrow; dining spending increasing; emergency-fund milestone reached; mortgage
payment next week; budget healthy; Review Queue has pending items.

**Story principles.** Events explain, not merely report. Insights connect, not
merely summarize. Forecasts prepare, not merely predict. Reviews teach, not merely
request confirmation.

# Chapter 11 — Financial Command Center

The Dashboard is not a landing page — it is a command center. Every component
answers a question: Can I safely spend money today? Am I on track? What changed?
What requires attention? What will happen next? What is unusual? What have I
ignored? If a widget cannot answer a meaningful question, it does not belong on the
Dashboard.

**Canonical Dashboard order** (authoritative; see Chapters 35 and 79 for the
product-experience spec and detailed layout):

1. Hero
2. Urgent Actions
3. Safe to Spend
4. Financial Story
5. Upcoming Timeline
6. Insights
7. Review Queue
8. Accounts
9. Recent Activity

Transactions appear last — not because they are unimportant, but because they
support the story.

# Chapter 12 — Product Differentiators

Ledger intentionally differentiates through six capabilities:

1. **Deterministic Financial Engine** — the financial model is mathematically
   correct, not probabilistic.
2. **Explainable Intelligence** — every AI decision is inspectable.
3. **Financial Story** — money becomes understandable, not merely recorded.
4. **Merchant Intelligence** — merchants become entities with memory, not strings.
5. **Universal Data Ingestion** — every financial source becomes one unified
   model.
6. **Learning Without Losing Control** — Ledger improves over time; users remain
   the authority.

---

# PART III — PLATFORM ARCHITECTURE

# Chapter 13 — System Architecture

Ledger follows a layered architecture; each layer has a single responsibility and
no layer contains business logic that belongs elsewhere.

```
User Interface
      ▼
Product Experience
      ▼
Intelligence Layer
      ▼
Financial Engine
      ▼
Universal Ingestion
      ▼
External Sources
```

The UI never owns financial logic. The Intelligence Layer never mutates Financial
Truth. The Financial Engine never depends on presentation. The ingestion layer
contains no business rules. **Financial Truth precedes Intelligence; Intelligence
precedes Presentation.**

**Layer 1 — External Sources.** Acquire data (Open Banking, CSV, PDF, email, SMS,
notifications, OCR, manual entry, future connectors). Never trusted; always
normalized; always versioned.

**Layer 2 — Universal Data Ingestion.** Transform every source into one canonical
format: parsing, normalization, validation, deduplication, currency normalization,
institution identification, merchant normalization, metadata extraction. Output:
Financial Events, nothing else. No UI logic, no AI, no forecasting. (Detailed in
Part XI.)

**Layer 3 — Financial Engine.** Ledger's source of truth: Financial Events,
accounts, balances, categories, budgets, transfers, institutions, currency,
reconciliation. Every value displayed anywhere must ultimately originate here.

**Layer 4 — Intelligence Layer.** Consumes Financial Truth, produces knowledge,
never mutates financial history directly. Sub-engines listed in Chapter 16.

**Layer 5 — Product Experience.** Presentation only: Dashboard, Financial Story,
Review Queue, Merchant pages, Forecast, Insights, Search, Widgets, Notifications,
Settings. No business logic.

# Chapter 14 — Domain Architecture

Ledger models real-world financial concepts, not UI concepts. Core domains:
Institution, Account, Financial Event, Merchant, Category, Budget, Goal,
Relationship, Forecast, Insight, Review, Notification, Learning Rule, Evidence,
Confidence.

Every feature belongs to exactly one primary domain; cross-domain communication
occurs through the Financial Engine.

Domain flow (one-directional):

```
Institution → Accounts → Financial Events → Merchant Intelligence →
Relationships → Insights → Forecasts → Financial Story
```

Financial Story never changes Financial Events. Forecasts never modify balances.
Insights never overwrite history.

# Chapter 15 — Universal Data Ingestion (Overview)

Universal Data Ingestion eliminates source-specific behavior. Every source
(notification, SMS, email, CSV, PDF, OCR, Open Banking, manual entry) passes
through the same pipeline and exits as Financial Events — never UI objects, never
provider-specific transactions. Adding a new source requires only a parser, a
normalizer mapping, and a connector; every downstream system continues functioning
unchanged. **This chapter is an overview; Part XI (Chapters 134–151) is the
authoritative Universal Data Ingestion Specification.**

# Chapter 16 — Intelligence Architecture (Nine Canonical Subsystems)

Ledger Intelligence is modular; each engine has one responsibility. The canonical
subsystems are exactly nine. Everything else belongs inside one of them.

1. **Financial Engine** — the deterministic source of truth (events, accounts,
   balances, reconciliation, currency). Not an "AI" engine, but the foundation the
   others consume.
2. **Merchant Intelligence Engine** — merchant profiles, memory, confidence,
   categories, relationships.
3. **Relationship Engine** — relationships between entities (merchant ↔ category,
   merchant ↔ institution, account ↔ goal, recurring and behavioral links).
   Subscription detection and recurring-payment detection are responsibilities of
   this engine, not separate engines.
4. **Forecast Engine** — cash flow, balance forecasts, subscription forecasts,
   budget projections, Safe-to-Spend.
5. **Learning Engine** — observes user corrections; produces personalized rules,
   confidence improvements, review reduction, learning history.
6. **Explainability Engine** — evidence chains, confidence, decision history,
   correction paths.
7. **Recommendation Engine** — actionable improvement suggestions (subscription
   optimization, budget adjustments, duplicate services, savings opportunities).
8. **Anomaly Detection Engine** — unusual (not necessarily suspicious) activity:
   unexpected merchants/amounts, duplicates, missing salary, rapid spending
   increases.
9. **Universal Data Ingestion** — the sole gateway for external financial data
   (Part XI).

These subsystems operate independently but communicate through shared Financial
Events.

# Chapter 17 — Backend Capability Matrix (Target Architecture)

> **This matrix describes the architectural *target* state, not current
> implementation status.** The specification is the source of truth for
> architectural intent; the codebase (`ENGINEERING_STATUS.md`) is the source of
> truth for what is actually implemented. Never assume a capability exists because
> it appears here — verify against the code. "Target" means the capability is part
> of the intended architecture; its real build status may be complete, partial,
> debug-only, or not-yet-started.

| Capability | Owning Subsystem | Architectural Role |
|---|---|---|
| Financial Events | Financial Engine | Target domain core |
| Accounts | Financial Engine | Target |
| Institutions | Institution Registry (Financial Engine) | Target |
| Merchant Intelligence | Merchant Intelligence Engine | Target |
| Relationship Discovery | Relationship Engine | Target |
| Learning | Learning Engine | Target |
| Explainability | Explainability Engine | Target |
| Forecasting | Forecast Engine | Target |
| Recommendations | Recommendation Engine | Target |
| Anomaly Detection | Anomaly Detection Engine | Target |
| Dashboard | Product Layer | Product surface |
| Financial Story | Product Layer | Product surface |
| Review Queue | Product Layer | Product surface |
| Merchant Detail | Product Layer | Product surface |
| Search | Product Layer | Product surface |
| Widgets | Product Layer | Product surface |
| Notifications | Product Layer | Product surface |
| Universal Data Ingestion | Universal Data Ingestion | Target (Part XI) |

# Chapter 18 — Traceability Matrix

Every product feature must map to backend subsystems. If a feature cannot be
traced to subsystems, its specification is incomplete.

| Product Feature | Required Subsystems |
|---|---|
| Dashboard | Financial Engine + Forecast + Insights |
| Financial Story | Relationship + Forecast + Merchant Intelligence |
| Review Queue | Learning Engine |
| Merchant Detail | Merchant Intelligence |
| Institution Detail | Financial Engine (Institution Registry) |
| Budget Overview | Financial Engine |
| Spending Insights | Relationship + Merchant Intelligence |
| Forecast Screen | Forecast Engine |
| Safe to Spend | Forecast + Financial Engine |
| Explain Button | Explainability Engine |
| Search | Financial Engine + Merchant Index |
| Notifications | Forecast + Rules + Intelligence |
| Recommendations | Recommendation Engine |
| Alerts | Anomaly Detection Engine |

# Chapter 19 — Architectural Rules

These rules are permanent (see also Appendix B, Architectural Invariants):

1. No UI component performs financial calculations.
2. No intelligence engine directly mutates Financial Truth.
3. Every AI decision is reproducible.
4. Every Financial Event is immutable; corrections generate new history, never
   overwrite.
5. Every backend subsystem has one responsibility.
6. Every product feature must be explainable.
7. Universal Data Ingestion is the only entry point for external financial data.
8. Presentation depends on subsystems; subsystems never depend on presentation.
9. Financial Truth always precedes Intelligence.
10. Intelligence always precedes Presentation.

---

# PART IV — DESIGN LANGUAGE

# Chapter 20 — Design Philosophy

Design is the visual expression of Financial Truth, not decoration. Every visual
element must improve understanding, reduce cognitive effort, or communicate trust;
if it does not, it should not exist. Ledger values clarity above novelty, and
premium is achieved through precision, not ornamentation. Every screen should feel
calm, precise, intentional, confident, trustworthy, and fast. Users should never
wonder "What does this mean?" or "What happens if I press this?" — the interface
answers those questions before they arise. Ledger should evoke control, confidence,
clarity, progress, and calm — never urgency, stress, fear, addiction, or noise.

# Chapter 21 — Visual Identity

Ledger's identity is defined by restraint. Visual hierarchy replaces visual
complexity: white space is a feature, motion is communication, typography is
structure, color communicates meaning, icons provide recognition, illustrations
provide personality. The interface should resemble a professional financial
instrument (Apple Wallet + Linear + Things 3 + GitHub, without copying any),
becoming immediately recognizable through consistency. Large numbers deserve
emphasis; actions deserve clarity; warnings deserve restraint; success is
understated; errors educate; animations disappear into the experience.

# Chapter 22 — Layout System

Spacing is based on an 8dp system. Allowed values: 4, 8, 12, 16, 20, 24, 32, 40,
48, 64. Never introduce arbitrary spacing.

Grid: phones 4-column adaptive; tablets 8-column; desktop 12-column responsive;
foldables dual adaptive regions; Wear OS single-column glance. Reading width
remains comfortable; financial data should not span the entire screen
unnecessarily; whitespace increases comprehension.

# Chapter 23 — Typography

Typography is Ledger's primary organizational tool and should communicate
importance before color does.

**Number philosophy.** Financial values always use tabular numbers — to prevent
visual jumping, improve comparison, and increase perceived stability.

**Hierarchy.** Display (net worth, account balance, Safe to Spend, large metrics);
Headline (Financial Story, insight titles, section headers); Title (cards, dialogs,
merchant pages); Body (descriptions, transactions, metadata); Caption (dates, tags,
supporting information).

**Rules.** Never truncate money. Never abbreviate important financial values.
Align decimal places where possible. Negative numbers remain immediately
recognizable. Currency symbols remain visually secondary.

# Chapter 24 — Color Philosophy

Color communicates state, never decoration. Roles: Primary (identity, navigation,
interactive elements); Success (confirmed, healthy, positive); Warning (needs
review, pending, attention); Error (incorrect, failure, critical); Information
(neutral knowledge, explanations, insights).

**Financial color rules.** Positive cash flow uses the positive color; negative
cash flow the negative color; transfers are neutral; forecasts are muted until
confirmed. Predictions never share identical styling with confirmed financial
truth — users must instantly distinguish facts from predictions.

# Chapter 25 — Elevation & Surfaces

Depth is used sparingly and only to communicate hierarchy. Surface types: Base,
Cards, Dialogs, Sheets, Floating Elements, Navigation — each with a defined
elevation token; avoid arbitrary shadows. Cards feel lightweight; dialogs command
attention; bottom sheets feel connected.

# Chapter 26 — Component Philosophy

Components solve problems, not layouts. Every component defines purpose, states,
accessibility, motion, failure state, loading state, empty state, and
implementation notes, and must be documented. (The full component catalog is Part
VII.) Every component should expose the universal states: Loading, Empty, Success,
Warning, Failure, Disabled, Expanded, Collapsed.

# Chapter 27 — Motion Philosophy

Motion communicates cause and effect, never entertainment. Every animation answers:
where did this come from, where did it go, what changed. Categories: navigation,
state change, expansion, insertion, removal, confirmation, loading, transition,
shared element. Durations: fast 100–150ms, normal 200–300ms, complex 350–450ms;
never exceed 500ms for ordinary interactions. Motion reduces cognitive load, never
increases it.

# Chapter 28 — Accessibility

Accessibility is a core requirement, not an enhancement. Support screen readers,
dynamic font sizes, high contrast, color blindness, reduced motion, keyboard
navigation, foldables, tablets, desktop, landscape, and Wear OS. Every interaction
must remain usable without relying solely on color; icons require labels; charts
require textual summaries; animations require reduced-motion alternatives. Minimum
touch target: 48dp (see Chapter 98 for the full touch-target scale).

# Chapter 29 — Empty States

Empty states educate; they never apologize. Bad: "No data." Good: "You haven't
imported any accounts yet. Connect your bank or import a CSV to begin building your
financial story." Every empty state answers: what happened, why, what next.
(Screen-specific empty states are Chapter 94.)

# Chapter 30 — Loading Philosophy

Loading communicates progress, never uncertainty. Prefer skeletons, progressive
loading, optimistic updates, background refresh. Avoid infinite spinners, blank
screens, blocking dialogs.

# Chapter 31 — Error Philosophy

Errors are explanations, not accusations. Every error answers: what happened, why,
can Ledger recover, can the user recover, what should happen next. Never expose
technical jargon unless explicitly requested. (Screen-specific error handling is
Chapter 95.)

# Chapter 32 — Design Commandments

1. Whitespace is information.
2. Typography creates hierarchy.
3. Color communicates meaning.
4. Motion communicates relationships.
5. Every screen answers one primary question.
6. Facts and predictions must always look different.
7. Large financial values deserve visual priority.
8. AI must remain visually distinguishable from Financial Truth.
9. Components are reusable before they are beautiful.
10. Premium comes from consistency, not decoration.

---

# PART V — PRODUCT EXPERIENCE & SCREEN SPECIFICATION

# Chapter 33 — Information Architecture

Ledger organizes information by user intent, not database structure. Every screen
answers one primary question. Core user questions map to destinations:

1. Where do I stand today? → Dashboard
2. What happened? → Financial Story
3. What requires my attention? → Review Queue
4. Why did this happen? → Insights, Merchant pages, Explainability
5. What will happen next? → Forecast, Upcoming Events, Safe to Spend
6. Where can I find something? → Universal Search
7. How does Ledger work? → Settings, Privacy, Learning, Data Sources

Navigation exists to answer these questions, not to expose features.

# Chapter 34 — Navigation Philosophy

Navigation remains stable so users develop spatial memory; changing it frequently
destroys confidence. **Five permanent primary destinations:** Dashboard, Story,
Review, Search, Settings. Future features integrate into these rather than creating
new navigation. Secondary experiences (Merchant, Institution, Forecast, Budget,
Goal, Subscription, Account) appear through navigation from primary destinations
and never become primary destinations unless product strategy fundamentally
changes.

# Chapter 35 — Dashboard (Product Experience)

**Primary question:** "Where do I stand right now?" The Dashboard is Ledger's
Financial Command Center — a decision-support interface, not a summary page or
widget collection. Every section should influence a financial decision.

**Canonical section order** (see Chapter 11 for rationale, Chapter 79 for detailed
layout): Hero → Urgent Actions → Safe to Spend → Financial Story → Upcoming
Timeline → Insights → Review Queue → Accounts → Recent Activity.

**Hero.** Communicates the user's financial state within five seconds. Possible
content: net worth, cash available, Safe to Spend, current trend, next major event,
one primary insight. Never overload the Hero; never more than one primary action.

**Urgent Actions.** Displayed only when action matters (review transaction,
reconnect institution, potential duplicate, large unusual purchase, upcoming
overdraft). No urgency means no section.

# Chapter 36 — Financial Story

**Primary question:** "What happened in my financial life?" The Story screen is
Ledger's defining experience and replaces the traditional transaction feed. Instead
of Transaction → Category → Amount, Ledger presents Financial Event → Context →
Relationships → Outcome → Evidence. Every story item answers: what happened, why it
matters, what changed, what happens next.

Example: "Netflix renewed. Subscription cost increased AED 5 — the third increase in
two years. Projected annual cost: AED XXX. Suggested alternatives available.
Confidence 98%. Explain available." Transactions become expandable evidence, not the
headline.

# Chapter 37 — Review Queue

**Primary question:** "What does Ledger need from me?" The Review Queue exists to
reduce future work, never create it — every confirmation should improve the system.
Examples: unknown merchant, low-confidence category, duplicate detection,
recurring-payment suggestion, subscription detection, forecast correction,
relationship confirmation, learning proposal. Every review card displays the
suggestion, confidence, evidence, and Accept / Modify / Reject / Explain. Never ask
users to make blind decisions.

# Chapter 38 — Merchant Experience

Merchants are living entities, not strings. A merchant page contains overview,
financial relationship, visit frequency, monthly spend, category, subscriptions,
related merchants, insights, timeline, evidence, and learning history. It answers
"What is my relationship with this merchant?" — not merely "What transactions
exist?"

# Chapter 39 — Institution Experience

Institutions are financial homes. An institution page contains accounts, balances,
connections, history, reliability, synchronization, imported sources, linked cards,
security, and institution health. Users should immediately understand which
institution owns which financial data.

# Chapter 40 — Forecast Experience

**Primary question:** "What happens next?" Forecasts communicate confidence, never
speculation. Timeline: Tomorrow, This Week, This Month, Three Months, One Year.
Content: upcoming bills, expected salary, recurring payments, subscription
renewals, projected cash flow, Safe to Spend, savings trend. Every prediction
exposes confidence, evidence, reasoning, and adjustment history.

# Chapter 41 — Universal Search

Search feels instantaneous; users should not need to know where information lives.
It indexes merchants, transactions, accounts, institutions, categories, insights,
forecasts, subscriptions, goals, review items, and learning rules. Search is an
intelligence layer, not a filter.

# Chapter 42 — Notifications

Notifications protect attention, never compete for it. Allowed: large transaction,
forecast risk, review required, subscription renewal, salary received, institution
disconnected, potential fraud, goal milestone. Avoid daily reminders, artificial
engagement, streaks, promotional notifications, and meaningless summaries.

# Chapter 43 — User Journeys

- **First launch:** Onboarding → Import → Normalization → Story Creation → Review
  Queue → Dashboard.
- **Daily user:** Dashboard → Story → Review → Exit (minimal effort).
- **Unexpected spending:** Notification → Dashboard → Story → Merchant → Evidence →
  Resolution.
- **Budget planning:** Dashboard → Forecast → Safe to Spend → Decision.

# Chapter 44 — Product Experience Rules

1. Users understand the Dashboard within five seconds.
2. Every AI suggestion exposes evidence.
3. Every screen answers one primary question.
4. Every review improves future automation.
5. Financial Story replaces transaction-first thinking.
6. Search discovers everything.
7. Navigation remains stable.
8. Forecasts distinguish certainty from probability.
9. Users remain in control.
10. Ledger should feel calmer after five minutes than before opening it.

---

# PART VI — ENGINEERING, QUALITY & EVOLUTION

> This Part is the product-specification view of engineering. The standalone
> `ENGINEERING_HANDBOOK.md` remains the detailed engineering reference and is
> governed by this specification (see Chapter 58 and Governance).

# Chapter 45 — Engineering Philosophy

Engineering exists to preserve product quality, not merely to deliver features.
Every line of code should make Ledger more understandable, more deterministic, more
testable, more maintainable, and easier to extend. Features are temporary;
architecture is permanent. Principles: correctness before convenience; readability
before cleverness; determinism before automation; composition before inheritance;
explicit behavior before hidden magic; delete unnecessary code; every abstraction
must justify its existence.

# Chapter 46 — Clean Architecture

Ledger follows dependency inversion — dependencies point inward:

```
Presentation → Application → Domain → Infrastructure
```

Business rules never depend on Android, Compose, databases, networking, dependency
injection, or frameworks. Frameworks are replaceable; business logic is not. The
Domain layer compiles without Android.

# Chapter 47 — Product Modules

The application evolves as cohesive, ownership-clear modules. Suggested modules:
Core, Financial Engine, Intelligence, Merchant, Forecast, Review, Story, Dashboard,
Search, Settings, Import, Notifications, Analytics, Widgets, Shared UI. Each module
owns its models, use cases, repositories, tests, and UI. No module becomes a
"miscellaneous" bucket.

> **Brownfield note.** The current codebase is a single Gradle module with
> package-based layering. Convergence toward the module structure above is
> incremental; modules are introduced only where they add real boundary value, not
> as a speculative rewrite.

# Chapter 48 — State Management

Every screen has a single source of truth. UI observes state; it never constructs
truth. State is immutable, predictable, recoverable, and serializable when
appropriate. Every state transition is reproducible. Avoid mutable global state,
shared mutable objects, and hidden caches.

# Chapter 49 — Performance Standards

Ledger should feel instantaneous. Budgets: cold start < 2s; warm start < 500ms;
navigation < 150ms; search < 100ms; filtering real-time; animations ≥ 60fps. Heavy
work belongs off the main thread. Avoid unnecessary allocation, repeated database
reads, and duplicate models; prefer streaming over loading entire datasets.
Background work (forecasting, learning, synchronization) respects device
constraints. Measure first, optimize second.

# Chapter 50 — Data Integrity

Financial correctness has higher priority than performance. Never silently discard
Financial Events; never overwrite historical records; every mutation is traceable;
every synchronization is idempotent; duplicate detection is deterministic; conflicts
require explicit resolution.

# Chapter 51 — Privacy

Privacy is a product feature, not merely a legal requirement. Ledger operates
locally whenever possible; cloud functionality is additive, never mandatory. Users
understand what is stored, where, why, and how to delete it. Principles: data
minimization, explicit consent, local-first where feasible, user ownership,
portable exports, predictable retention.

# Chapter 52 — Security

Security is continuous, not a release milestone. Requirements: encrypted storage,
secure authentication, biometric unlock, certificate pinning (where applicable),
secure backups, tamper resistance, dependency auditing. No secrets in source
control; sensitive operations are logged.

# Chapter 53 — Observability

Every important subsystem exposes health: import success rate, Review Queue size,
learning accuracy, forecast accuracy, synchronization failures, merchant-confidence
distribution, duplicate-detection rate, crash-free sessions, performance metrics.
Telemetry improves the product and never compromises privacy.

# Chapter 54 — Testing Strategy

Testing pyramid: unit, integration, UI, manual exploratory. Highest coverage:
Financial Engine, Forecast Engine, import pipeline, Learning Engine, merchant
matching, currency handling, synchronization, Review Queue, explainability.
Incorrect financial calculations are release blockers.

# Chapter 55 — Definition of Done

A feature is complete only if it is functional, tested, accessible, explainable,
within performance budget, uses the design system, documented, localized (when
applicable), analytics-reviewed, privacy-reviewed, and security-reviewed. Code
complete is not feature complete.

# Chapter 56 — Release Strategy

Product evolution occurs in phases:

- **Phase 1 — Financial Truth:** reliable financial foundation.
- **Phase 2 — Intelligence:** merchant recognition, learning, forecasting,
  explainability.
- **Phase 3 — Universal Ingestion:** notifications, CSV, PDF, email, SMS, Open
  Banking, OCR.
- **Phase 4 — Premium Product Experience:** Financial Story, Review Queue, widgets,
  insights, motion, accessibility.
- **Phase 5 — Platform Expansion:** desktop, Wear OS, tablets, foldables, public
  APIs, partner integrations.

> **Scope note.** Phase 1 is **Android only**. Part V of the Ecosystem material and
> Phase 5 above are architectural guidance for the future, not current work.

# Chapter 57 — Long-Term Vision

Ledger should evolve into a complete financial operating system. Possible future
capabilities: investment tracking, net-worth analytics, tax-preparation support,
business finance, family finance, shared accounts, financial goals, retirement
planning, document vault, receipt management, AI financial assistant. None may
violate the principles in this specification. (Research directions are catalogued in
`FUTURE_RESEARCH.md`.)

# Chapter 58 — Canonical Decision Framework

Whenever uncertainty exists, ask: Does this increase Financial Truth? Improve
understanding? Can the result be explained? Does it preserve user trust? Is it
deterministic? Does it reduce manual effort? Can users override it? Does it respect
privacy? Will this still make sense five years from now? If the answer to multiple
questions is "No," reconsider the proposal.

# Chapter 59 — The Ledger Promise

Ledger makes one promise: **your financial life will become easier to understand
without asking you to surrender control.** Everything in this specification exists to
fulfill that promise. If an implementation conflicts with this promise, the
implementation must change — the promise does not.

## Appendix A — Canonical Vocabulary

Use: Financial Event, Financial Story, Review Queue, Merchant Intelligence,
Institution, Forecast, Insight, Evidence, Confidence, Learning Rule, Universal Data
Ingestion, Safe to Spend, Financial Truth, Explainability. Also canonical (Chapter
16): Financial Engine, Merchant Intelligence Engine, Relationship Engine, Forecast
Engine, Learning Engine, Explainability Engine, Recommendation Engine, Anomaly
Detection Engine.

Avoid: Expense List, Magic AI, Smart Guess, Auto Fix, History Feed, Prediction
Engine, Unknown Transaction. Consistent language reinforces a consistent mental
model.

## Appendix B — Architectural Invariants

These must never be violated. Changing any invariant requires a deliberate
architectural decision (ADR) and an update to this specification.

1. Financial Events are immutable; corrections generate new history, never
   overwrite.
2. Intelligence never rewrites Financial Truth.
3. Every AI decision exposes evidence.
4. Every automated action is reversible; user corrections always override learned
   behavior.
5. Universal Data Ingestion is the only gateway for external financial data.
6. Business logic remains independent of frameworks; the Domain compiles without
   Android.
7. UI never performs financial calculations.
8. Predictions are visually distinct from confirmed facts.
9. Every balance is reconstructable; balances are never a cached independent source
   of truth.
10. The product remains explainable by design.

---

# PART VII — COMPONENT BIBLE

# Chapter 60 — Component Philosophy

Components are the building blocks of Ledger — not merely reusable widgets. Each
represents a specific financial concept or interaction and must satisfy six
responsibilities: display information, communicate state, support accessibility,
handle failure, support loading, behave predictably. A component should never
require another engineer to guess why it exists, when to use it, how it behaves, or
how it fails. Every component is a contract, and every reusable component defines:
purpose, responsibilities, variants, properties, states, interactions, motion,
accessibility, analytics, implementation notes, and future extension points — before
implementation.

# Chapter 61 — Net Worth Hero

Communicates the user's current financial position immediately (Dashboard only).
Contains net worth, available cash, Safe to Spend, trend, primary insight, upcoming
event. One primary action only; readable within five seconds. States: Loading,
Empty, Healthy, Warning, Critical, Offline. Screen reader announces current value,
trend, and change since previous period; never relies on color alone.

# Chapter 62 — Financial Story Card

Transforms raw financial events into meaningful narratives. Contains headline,
supporting context, confidence, evidence indicator, actions, timestamp, related
entities. Interactions: expand, collapse, explain, navigate, share. Motion: card
expands naturally; supporting evidence fades in beneath the narrative. Never open
modal dialogs for ordinary story expansion.

# Chapter 63 — Review Queue Card

Presents decisions requiring user validation: suggestion, reason, confidence,
evidence, recommended action, and Accept / Modify / Reject / Explain. Accept improves
the Learning Engine; Reject updates learning rules; Modify becomes new canonical
truth. Review completion immediately reduces future workload.

# Chapter 64 — Merchant Card

Represents a merchant as an intelligent entity: name, category, relationship
summary, visit frequency, average spend, trend, confidence, subscriptions, insights.
Actions: open merchant, view story, view evidence, transactions, compare. Never
display merchants merely as text strings.

# Chapter 65 — Institution Card

Represents financial institutions: institution, accounts, connection status,
balance, synchronization health, import source, security. Status indicators:
Healthy, Warning, Disconnected, Importing, Sync Error.

# Chapter 66 — Forecast Card

Communicates future financial expectations: forecast, confidence, evidence,
projected balance, upcoming bills, recurring income, Safe to Spend. Forecasts remain
visually distinct from confirmed financial values.

# Chapter 67 — Evidence Timeline

Explains reasoning: Evidence → Confidence → Decision → User Correction → Learning.
Every intelligent decision should reference an Evidence Timeline.

# Chapter 68 — Confidence Indicator

Communicates certainty using categories (Very High, High, Medium, Low, Unknown) —
users understand categories faster than precise values. Detailed percentages remain
available in expanded views. Category mapping follows the canonical confidence model
(Chapter 8 / 118).

# Chapter 69 — Insight Card

Communicates knowledge, not raw statistics: headline, explanation, supporting
evidence, suggested action, confidence, and Dismiss / Save / Explain. Insights answer
"So what?" — not merely "What happened?"

# Chapter 70 — Safe-to-Spend Widget

Answers "What can I safely spend today?" Inputs: forecast, upcoming bills, recurring
payments, income, current balance. Output: safe spending limit, confidence,
explanation, warnings. One of Ledger's flagship capabilities. (The Safe-to-Spend
computation must be specified as a deterministic algorithm before implementation —
see the open engineering items in `DOCUMENTATION_REVIEW.md` §3.)

# Chapter 71 — Budget Progress

Visualizes progress without shaming users: progress, remaining, forecast, expected
completion, trend. Avoid red panic bars, aggressive warnings, and fear-based
language.

# Chapter 72 — Search Result

Presents every searchable entity consistently (merchant, institution, Financial
Event, account, insight, forecast, goal, category, subscription, learning rule).
Each result includes icon, title, context, relationship, quick actions.

# Chapter 73 — Notification Card

Represents meaningful events; answers what happened, why, what to do, and whether it
can be ignored. Actions: Open, Dismiss, Explain, Snooze (where appropriate).
Notifications are never advertisements.

# Chapter 74 — Transaction Row

Displays evidence, not narrative: merchant, amount, time, category, account,
confidence, status, expandable (related story, explain, evidence). Transaction rows
remain intentionally minimal; context belongs to the Financial Story.

# Chapter 75 — Universal Component States

Every reusable component supports: Loading, Skeleton, Empty, Offline, Success,
Warning, Failure, Disabled, Expanded, Collapsed, Focused, Selected, Error. No
component invents custom state terminology.

# Chapter 76 — Interaction Contract

Every interactive component defines primary action, secondary action, long press,
keyboard navigation, screen-reader behavior, haptic feedback, animation, analytics
events, and future extension points. Behavior remains predictable across the
application.

# Chapter 77 — Component Commandments

1. Components communicate, not decorate.
2. Reuse behavior, not only visuals.
3. Every component degrades gracefully.
4. Accessibility is mandatory.
5. Loading is a first-class state.
6. Failure is a designed experience.
7. Evidence is always reachable.
8. Components explain themselves.
9. One responsibility per component.
10. Components reduce cognitive load, never increase it.

---

# PART VIII — SCREEN BIBLE

# Chapter 78 — Screen Philosophy

Every screen answers one primary question and never competes with itself. A user
should immediately understand why they are here, what the most important information
is, and what to do next. Every screen follows the same hierarchy: primary question →
primary answer → supporting context → suggested action → supporting evidence. If
this hierarchy is violated, redesign the screen.

# Chapter 79 — Dashboard (Detailed Layout)

The Dashboard is Ledger's Financial Command Center, answering "How am I doing right
now?" within five seconds. It is a decision-support interface — not a homepage or
report.

**Canonical layout** (top = decisions, bottom = evidence):

```
App Bar
  ▼
Hero (Financial Story summary + Net Worth / Cash / Safe to Spend / Trend / Next Event)
  ▼
Urgent Actions        (only when required)
  ▼
Safe to Spend
  ▼
Financial Story
  ▼
Upcoming Timeline     (Tomorrow / This Week / This Month; salary, bills, subscriptions, goals)
  ▼
Insights
  ▼
Review Queue
  ▼
Accounts              (balance, trend, institution, health, last sync)
  ▼
Recent Activity       (evidence — transactions appear only after the story is communicated)
  ▼
Footer
```

Scrolling reveals increasing detail. The Hero never carries more than one primary
CTA. Urgent Actions appear only when action matters (unknown merchant, institution
disconnected, upcoming overdraft, potential duplicate, forecast warning, review
required). Accounts never expose implementation details.

# Chapter 80 — Financial Story Screen

Tells the user's financial story rather than listing transactions. Timeline:
Yesterday → Today → Tomorrow → Future, every event in context. Example: "Salary
received → Savings increased → Budget recovered → Emergency fund now at 83% → Next
electricity bill due Tuesday." The Story Card contains headline, narrative,
confidence, evidence, impact, and actions (Explain, Merchant, Forecast, Related
Events). Nearby events group naturally (Morning Coffee + Parking + Fuel → Morning
Commute) rather than appearing as unrelated transactions.

# Chapter 81 — Review Queue Screen

Reduces future work; every review improves Ledger and never creates repetitive
confirmation. Priorities: Critical → High → Medium → Low → Learning Suggestions.
Critical examples: duplicate, unknown institution, currency conflict, large
transaction. Card layout: Suggestion → Evidence → Confidence → Accept / Modify /
Reject / Explain → Learning Result. Accepting visibly teaches Ledger.

# Chapter 82 — Merchant Profile

Understands a merchant, not merely lists purchases. Sections: overview, financial
relationship, visit patterns, monthly spend, subscriptions, timeline, insights,
evidence, learning history. Metrics: lifetime spend, average spend, frequency,
preferred time, trend, category, confidence, most-visited location. Answers: how
often, how much, has spending changed, recurring, related merchants, alternatives.

# Chapter 83 — Institution Profile

Represents financial institutions: overview, accounts, balances, cards, connection,
import sources, sync health, history, security, reliability. Answers "What financial
relationship do I have with this institution?"

# Chapter 84 — Forecast Screen

Helps users prepare, not surprise them. Timeline: Tomorrow, This Week, This Month, 3
Months, 1 Year. Cards: projected balance, upcoming bills, recurring income,
subscriptions, Safe to Spend, savings projection, risk events, confidence, explain.
Forecasts never pretend certainty.

# Chapter 85 — Search Screen

Universal discovery that behaves like Spotlight — users search concepts, not
locations. Searches: merchant, institution, category, transaction, goal, insight,
forecast, learning rule, evidence, subscription. Tolerates mistakes; results appear
instantly.

# Chapter 86 — Settings

Transparency, not preferences alone. Sections: Profile, Connections, Privacy,
Learning, AI, Notifications, Import Sources, Appearance, Security, Backup, Developer,
About. Every intelligent feature is configurable.

# Chapter 87 — Onboarding

Build trust before asking for permissions. Steps: Welcome → Ledger Philosophy →
Privacy → Import Options → Normalization → Review Queue → Dashboard. Progressive
disclosure only; do not overwhelm.

# Chapter 88 — Import Experience

Supported sources: Open Banking, CSV, SMS, notifications, email, PDF, OCR, manual.
Flow: Choose Source → Permission → Import → Normalization → Review → Story Created.
Users always know what Ledger is doing. (Detailed pipeline: Part XI.)

# Chapter 89 — Widget Experience

Widgets answer questions, not duplicate screens: Safe to Spend, upcoming bills, net
worth, story summary, Review Queue, goals, forecast. Widgets remain glanceable.

# Chapter 90 — Tablet Experience

Tablets are not enlarged phones; use adaptive multi-pane layouts (Dashboard left /
Story right; Review split; Merchant two-pane; Search master-detail). *(Guidance for
future platform work; Phase 1 is Android phone-first.)*

# Chapter 91 — Foldables

Foldables expose two simultaneous contexts (Story ⇄ Merchant; Forecast ⇄ Evidence);
never waste additional space. *(Future platform guidance.)*

# Chapter 92 — Desktop

Desktop supports exploration: persistent navigation, resizable panels, multi-window,
drag-and-drop, keyboard shortcuts, advanced search, bulk review, power-user
workflows. Not simply Android stretched. *(Future platform guidance.)*

# Chapter 93 — Wear OS

Wear is glance-first: Safe to Spend, upcoming bills, notifications, balance, goals,
quick review; nothing requiring prolonged interaction. *(Future platform guidance.)*

# Chapter 94 — Empty Screens

Every empty screen educates, never apologizes. Examples: "No Forecast → Connect an
account to begin forecasting." "No Story → Import financial history to create your
first Financial Story." Each explains what, why, and next step.

# Chapter 95 — Error Screens

Every failure answers what happened, why, whether Ledger can recover, whether the
user can recover, and the next action. Technical messages remain hidden unless
Developer Mode is enabled.

# Chapter 96 — Screen Commandments

Every screen answers one question, exposes evidence, supports accessibility,
supports loading, supports offline behavior, supports empty state, supports failure
state, supports search where applicable, and contributes to the Financial Story.
Navigation remains predictable.

---

# PART IX — INTERACTION & BEHAVIOR BIBLE

# Chapter 97 — Interaction Philosophy

Interaction is communication. Every gesture, animation, transition, tap, swipe, long
press, or vibration must communicate meaning and answer one of three questions: what
happened, what changed, what should happen next. If an interaction cannot answer one
of these, it should not exist. Interactions should feel predictable, calm, precise,
responsive, forgiving, and trustworthy; users develop muscle memory and are never
surprised by changing behavior.

# Chapter 98 — Touch Model

Primary interaction: tap. Secondary: long press. Contextual: swipe. Advanced:
drag-and-drop. System: back gesture. Predictability outweighs novelty. Touch
targets: minimum 48dp; preferred 56dp; critical actions 64dp. Financial values never
require pixel-perfect interaction.

# Chapter 99 — Navigation Behavior

Navigation communicates hierarchy: primary navigation is instant; secondary is
animated; contextual uses shared elements; destructive requires confirmation. Deep
navigation never exceeds four levels; users always know how to return.

# Chapter 100 — Gesture Language

Swipe left: dismiss / archive / reject (by context). Swipe right: accept / complete
/ confirm. Long press: reveal advanced actions (never hide primary actions behind
it). Pull down: refresh (only where live information exists). Drag: reorder /
prioritize / organize. Never overload gestures.

# Chapter 101 — Motion Language

Motion preserves continuity — nothing appears unexpectedly, and every animation
preserves object permanence so users understand where an element came from and where
it went. Categories: navigation, state change, expansion, collapse, insertion,
removal, loading, confirmation, error recovery. Shared-element transitions are
recommended for merchant, institution, forecast, story, review, account, and large
cards; avoid unnecessary transitions for simple lists.

# Chapter 102 — Animation Rules

Animations never delay work, block interaction, or become performance bottlenecks.
Durations: micro 100ms; small 150–200ms; normal 250–300ms; large 350–450ms; never
exceed 500ms during normal navigation.

# Chapter 103 — Haptic Language

Haptics reinforce confidence, not excitement: Light (selection), Medium
(confirmation), Heavy (critical warning), Double (major achievement). Never vibrate
for ordinary scrolling or decoration.

# Chapter 104 — Undo Philosophy

Undo builds trust. Every reversible action exposes Undo (delete, archive, merge,
import, review, learning rule, dismiss). Users should never fear experimentation.

# Chapter 105 — Confirmation Philosophy

Confirm only when an action is irreversible, financially meaningful, privacy-
affecting, or security-affecting (delete account, reset learning, disconnect
institution, erase history). Never ask "Are you sure?" for routine navigation.

# Chapter 106 — Progressive Disclosure

Reveal complexity gradually: Transaction → Merchant → Relationship → Evidence →
Learning History → Raw Financial Event. Power users receive depth; beginners receive
clarity.

# Chapter 107 — Context Menus

Long press exposes Explain, Share, Pin, Compare, View Evidence, View Merchant, View
Institution, History, and Developer (Debug Mode). Context menus remain consistent
across entities.

# Chapter 108 — Selection Model

Single selection is the default. Multi-selection supports bulk review, delete,
categorize, and export. Selection remains visually obvious.

# Chapter 109 — Keyboard Support

Desktop, Chromebook, and tablets support arrow keys, tab navigation, Enter, Escape,
a search shortcut, and a quick-review shortcut. Power users should not require touch.
*(Relevant to future platform work.)*

# Chapter 110 — Offline Behavior

Offline feels intentional, not broken. Users understand available, unavailable,
pending synchronization, pending review, offline changes, and the synchronization
queue. No silent failures.

# Chapter 111 — Error Recovery

Every recoverable failure provides Retry, Explain, Contact Support (if applicable),
and Continue Offline. Never trap users; recovery is part of interaction design.

# Chapter 112 — Micro-Interactions

Micro-interactions reinforce confidence. Example (review accepted): card gently
collapses → learning indicator appears → queue count updates → Undo available.
Example (import complete): normalization summary → Financial Story generated →
Dashboard refreshes → first insight appears. Every interaction acknowledges user
effort.

# Chapter 113 — Behavioral Consistency

Users should never ask "What happens if I press this?" Behavior remains identical
throughout: if one card expands vertically, every equivalent card does; if one swipe
accepts, every equivalent swipe accepts. Consistency reduces learning.

# Chapter 114 — Interaction Commandments

1. Motion communicates, never decorates.
2. Undo whenever possible.
3. Long press reveals depth, never primary functionality.
4. Gestures remain consistent.
5. Animations preserve continuity.
6. Offline is a supported state.
7. Recovery is designed, not improvised.
8. Interactions teach confidence.
9. Users remain in control.
10. Every interaction reduces cognitive effort.

---

# PART X — AI & INTELLIGENCE BIBLE

# Chapter 115 — Intelligence Philosophy

Ledger Intelligence exists to increase understanding — not automation, engagement,
or novelty. It continuously transforms raw financial events into useful knowledge
while preserving Financial Truth, and never becomes a black box. Understanding is
always more valuable than surprise.

# Chapter 116 — AI Principles

Every intelligent capability satisfies all of: Financial Truth first; explainability
always; confidence visible; evidence available; corrections respected; learning
reversible; privacy preserved; deterministic when possible, probabilistic only when
necessary. AI augments judgment; it never replaces it.

# Chapter 117 — Intelligence Pipeline

Every intelligent conclusion follows the same lifecycle:

```
Financial Event → Normalization → Feature Extraction → Relationship Analysis →
Evidence Collection → Confidence Evaluation → Recommendation → Review (if required) →
Learning → Knowledge Base Update
```

No shortcut may bypass explainability.

# Chapter 118 — Confidence Model (Canonical)

Confidence represents certainty, not importance, and is mandatory on every
intelligent output (merchant detection, category, subscription, forecast,
relationship, recommendation, anomaly, review suggestion). The canonical scale
(identical to Chapter 8):

| Confidence | Meaning |
|---|---|
| 95–100 | Deterministic |
| 85–94 | Very High |
| 70–84 | High |
| 50–69 | Needs Review |
| Below 50 | Never automate |

Confidence increases as Ledger learns, not because time passes. Below 50, Ledger
never automates and always requests confirmation.

# Chapter 119 — Evidence Model

Every conclusion requires inspectable evidence. Possible evidence: merchant name,
institution, category history, location, time, amount, frequency, device, account,
previous decisions, recurring pattern, relationship graph, user corrections. No
recommendation exists without supporting evidence. Example — Merchant: Netflix;
Evidence: name similarity, previous confirmations, monthly recurrence, subscription
behavior, card consistency; Confidence: 99%.

# Chapter 120 — Explainability

Every intelligent decision answers: what happened, why, which evidence, how certain,
what alternatives were considered, and whether the user can change it. If one answer
is missing, the implementation is incomplete. Explainability exists before
automation, not after it.

# Chapter 121 — Learning Lifecycle

Learning is collaborative: Ledger proposes, users teach, Ledger remembers, users
remain authoritative. Cycle: Suggestion → Review → Correction → Knowledge Update →
Future Improvement → Confidence Increase. Learning always reduces future work.

# Chapter 122 — User Override Model

Users always have final authority. Overrides apply to merchant, category,
institution, forecast, relationship, budget, subscription, and learning rule. Every
override becomes first-class knowledge and never disappears silently.

# Chapter 123 — Personalization

Ledger becomes increasingly personal, not increasingly unpredictable: preferred
merchants, typical schedules, payment timing, salary cycle, spending rhythm, category
preferences, notification timing, dashboard emphasis, search ranking. Personalization
explains itself.

# Chapter 124 — Forecast Intelligence

Forecasting communicates probability, not certainty. Inputs: recurring payments,
salary, bills, historical behavior, seasonality, goals, manual plans. Predictions
always include confidence, evidence, assumptions, and possible variance.

# Chapter 125 — Recommendation Engine

Recommendations answer "What can I improve?" — never "What keeps me engaged?"
Examples: subscription optimization, budget adjustments, duplicate services, savings
opportunities, upcoming cash-flow risks, unexpected trends. Recommendations remain
actionable.

# Chapter 126 — Anomaly Detection

An anomaly is unexpected, not necessarily suspicious: unusual merchant, unexpected
amount, location change, duplicate payment, new subscription, missing salary, rapid
spending increase. Every anomaly includes explanation, confidence, evidence, and a
suggested action.

# Chapter 127 — Merchant Intelligence

Merchants are knowledge objects, not text labels. A merchant profile contains
identity, aliases, categories, locations, relationships, visit history, average
spend, subscriptions, confidence, and learning history. Merchant Intelligence
improves over time.

# Chapter 128 — Relationship Intelligence

Ledger continuously builds relationships: merchant ↔ merchant, merchant ↔ category,
merchant ↔ institution, goal ↔ spending, income ↔ budget, subscriptions ↔ forecast.
Relationships explain context, not merely store links.

# Chapter 129 — Memory Model

Ledger remembers confirmed decisions, corrections, preferences, patterns, review
history, learning rules, merchant aliases, and institution mappings. Memory remains
editable and is never hidden.

# Chapter 130 — Review Intelligence

The Review Queue should become smaller over time (through learning, confidence
improvement, pattern recognition, relationship discovery) — never larger. If the
Review Queue continually grows, the Learning Engine has failed.

# Chapter 131 — AI Personality

Ledger Intelligence feels professional, calm, honest, transparent, and helpful —
never playful, overconfident, dramatic, manipulative, or anthropomorphic. It never
pretends to be human. Instead of "I think…", it says "Ledger found…"; instead of
"I'm sure…", "Confidence: 93%"; instead of "Trust me," "Evidence available."

# Chapter 132 — Failure Philosophy

Intelligence sometimes fails, and failure must remain visible. Unknown is preferable
to incorrect. Ledger comfortably states "I don't know." Low confidence is a valid
result.

# Chapter 133 — AI Commandments

1. Truth before intelligence.
2. Explain before automate.
3. Evidence before confidence.
4. Confidence before recommendations.
5. Recommendations before decisions.
6. Users always override AI.
7. Learning remains reversible.
8. Unknown is acceptable; false certainty is not.
9. Every recommendation teaches.
10. AI exists to reduce uncertainty, never create it.

---

# PART XI — UNIVERSAL DATA INGESTION SPECIFICATION

# Chapter 134 — Philosophy

Financial data should not care where it came from. Banks, formats, providers, and
countries differ; Financial Truth does not. The Universal Data Ingestion Framework
transforms every financial source into one canonical representation. Once normalized,
the rest of Ledger never needs to know whether data originated from Open Banking,
CSV, SMS, notifications, email, PDF, OCR, manual entry, or future connectors — every
source becomes identical.

# Chapter 135 — Design Goals

Universal Data Ingestion satisfies five objectives: source independence,
deterministic normalization, traceability, extensibility, data integrity. Every
parser solves exactly one problem — understanding its source. Every downstream system
speaks Financial Events.

# Chapter 136 — Pipeline Architecture

```
External Source → Connector → Raw Import → Parser → Normalizer → Validator →
Deduplicator → Institution Resolver → Merchant Resolver → Category Suggestions →
Financial Event Builder → Financial Engine → Intelligence Layer → Product Layer
```

Every stage owns exactly one responsibility.

# Chapter 137 — Connectors

Connectors acquire data and nothing more: authentication, permissions, download,
metadata, scheduling, retry. They never contain business logic. Examples: CSV, Open
Banking, notification listener, SMS listener, email, PDF import, OCR import, future
API connector.

# Chapter 138 — Parser Contract

Each parser converts source-specific formats into a common intermediate
representation (CSV → rows → intermediate objects; PDF → OCR → structured fields; SMS
→ regex → payment object; bank API → JSON → intermediate object). Parser output is
deterministic. **No AI belongs inside parsers.**

# Chapter 139 — Normalization

Normalization creates canonical financial meaning: merchant names ("STARBUCKS #123"
→ Starbucks), currency codes (normalize to ISO currency codes — code normalization
only, never FX conversion; see Chapter 7), dates (various formats → UTC), amounts
(provider-specific → Ledger standard integer minor units). Normalization removes
provider-specific complexity and is deterministic.

# Chapter 140 — Validation

Validation guarantees integrity: missing amount, missing date, currency validity,
account mapping, institution mapping, duplicate identifiers, malformed records,
future-date errors. **Validation never silently fixes data** — issues become
reviewable.

# Chapter 141 — Deduplication

Duplicate detection is deterministic. Signals: transaction ID, timestamp, amount,
merchant, account, reference number, institution, hash. Duplicates expose evidence
and are never silently discarded. States: Unique, Likely Duplicate, Confirmed
Duplicate, Conflict. *(The hash composition must be specified before implementation —
see `DOCUMENTATION_REVIEW.md` §3.)*

# Chapter 142 — Institution Resolution

Identifies financial institutions consistently (ENBD / Emirates NBD / NBD →
Institution Registry). Deterministic; unknown institutions remain reviewable.

# Chapter 143 — Merchant Resolution

Transforms text into entities ("AMZN Mktp" → Amazon) using aliases, history,
institution, location, patterns, learning, confidence, and evidence. One of Ledger's
core differentiators.

# Chapter 144 — Category Suggestion

Categories are suggested, never imposed. Inputs: merchant, history, user preference,
institution, relationships, learning. Outputs: category, confidence, evidence, review
requirement. Users remain authoritative.

# Chapter 145 — Financial Event Builder

All normalized records become Financial Events. Fields include identifier,
timestamp, amount, currency, merchant, institution, account, type, metadata,
evidence, source, confidence, relationships. **Every Financial Event is immutable.**
*(The concrete Financial Event schema, including the one-event→many-records mechanism
and the immutability/correction model, must be specified via ADR before
implementation — see `DOCUMENTATION_REVIEW.md` §3 and the brownfield migration note
in Chapter 7.)*

# Chapter 146 — Source Traceability

Ledger always answers "Where did this come from?" Every event stores original
source, import time, connector, parser version, normalization version, review
history, and learning changes. Users can always inspect origin.

# Chapter 147 — Plugin Architecture

Future connectors require minimal engineering effort. A plugin provides a connector,
parser, metadata, capabilities, and validation rules; the rest of Ledger remains
unchanged. Adding a connector never requires rewriting the Financial Engine. *(Plugin
distribution is future work; see `FUTURE_RESEARCH.md`.)*

# Chapter 148 — Import Experience

Import feels trustworthy. Stages: Acquire → Analyze → Normalize → Validate → Review →
Create Story → Done. Users always understand what Ledger is doing.

# Chapter 149 — Import Report

Every completed import produces a permanent report: imported, skipped, duplicates,
errors, warnings, unknown merchants, unknown institutions, review required,
confidence summary.

# Chapter 150 — Data Quality

Ledger measures ingestion quality: normalization rate, duplicate rate, merchant-
resolution accuracy, institution accuracy, review percentage, learning success,
import duration, connector reliability.

# Chapter 151 — Universal Data Principles

External sources are never trusted. Normalization is deterministic. Financial Events
are immutable. Every import is reversible, explainable, and traceable. Every source
follows the same pipeline. No connector bypasses normalization or validation. No
connector writes directly into Financial Truth.

---

# END OF LEDGER MASTER SPECIFICATION v1.0

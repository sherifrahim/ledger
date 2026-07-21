# Ledger — Engineering Status

RC9. This is the canonical "state of the codebase" document — read this first
when picking up work on Ledger. See `ARCHITECTURE.md` for diagrams,
`FINANCIAL_ENGINE.md`/`INTELLIGENCE_ENGINE.md`/`PIPELINE_ARCHITECTURE.md`/
`MERCHANT_ARCHITECTURE.md` for subsystem-level depth.

## Current architecture, in one paragraph

Ledger is a single-module (`:app`) offline-first Android app. A deterministic
capture pipeline (`NotificationFilter` → `ExtractionRegistry` →
`DeterministicFinancialIntentClassifier` → `ReconciliationEngine` →
`DeterministicAccountIdentityResolver` → `InsertTransactionUseCase`) is the
ONLY write path into the `transactions`/`accounts` tables — see
`PIPELINE_ARCHITECTURE.md`. Everything else (merchant canonicalization,
category classification, relationship inference, recurring/forecast
projection, balance replay) runs at READ time, on demand, per screen, never
cached — see `INTELLIGENCE_ENGINE.md`. AI (`AIOrchestrator` + friends) is
fully built but has exactly one live call site (an on-demand "Ask AI" button
in the debug-only Intelligence Inspector) and is never in the write path.

## Subsystem ownership

| Subsystem | Owner file(s) | Frozen? |
|---|---|---|
| Balance truth | `BalanceCalculator`, `AccountBalanceService` | **Yes** |
| Institution identity | `InstitutionRegistry` | No (RC7 extended it) |
| Account identity | `DeterministicAccountIdentityResolver` | No (RC7/RC8 extended it) |
| Duplicate detection (scoring) | `ReconciliationEngine` | **Yes, scoring specifically** — but `explainScoring()` (RC9, additive, read-only) is not |
| Category classification (live) | `feature/merchant/*` + `CategoryIntelligenceEngine` | No |
| Category classification (dead) | `core/domain/service/transaction/CategoryResolver` | N/A — intentional scaffolding, always returns null |
| Merchant identity for history grouping | `core/domain/service/transaction/MerchantResolver` (System B) | No, but read by a real screen — see `MERCHANT_ARCHITECTURE.md` before touching |
| Relationship inference | `RelationshipEngine`, `RelationshipResolvers.kt` | **Yes** |
| Recurring/Forecast | `RecurringScheduleAnalyzer`, `ForecastEngine` | No (new, RC8) |
| Analytics/Dashboard shaping | `GetFinancialAnalyticsUseCase` | **Yes** |
| Learned memory | `LearnedMerchantCategoryStore`, `LearnedDecisionStore` | No |
| AI infrastructure | `feature/ai/**` | No (but `AIOrchestrator`'s pipeline shape — cache→retry→fallback→validate→audit→trace — should not be casually altered) |
| Diagnostics/Developer Console | `feature/debug/presentation/**`, `DiagnosticCollector` impls | No |

**Frozen means**: changes require a specific, demonstrated bug — not general
cleanup or refactoring taste. Every frozen-file change actually made across
RC6-RC9 (the HDFC currency-mixing fix, the three cross-currency summation
fixes, the `GetFinancialAnalyticsUseCase.computeNetWorth()` delegation fix)
followed this rule and is documented in `FINANCIAL_ENGINE.md`/`CLAUDE.md`
with the evidence that justified it.

## Areas safe to modify without special caution

- Anything under `feature/*/presentation` (screens/ViewModels) not listed
  above as frozen-adjacent.
- `core/domain/service/intelligence/**` (all RC8/RC9-era, designed to be
  extended — new decision types, new `RecurringKind` values, etc.).
- `feature/ai/**` except `AIOrchestrator`'s core pipeline shape.
- New `DiagnosticCollector` implementations (additive by design — one new
  `@Binds @IntoSet` function, see `DiagnosticCollectorModule.kt`).
- `InstitutionRegistry`'s seed data (new institutions — pure data, no logic change).
- `MerchantRegistry`'s seed data (new merchant profiles — same).

## Areas requiring caution

- **Balance/reconciliation/relationship/analytics frozen files** (table above) — real evidence required, not "cleaner code."
- **`core/domain/service/transaction/MerchantResolver` (System B)** — looks like dead scaffolding, is NOT; `TransactionDetailsViewModel` reads its output for a real "other purchases from this merchant" feature. Read `MERCHANT_ARCHITECTURE.md` before touching.
- **`TransactionEntity`/`AccountEntity` schema** — real FKs, real indices, real migration history (currently at DB version 10). Any column change needs a new migration, not a destructive fallback.
- **`AIOrchestrator`'s pipeline order** (cache → retry → fallback → validate → cache-write → audit → debug-trace) — every AI-adjacent class assumes this exact sequence; changing it without updating `INTELLIGENCE_ENGINE.md`'s AI Boundaries section risks silently reintroducing a bypass.

## Technical debt eliminated (RC9)

- `core/domain/repository/CategoryRepository.kt`, `RoomCategoryRepository.kt`, `core/domain/model/Category.kt` — a genuinely dead application-layer wrapper (zero call sites beyond self-referential DI binding) around a table (`categories`) that's real (has a live FK from `transactions`) but never seeded or read. The Room entity/DAO were deliberately KEPT (FK risk, see `MERCHANT_ARCHITECTURE.md`); only the unused wrapper above it was removed.
- `app/src/debug/.../DeveloperConsoleScreen.kt` — a second, older Developer Console superseded by `DebugConsoleScreen.kt`, confirmed zero callers anywhere.
- `core/model/*` (21 files) — a flat, non-domain model set already flagged dead in `CLAUDE.md`, re-confirmed via fresh grep (zero imports), removed along with its one orphaned test (`TransactionDomainTest.kt`, which only exercised the dead package).
- `RelationshipType.UNKNOWN_RELATIONSHIP` — a confirmed-dead enum value (zero resolvers ever produce it, zero other references).
- A real N+1 performance bug in `IntelligenceInspectorViewModel` (RC8/RC9's own new code) — `ConfidenceGate.shouldConsultAi()` was called once per displayed transaction (up to 25×), each call re-reading a DataStore flow; fixed by fetching the threshold once per screen load.

## Technical debt remaining (documented, not fixed — see individual docs for why)

1. **Two merchant/category systems** (`feature/merchant` vs `core/domain/service/transaction`) — real, load-bearing, NOT safely mergeable this RC (functional risk: merging would change "merchant history" grouping behavior, failing the "identical functionality" bar). Full investigation and reasoning in `MERCHANT_ARCHITECTURE.md`. One safe, narrow follow-up identified: normalize System B's exact-text Brand lookup (case/whitespace) — not done, flagged only.
2. **`ReconciliationEngine`'s per-pair score breakdown** was logged-only until RC9 added `explainScoring()` (additive, read-only, zero behavior change) for the Intelligence Inspector — the underlying scoring/threshold logic itself remains untouched and un-auditable outside that one new diagnostic entry point.
3. **`ForecastEngine`/`CategoryIntelligenceEngine` are debug-only-reachable** — real, working, but not wired into any user-facing screen. The actual product payoff (upcoming bills on the Dashboard, a category-confidence indicator on Transaction Details) doesn't exist yet.
4. **CSV import / manual entry** — `IngestionSource.CSV`/`.MANUAL` remain unused placeholders. The seams for adding them (`SourceAdapter`, `TransactionCandidate.source`) already exist and don't need refactoring first — see `FINANCIAL_ENGINE.md`'s Financial Events section.
5. **`core/domain` importing `feature/*`** — pervasive (15+ files), confirmed via this RC's dependency review. Not a regression, not fixed — a stable, longstanding characteristic where `feature/*` hosts shared domain-adjacent services (relationship inference, merchant intelligence, the capture pipeline), not just UI. A true DDD-clean domain layer would require moving several `feature/*` packages into `core/domain`, which is a large, un-scoped refactor with no demonstrated bug behind it — explicitly out of place in a stabilization RC.
6. **Debug-only Compose lists lack `key = { it.id }`** (`DebugConsoleScreen`, `BalanceInspectorScreen`, `IntelligenceInspectorScreen`, `AiMetricsScreen`, `AiDebugScreen`, `PipelineDiagnosticsScreen`, `DiagnosticsScreen`) — confirmed via this RC's performance review. Real user-facing screens (`DashboardScreen`, `TransactionsScreen`) already do this correctly. Low real-world impact (debug builds only, small bounded lists) — not fixed, noted for completeness.
7. **Two Room-facing merchant "brand" concepts share the word "brand" but are unrelated**: `BrandEntity.brandKey` (System B, hardcoded `"manual"`, its doc comment claiming it "matches `LedgerBrandRegistry`" is confirmed FALSE) vs. `core/designsystem/component/LedgerBrandRegistry` (a UI-only icon/color resolver that never reads `brandKey`). Documented in `MERCHANT_ARCHITECTURE.md`; the stale comment was not corrected in code this RC (a one-line fix, low priority, noted for RC10).

## Risks identified

- **No Robolectric** — every JVM unit test must fake Android-framework dependencies by hand. This is a real, structural constraint on how much confidence any single test suite run can provide for Android-lifecycle-dependent code (Activity/Service/BroadcastReceiver behavior is untested by `testDebugUnitTest`).
- **No CI workflow configured in this repo** (confirmed in `CLAUDE.md`) — `./gradlew test`/`assembleDebug` are run manually by whoever is working on the branch. A regression only surfaces if someone runs the full suite before committing.
- **Uncommitted work across multiple RCs** — as of RC9, RC4 through RC9's entire body of work sits uncommitted in the working tree of the `feature/ldl-foundation` checkout at `C:\ledger` (see the RC7 section of `CLAUDE.md` for the worktree-vs-checkout discovery). This is a real, standing risk: any git operation that discards working-tree state (accidental `git checkout .`, a bad merge, disk failure) would lose all of it. Not a code-quality risk, but a real operational one worth flagging explicitly.

## Recommendations

1. Commit the accumulated RC4-RC9 work (or otherwise persist it) before any further large-scale change — see the risk above.
2. Pick ONE of `INTELLIGENCE_ENGINE.md`'s Suggested RC9 items (wiring `ForecastEngine` into the Dashboard) as the first "Product Renaissance"-adjacent milestone, since it's the one piece of RC8/RC9 infrastructure with a clear, ready product payoff and zero remaining architectural blockers.
3. Defer the merchant-system merge (`MERCHANT_ARCHITECTURE.md`) until/unless the "other purchases from this merchant" feature is prioritized for improvement — it's the only thing standing between "leave alone" and "worth merging."
4. Add a CI workflow (even a minimal one running `./gradlew test`) before the UI/UX redesign phase begins — the redesign will touch many files across many PRs, and manual-only verification doesn't scale well against that.

## Suggested next milestone

Per the user's own stated intent: shift primary focus to **Project
Renaissance** (design system, UX research, premium experience). Backend work
becomes incremental, targeted at whatever the redesign specifically needs
(e.g., if the redesign wants to show "upcoming bills" on a new Dashboard
layout, THAT'S when `ForecastEngine` gets wired in — pulled by a real UI
need, not built speculatively ahead of it).

## Final Architecture Assessment

See the Product Readiness Scorecard below for the full, evidenced rating.
Summary: the backend is genuinely stable enough to support a UI/UX redesign
without foundational rework. Every financial number traces to one documented
rule (Financial Truth, `FINANCIAL_ENGINE.md`). Every intelligence decision
carries a confidence + reason (`INTELLIGENCE_ENGINE.md`). The two pieces of
debt that would matter most to a redesign — CSV/manual entry not existing,
and merchant identity being split across two systems — are both scoped,
documented, and don't block UI work (neither is on the pipeline a redesign
would touch first).

---

## Product Readiness Scorecard (RC9 Phase I)

Every rating below is backed by evidence already established in this
document or its companions — no inflated scores, no unsupported claims.

| Dimension | Rating | Evidence |
|---|---|---|
| **Architecture** | **Good** | Clear write/read-time split (`PIPELINE_ARCHITECTURE.md`), single balance source of truth, deterministic-first with AI strictly advisory. Held back from Excellent by the confirmed, pervasive `core/domain → feature/*` coupling (not fixed, documented as accepted debt) and the two-merchant-system split. |
| **Code quality** | **Good** | Consistently documented "why," not "what" (every frozen file's doc comment explains its own constraints); real regression tests exist for every RC's changes. Held back by minor duplication (category logic in two places, by design) and the stale `LedgerBrandRegistry` comment found this RC. |
| **Maintainability** | **Good** | 8 canonical `.md` docs now exist (`FINANCIAL_ENGINE`, `INTELLIGENCE_ENGINE`, `MERCHANT_ARCHITECTURE`, `PIPELINE_ARCHITECTURE`, `ARCHITECTURE`, `ENGINEERING_STATUS`, plus `CLAUDE.md`/`AI_ENGINEERING_GUIDE.md`/`ARCHITECTURE_LOCK.md` from earlier work) covering every subsystem this RC touched. A new engineer reading `CLAUDE.md` → `ARCHITECTURE.md` → the relevant deep-dive doc can orient in well under a day, which was this RC's explicit success criterion. |
| **Testability** | **Needs Work** | No Robolectric — Android-framework-adjacent code (receivers, services, notification listener) is structurally hard to unit test and isn't. Plain-JVM domain/service logic IS well-tested (every RC added real tests, not just compiled code). The gap is specifically at the Android integration boundary, not business logic. |
| **Debuggability** | **Excellent** | This is the standout dimension across RC5-RC9: `PipelineTracer`, `FinancialTraceCollector`/Balance Inspector, `AiDebugTraceStore`, and now the Intelligence Inspector (merchant/category/relationship/recurring/forecast/learning/duplicate reasoning, all in one screen) give a developer more inspectable state than most apps this size ever build. |
| **Performance** | **Good** | Room indices confirmed correct on every hot query column (`account_id`, `timestamp_millis`, `fingerprint` unique, composite origin+tail). Main user-facing lists use proper Compose keys. One real N+1 was found AND fixed this RC (Intelligence Inspector's confidence-threshold lookup). Not Excellent only because debug-tooling lists still lack keys (low-impact, documented, not fixed). |
| **Scalability** | **Needs Work** | Balance replay (`AccountBalanceService.currentBalances()`) and several RC7-9 diagnostics/intelligence services call `observeAllTransactions()` — full history, every call, no pagination. Fine at hundreds-to-low-thousands of transactions; a multi-year power user (CLAUDE.md itself flags "a user with 3+ years of SMS history could have 10,000+ transactions" as the realistic ceiling) is the point this would need revisiting. Not a bug today, a real scaling question for later. |
| **Developer Experience** | **Good** | Consistent patterns are easy to follow once learned (Hilt `@Binds`/`@IntoSet` multibinding for every extensible seam — parsers, extractors, diagnostic collectors, AI providers). The one friction point this RC hit directly: no CI, so build/test verification is manual and easy to skip. |
| **Explainability** | **Excellent** | The RC9 mission's own bar ("every financial decision should be explainable") is met: every stage in `PIPELINE_ARCHITECTURE.md`'s 14-stage audit either has confidence+reason today, or the gap is explicitly named (3 real gaps found and documented, one partially closed this RC via `ReconciliationEngine.explainScoring`). |
| **AI integration** | **Good** | Textbook advisory-only design — cache/retry/fallback/validate/audit/debug-trace, one real call site, gated by confidence, never a write path. Not Excellent yet only because it has exactly one live consumer (`CategoryIntelligenceEngine`) — the infrastructure is broader than its current usage, which is a deliberate, documented choice (RC5-RC6 built ahead of Phase C wiring) rather than a flaw. |
| **Determinism** | **Excellent** | The core mission of RC5-RC9: balances, accounts, reconciliation, currencies, and transfers are 100% deterministic with zero AI involvement, verified structurally (frozen-file discipline) not just by convention. AI touches exactly one non-authoritative surface. |

**No dimension rated Critical.** The two "Needs Work" ratings (Testability,
Scalability) are both real, both already understood, and both explicitly
deferred rather than hidden — consistent with this RC's own instruction to
report honestly rather than inflate.

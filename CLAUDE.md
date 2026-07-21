# Ledger — Project Context

Offline-first Android personal-finance app. Kotlin, Jetpack Compose, Hilt, Room, MVVM+Clean Architecture. Package `com.sherif.ledger`. Work happens on `feature/ldl-foundation`. **This checkout is `C:\ledger` — a separate git worktree elsewhere on this machine (`.claude/worktrees/*`) is stale and missing most of this project's history; if a session's default working directory isn't `C:\ledger`, use absolute paths into `C:\ledger` explicitly rather than trusting the default cwd.**

## Token & context discipline (standing practice)

This file is auto-loaded into every session's context, every message, forever — it is the single most expensive piece of "always-on" context in this project. It previously grew to ~275 lines / ~29,000 tokens (RC4 through RC9 narrative appended in full) and got truncated by a Read tool call, which is what triggered this section. Rules going forward:

- **Narrative/delivery-log content about a specific RC belongs in that RC's canonical doc (see index below), not here.** If a canonical doc doesn't exist yet for the work, put the narrative in `SESSION_HISTORY.md` instead of this file. This file gets at most a 1-2 line pointer per RC.
- **Only put in this file what EVERY future session needs to see immediately**: build commands, currently-frozen files, currently-true reachability facts, currently-open deferred items, the canonical-docs index. Anything else is one Read/Grep away when actually needed — don't pre-pay for it every message.
- **Prune, don't just append.** When a "Deferred, not forgotten" item is resolved, remove it (or fold it into the relevant canonical doc's history) rather than leaving it to accumulate.
- **If a large multi-phase piece of work is starting (a new RC-sized spec), consider suggesting a fresh conversation for it** rather than continuing an already-long thread — a new session starts with maximal context headroom, which both delays auto-compaction and keeps this project's context budget predictable. This can't be done unilaterally (the user drives when a new conversation starts), but it's worth naming as an option when a session is already long and a large new spec arrives.
- **Prefer targeted Grep/Read over broad exploration when the target is already known** (e.g. from an existing canonical doc) — re-deriving something already documented wastes tokens on both the investigation and the restatement.
- **Before dispatching a research subagent, check whether the answer is already in a canonical doc.** RC9 caught itself nearly re-investigating something RC8 already covered; the canonical docs exist specifically so this stops happening.
- Note on the mechanism itself: automatic context compaction (summarizing older turns when a session's context fills up) is a harness-level behavior, not something configurable from inside a session — there's no in-session setting to change when it triggers. The actionable levers are the ones above: keep the auto-loaded project context small, avoid redundant investigation, and split very large work across sessions when practical.

## Documentation freeze — Version 1.0 (in effect 2026-07-21)

The canonical specification set is **frozen at v1.0** and considered stable: `LEDGER_MASTER_SPECIFICATION.md`, `ENGINEERING_HANDBOOK.md`, `ECOSYSTEM_SPECIFICATION.md`, `GOVERNANCE.md`, `DESIGN_RATIONALE.md`, `FUTURE_RESEARCH.md` (and the derived `LEDGER_CONSTITUTION.md`). **Do NOT modify any of these during feature implementation.** If implementation reveals a flaw in the specification: (1) raise the issue to the user, (2) propose an ADR (under `docs/adr/`), (3) wait for approval. **Never silently modify the specification while coding.** This rule is itself the reason the freeze status is recorded here, in the session-context layer, rather than inside a frozen document.

## Canonical docs index

Each of these is comprehensive on its own topic — read the relevant one instead of asking "how does X work" from scratch:

- `FINANCIAL_ENGINE.md` — Financial Truth, Institution Registry, Account Resolver, Currency Rules, Balance Calculation, Reconciliation, AI Boundaries (RC7).
- `INTELLIGENCE_ENGINE.md` — Merchant/Learning/Category/Relationship/Recurring/Duplicate/Forecast engines, Confidence Model, Decision Hierarchy (RC8).
- `MERCHANT_ARCHITECTURE.md` — the merchant/category "System A vs System B" investigation and merge decision (RC9).
- `PIPELINE_ARCHITECTURE.md` — stage-by-stage capture pipeline audit: input/output/confidence/reason/fallback/AI-usage/failure-mode per stage (RC9).
- `ARCHITECTURE.md` — visual index, Mermaid diagrams for every major flow (RC9).
- `ENGINEERING_STATUS.md` — the single "state of the codebase" doc: subsystem ownership, technical debt eliminated/remaining, risks, recommendations, Product Readiness scorecard (RC9).
- `SESSION_HISTORY.md` — RC4-RC6 delivery narrative (predates the canonical-doc convention; relocated here from this file 2026-07-21 to keep this file lean).
- **`LEDGER_MASTER_SPECIFICATION.md`** — the single canonical product specification, v1.0 (Parts I–XI, Chapters 1–151): product foundation, financial philosophy, platform/domain architecture, design language, product/screen/component/interaction/AI bibles, Universal Data Ingestion. **Level-1 governance authority** (the product vision lives here; there is no separate vision doc). Produced 2026-07-21 by merging the eleven `part*` drafts (now archived under `docs/spec-archive/`).
- **`LEDGER_CONSTITUTION.md`** — distilled long-lived project principles + the 10 resolved canonical decisions (§13: brownfield, Financial Event as domain model, capability-matrix-is-target, dashboard order, confidence ladder, 9 subsystems, master-spec-as-Level-1, Android-only, currency). **Read this before spec-driven implementation work instead of re-reading the full master spec.**
- `DOCUMENTATION_REVIEW.md` — principal-architect review of the spec set: contradictions, missing requirements, suggested ADRs, risks. The 10 open questions it raised are now resolved (see the resolution note at its top and `LEDGER_CONSTITUTION.md` §13); retained as historical record. Note it still lists genuine *missing engineering details* (§3) — Financial Event schema, Safe-to-Spend/Story/Forecast algorithms, dedup hash — that need ADRs before those capabilities are built.
- `LEDGER_MANIFESTO.md` / `docs/NORTH_STAR.md` — product philosophy (predates the spec set).
- `PROJECT_STANDARDS.md` — git branching, Conventional Commits.
- `docs/KNOWN_RUNTIME_ISSUES.md`, `docs/technical_debt/DB_MIGRATIONS.md`, `docs/adr/` — as named.

## Build & test

- Debug build: `./gradlew installDebug`
- Release build: `./gradlew assembleRelease` — stricter, separate task graph than debug. `compileDebugKotlin` passing is not proof `compileReleaseKotlin` passes; run both before calling something done.
- Unit tests: `./gradlew testDebugUnitTest`
- No Robolectric — plain JVM unit tests, no Android framework classes. Anything needing `Context`/notifications/etc. needs an interface with a real Android-backed implementation so a fake can implement it without touching the framework.

## Critical gotchas

- **Same-package top-level `private object`/`private class` name collision.** `private` restricts which *file* can reference a declaration, but Kotlin still generates a class with that name in the *package* namespace regardless — two files in the same package both declaring `private object Foo` collide. Use `internal`, not duplicated `private`, for a test double needed in more than one file.
- **Expression-bodied functions (`fun x(): Y = try { ... }`) cannot contain bare `return`.** Block-body only. Breaks `compileReleaseKotlin` while `compileDebugKotlin` passes — don't trust a debug-only compile as proof.
- **Build-variant source sets do real work.** `app/src/debug/` and `app/src/release/` both define their own `DebugNavGraph.kt` — release's is a deliberate no-op stub. Check both source sets before adding Developer-Console-adjacent features.
- **A fake repository's `Flow<...> = flowOf()` (zero args) is a genuinely empty flow.** `.first()` on it throws `NoSuchElementException` immediately. Fakes should emit a real value or `LedgerResult.Success(emptyList())`.
- **Never import `androidx.compose.foundation.layout.weight` explicitly** — `Modifier.weight()` resolves implicitly inside `Row`/`Column`; a bare top-level import breaks it ("internal in file"). Hit multiple times across different files.
- **Before assuming a failing test is a regression, check whether it ever ran before.** A test that never compiled until now isn't a new break.

## Frozen / protected — don't refactor without evidence a bug exists

`BalanceCalculator.kt`, `AccountBalanceService.kt`, `GetFinancialAnalyticsUseCase.kt`, `RelationshipResolvers.kt`, `RelationshipEngine.kt`, `ReconciliationEngine.kt` (scoring specifically — `explainScoring()`, RC9, is a purely additive read-only exception, not a relaxation), `core/domain/service/transaction/MerchantResolver.kt` (**System B**, wired into the live `InsertTransactionUseCase` write path — **NOT** `feature/merchant/MerchantResolver.kt`, a different class with the same name that IS safe to extend, see `MERCHANT_ARCHITECTURE.md`), `core/domain/service/transaction/MerchantRegistry.kt`-adjacent concerns (same disambiguation — `feature/merchant/MerchantRegistry.kt`'s seed data is explicitly safe to extend), `PipelineTracer.kt`/`PipelineTrace.kt`, `ExtractionRegistry.kt`, `ConfirmationMatcher.kt`. Deliberately-tuned scoring/matching logic — changes need a specific, demonstrated bug.

`core.common.diagnostics.PipelineTracker`/`RealPipelineTracker`/`PipelineEvent` is confirmed dead (nothing writes to it) but deliberately left in place so `app/src/debug` still compiles — don't build on it. Live system: `feature.diagnostics.PipelineTraceSink`.

## Feature reachability — verified, not assumed

- **Split (Ledger Split)**: full backend, zero UI/route. Not reachable.
- **Transaction Notes**: backend correct, `TransactionDetailsScreen.kt` doesn't render/edit it. Not reachable.
- **Notification actions (Split/Add Note/Undo)**: built and wired, but `POST_NOTIFICATIONS` is never requested at runtime — effectively unreachable on Android 13+ without a manual Settings grant.
- **Developer Console**: reachable (Profile → Developer Console), debug builds only. Real console is `DebugConsoleScreen.kt` — a second, older `DeveloperConsoleScreen.kt` was confirmed dead and removed in RC9.
- **Ledger Diagnostics, Balance Inspector, AI Metrics/Debug, Intelligence Inspector**: all reachable from `DebugConsoleScreen`'s top bar, debug builds only.
- **Adjust Starting Balance**: Profile → Adjust Starting Balance, all builds.
- **Profile Setup**: shown on first launch. **Edit Profile**: Profile screen button, both reachable.
- **Review Inbox**: Profile → "Review Uncategorized Transactions", reachable, backed by `ReviewInboxViewModel`.
- **AI Settings**: Profile → "AI Settings", permanently reachable (not debug-only).
- **`ForecastEngine`/`CategoryIntelligenceEngine`**: only reachable from the debug-only Intelligence Inspector — not wired into any user-facing screen yet (top recommended next step, see `ENGINEERING_STATUS.md`).

Before marking a feature "done": confirm backend → ViewModel → screen → navigation → actually tappable.

## Deferred, not forgotten (currently open)

- Git commit hash in `AppInfoCollector`/App Info needs a `build.gradle.kts` `buildConfigField` change — not attempted without the ability to compile-test it.
- `app/release/*.apk` and baseline profile `.dm` files got committed to git by accident at some point — should be `.gitignore`'d.
- Git history on `feature/ldl-foundation` is squashed/mixed — don't assume one commit equals one atomic change.
- RC5/RC6 AI infrastructure is built but never wired into live capture (`ProcessNotificationUseCase` untouched) — explicitly a later phase per the user's own roadmap.
- `direction=OUTGOING` extracted from text saying "credited" (the original HDFC message) is still unfixed — doesn't affect balance (currency guard zeroes the effect regardless) but is a real extraction bug. See `FINANCIAL_ENGINE.md` Suggested RC8.
- Whether the HDFC currency-mixing fix (RC6/RC7) fully explains the user's remembered SMS-tally gap is unconfirmed — `BalanceTraceReport.transactionContributions` (RC7) now provides the itemized comparison capability if the user wants to close this out.
- The merchant/category system split (`feature/merchant` vs `core/domain/service/transaction`) is fully investigated (`MERCHANT_ARCHITECTURE.md`) and deliberately NOT merged — a merge would change real behavior. One safe, narrow follow-up identified there (normalize System B's exact-text Brand lookup) but not done.
- CSV import, manual transaction entry, a real `FinancialEvent` domain type, structured parser-failure tracking — none built. Seams exist (`IngestionSource`, `SourceAdapter`) and don't need refactoring first. See `FINANCIAL_ENGINE.md`/`INTELLIGENCE_ENGINE.md` Suggested-next-RC sections.
- `BrandEntity.brandKey`'s doc comment claiming it "matches `LedgerBrandRegistry`" is confirmed false (always hardcoded `"manual"`) — a one-line comment fix, not done.
- Debug-only Compose lists (Balance/Intelligence Inspector, AI Metrics/Debug, Pipeline/Ledger Diagnostics) don't use `key = { it.id }` in `items()` — main-app screens already do this correctly. Low impact, not fixed.
- `RC4-RC9`'s work was uncommitted for a long stretch (discovered as a real operational risk in RC7, resolved by committing in RC9 — `git log` now reflects it). If a similar multi-RC uncommitted pile starts building up again, commit sooner rather than letting it compound.

## Test suite status

All unit tests pass as of RC9 (2026-07-20/21), including the corpus regression suite and `regression.PipelineCorpusRunnerTest`. `compileReleaseKotlin` verified separately from `compileDebugKotlin`. Run `./gradlew testDebugUnitTest` to confirm current state before trusting this line — it drifts as work continues.

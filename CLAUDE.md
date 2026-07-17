# Ledger — Project Context

Offline-first Android personal-finance app. Kotlin, Jetpack Compose, Hilt, Room, MVVM+Clean Architecture. Package `com.sherif.ledger`. Work happens on `feature/ldl-foundation`.

## Build & test

- Debug build: `./gradlew installDebug`
- Release build: `./gradlew assembleRelease` — compiles a stricter, separate task graph than debug. A change that passes `compileDebugKotlin` is not proven to pass `compileReleaseKotlin`; run both before calling something done.
- Unit tests: `./gradlew testDebugUnitTest`
- No Robolectric — unit tests are plain JVM, no Android framework classes available. Anything that must be faked in a test (notifications, `Context`, etc.) needs an interface with a real Android-backed implementation, so the fake can implement the interface without touching the framework.

## Critical gotchas (each one caused a real broken build this session)

- **Same-package top-level `private object`/`private class` name collision.** `private` on a top-level declaration restricts which *file* can reference it, but Kotlin still generates a class with that exact name in the *package's* namespace regardless. Two files in the same package declaring `private object Foo` collide — "Redeclaration" / "cannot access, it is private in file" errors that look unrelated but share this one cause. If a test double is needed in more than one file in the same package, put it in its own file without `private` (use `internal`) instead of duplicating it.
- **Expression-bodied functions (`fun x(): Y = try { ... }`) cannot contain bare `return`.** Only legal in a block body (`fun x(): Y { return try { ... } }`). This specifically broke `compileReleaseKotlin` while `compileDebugKotlin` passed — don't trust a debug-only compile as proof.
- **CRLF varies per file in this repo.** Some files have Windows line endings, some don't — it's not consistent. Match whatever the specific file already uses; don't bulk-normalize across the repo (a wildcard/timestamp-based line-ending pass will silently rewrite files that were never touched).
- **Build-variant source sets already do real work here.** `app/src/debug/` and `app/src/release/` both define their own `DebugNavGraph.kt` and `RealPipelineTracker.kt` — release's are deliberate no-op stubs so debug tooling never ships to production. Before adding a "Developer Console" feature or similar, check both source sets, not just `app/src/main`, or you'll duplicate work and get a "Conflicting overloads" compile error.
- **`git log` from WSL and PowerShell against the same checkout can disagree on paths.** Backslash paths that are correct in PowerShell resolve to nothing in WSL's git. If a git command returns suspiciously empty, check which shell it ran in before concluding the history doesn't exist.
- **A fake repository's `Flow<...> = flowOf()` (zero arguments) is a truly empty flow, not "no data yet."** `DeterministicAccountIdentityResolver` calls `accountRepository.observeAllAccounts().first()` unconditionally — `.first()` on a zero-emission flow throws `NoSuchElementException` immediately. Fakes should emit `flowOf(LedgerResult.Success(emptyList()))` or a real list, never bare `flowOf()`, for any flow something might call `.first()` on.
- **Before assuming a failing test means you broke something, check whether it ever ran before.** Every compile attempt earlier this session failed before `testDebugUnitTest` could execute anything — so failures that first appeared once compilation finally succeeded could be pre-existing and simply never run until now, not new regressions. Diff the test file and the production code it exercises against `git show a9206b5:<path>` (the original pre-session base) before assuming a change caused a failure.

## Frozen / protected — don't refactor without evidence a bug exists

`BalanceCalculator.kt`, `AccountBalanceService.kt`, `GetFinancialAnalyticsUseCase.kt`, `RelationshipResolvers.kt`, `RelationshipEngine.kt`, `ReconciliationEngine.kt` (scoring specifically), `MerchantResolver.kt`, `MerchantRegistry.kt`, `PipelineTracer.kt`/`PipelineTrace.kt`, `ExtractionRegistry.kt`, `ConfirmationMatcher.kt`. These encode deliberately-tuned scoring and matching logic. Changes here need a specific, demonstrated bug, not general cleanup.

`core.common.diagnostics.PipelineTracker`/`RealPipelineTracker`/`PipelineEvent` (old system) is confirmed dead — nothing writes to it anymore — but is deliberately still present, not deleted, so `app/src/debug`'s existing references still compile. Don't build anything new on it. The live system is `feature.diagnostics.PipelineTraceSink`, eagerly populated by `ProcessNotificationUseCase` on every real capture.

**Removed** (was here, no longer exists): `feature.capture.parsing.extraction.{TransactionExtractor,AmountExtractor,MerchantExtractor,CurrencyExtractor,TransactionTypeResolver}` — a second, older, entirely unused extraction pipeline, confirmed via zero production instantiations and zero DI wiring (grepped all of `app/src/main`, including every `core/di` module). Deleted along with its two dedicated tests (`ExtractionEngineTest.kt`, `AmountExtractorTest.kt`) rather than patched, because nothing in the app called it — patching a bug nobody reaches doesn't change app behavior, and the project's own standard is no duplicated code. `TextNormalizer`, `PatternEngine`, `MerchantNormalizer`, `ExtractionHelpers` (same package) are NOT dead — those are shared by the live `AdcbParser`/`GenericBankParser`/`HeuristicExtractor` path and were left untouched.

The bug the dead `AmountExtractor` had — `Regex.find()` grabbing the *first* number in the text (e.g. "ending in **1234**" before "AED **1,250.50**") instead of the actual amount — was checked against the live path and does NOT exist there: `ExtractionHelpers.extractAmountMinor` (used by `HeuristicExtractor`, the real extractor) is currency-anchored (`AED|USD|INR|Rs...` must precede the digits) specifically to avoid this. Verified, not assumed: added a new test to `HeuristicIntentTest` — "amount extraction ignores an account digit run that precedes the currency amount" — reproducing the exact scenario against the live extractor — passes. The parallel merchant-name-truncates-on-period question (dead `MerchantExtractor` turned "Amazon.ae" into "Amazon") was never resolved for the live `HeuristicExtractor.extractMerchant` either way — no corpus fixture asserts merchant name content, so this is unverified territory on the live path, not a confirmed bug.

## Feature reachability — verified, not assumed

Code existing in the repo is not the same as a user being able to reach it. Current status:

- **Split (Ledger Split)**: domain model, database, repository all real and tested. Zero ViewModel, zero screen, zero navigation route. Not reachable by any user action.
- **Transaction Notes**: backend correct (real `note` column, working `updateNote`), but `TransactionDetailsScreen.kt` doesn't render or edit it anywhere. Not reachable.
- **Notification actions (Split/Add Note/Undo on capture)**: built and wired, but `POST_NOTIFICATIONS` is never requested at runtime. On Android 13+ this defaults to denied and stays denied without a manual Settings grant — the feature is effectively unreachable on a fresh install until a runtime permission request is added.
- **Developer Console**: reachable, confirmed end-to-end (Profile → Developer Console). Debug builds only, by design.
- **Ledger Diagnostics** (RC4): reachable from Developer Console's top bar, debug builds only. `Export Diagnostic Bundle` produces a shareable zip via a real `FileProvider`.

Before marking any new feature "done," confirm it end-to-end: backend → ViewModel → screen → navigation → actually tappable from somewhere a user would be.

## Open investigation — the actual next task

The Dashboard's Total Balance has shown several different incorrect values across sessions (~44k, ~52k, ~56k). Two confirmed, previously-unfixed bugs have been patched: a liability self-match double-count in `AccountBalanceService`, and a scoring bug in `DeterministicAccountIdentityResolver` that awarded a type bonus with no type evidence (the latter cannot change any binding decision under current thresholds — a real fix, but not a likely cause). Whether these fully explain the discrepancy is **not yet confirmed**.

**Immediate next step ("Phase B"):** install the debug build on a real device, open Profile → Developer Console → Ledger Diagnostics → Export Diagnostic Bundle, and get the resulting `ledger_diagnostic_*.zip` onto the dev machine for analysis. That bundle (`financial_trace.json` especially) is the actual evidence needed — not further code inference. Don't propose new balance-related fixes without it.

## Test suite status

All unit tests pass. The 8th previously-failing test (`ExtractionEngineTest`) is gone, not fixed-in-place — it tested the now-deleted dead extraction pipeline (see "Frozen / protected" above). `compileReleaseKotlin` also verified separately (debug passing is not proof of release, per the gotcha above). Run `./gradlew testDebugUnitTest` to confirm current state before trusting this section — it may drift as work continues.

## Deferred, not forgotten

- Git commit hash in `AppInfoCollector`/App Info: needs a `build.gradle.kts` `buildConfigField` change, deliberately not attempted without the ability to compile-test it.
- `app/release/*.apk` and baseline profile `.dm` files got committed to git by accident at some point — should be `.gitignore`'d, not yet cleaned up.
- Git history on `feature/ldl-foundation` is squashed/mixed (multiple logical changes landed in single commits, sometimes combined with unrelated UI work) — don't assume one commit equals one atomic change when reading `git log`.

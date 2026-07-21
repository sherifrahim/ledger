# Session History — RC4/RC5/RC6 delivery log

Relocated out of `CLAUDE.md` (2026-07-21) to keep that file lean — it's
auto-loaded into every future session's context, so keeping point-in-time
narrative there forever was a real, growing token cost. This file is read
on demand, not automatically. RC7 onward have their own canonical docs
(`FINANCIAL_ENGINE.md`, `INTELLIGENCE_ENGINE.md`, `MERCHANT_ARCHITECTURE.md`,
`PIPELINE_ARCHITECTURE.md`, `ARCHITECTURE.md`, `ENGINEERING_STATUS.md`) and
aren't duplicated here — see `CLAUDE.md`'s canonical-docs index.

## Added — user identity was hardcoded everywhere; local-only Profile Setup added (2026-07-17)

"Sherif Rahim" / "SR" / "sherif.rahim@gmail.com" were hardcoded in exactly two places (confirmed via grep, no others): `ProfileScreen.kt`'s `UserProfileHeader` and `DashboardScreen.kt`'s `DashboardTopBar` avatar. Explicitly scoped with the user first: **local-only** profile (name + email, no password/server/real auth — this app has no networking/auth layer anywhere today, and "real sign-in + cloud sync" was deferred as a separate future decision), shown **first**, before notification-access or SMS-import onboarding.

- New `ProfileSetupScreen`/`ProfileSetupViewModel` (`feature/onboarding/presentation`), gated by a new `UserPreferencesRepository.isProfileSetup` flag (same pattern as `isSmsImported`) and a new `MainViewModel.isProfileSetup`, checked first in `MainActivity`'s launch sequence (before permission/SMS-import checks).
- New `UserProfileViewModel` (`feature/settings/presentation/viewmodel`) is the ONE place name/email/derived-initials are read from — both `DashboardTopBar` and `ProfileScreen`'s `UserProfileHeader` inject it via `hiltViewModel()` rather than each deriving initials separately.
- New reusable `core/designsystem/component/LedgerTextField.kt` (generic free-text LDL input, same visual family as `LedgerSearchBar`/`LedgerAmountInputField`) — used for the name/email fields, not Material's `OutlinedTextField`.
- Minimal validation only, on purpose: non-blank name, email containing `@`. No format/existence validation — there's no server to validate against yet.
- **Edit Profile wired too**: same `ProfileSetupScreen`/`ProfileSetupViewModel`, reused via `isEditMode = true` — reachable from Profile's "Edit Profile" button, which was previously `onClick = { /* TODO */ }`. New `LedgerRoute.EditProfile`.

Before marking any new feature "done," confirm it end-to-end: backend → ViewModel → screen → navigation → actually tappable from somewhere a user would be.

## Resolved — Dashboard Total Balance sign bug

A real `ledger_diagnostic_*.zip` was analyzed (2026-07-17, device OPPO CPH2651). Root cause was never balance *arithmetic* — `database_health.json`'s `balance_reconstruction` check passed. Root cause: `MoneyFormatter.format()` always does `abs(money.minorUnits)` (safe for transaction amounts, which carry direction via a separate field); `DashboardUiState.totalBalance` (net worth) had no such companion field, so its sign vanished between `DashboardViewModel` and the UI.

Fixed: added `DashboardUiState.isNegativeBalance`, consumed in `DashboardScreen.TotalBalanceSection` to color the balance red when negative, matching the existing Accounts-screen convention (`AccountUi.isNegative`). Also fixed the month-over-month change badge being unconditionally styled green regardless of the string's actual sign.

Not fixed, not asked: `AccountsUiState.netWorth` has the identical missing-sign defect but is never rendered — no live bug, left alone.

## Resolved — why import balloons to ~1 year of SMS history (2026-07-17)

`SmsImporter.importHistoricalSms()` queried SMS with only a lower bound (`DATE > lastImportDate`, defaulting to epoch `0`) and no upper bound — unbounded first-run import.

Fixed with a real onboarding range-selection UI: new first step in `SmsOnboardingScreen`/`SmsOnboardingViewModel` (`ImportRangeSelectionScreen`) — This Week (default) / This Month / Last 3 Months / Last 12 Months / Custom Date Range, via a custom-built `core/designsystem/component/calendar/LedgerDateRangeCalendar.kt` (deliberately not Material's `DatePicker`). `SmsImporter.importHistoricalSms(importStartDate, importEndDate, windowLabel)` now takes the window explicitly; the existing `lastSmsImportDate` watermark still applies on top. `ProcessNotificationUseCase.execute()` gained a `ProcessNotificationOutcome` return value (non-breaking) so `SmsImporter` can tally real outcomes. New `UserPreferencesRepository.ImportSummary` + `ImportSummaryCollector` (`import_summary.json` in the diagnostic bundle). Visually verified on-device (OPPO CPH2651).

## Resolved — `openingBalance` is always zero; a bounded import window makes this visible (2026-07-17)

Real-world evidence: "This Month" import showed Dashboard **-AED 8,030.88** vs. actual **+AED 4.3k**. Investigated first — sampled ~20 of 262 discarded messages, confirmed all genuinely non-transactional; `balance_reconstruction` still passed. Root cause: every account-creation path hardcoded `openingBalance = Money.zero(...)` — tolerable when import was unbounded (whole history replayed), broken once a bounded window left real balance history outside it with no anchor.

**Fixed**: new `SeedOpeningBalanceUseCase` — reads the account's current computed balance, takes a user-supplied real balance, persists `openingBalance = existingOpeningBalance + (actualBalance - computedBalance)`. Documented as a deliberate ONE-TIME-PER-USE exception to "opening balance never mutated afterward." Two entry points: (1) "Confirm Starting Balance" step in SMS onboarding (skippable), (2) **standalone, reachable anytime**: Profile → "Adjust Starting Balance" — added because the onboarding step only runs once per install, and a rebuild-reinstall cycle was directly observed skipping it.

Also fixed: `DashboardScreen.TotalBalanceSection`'s month-over-month badge rendering one character per line on real (wide) device data — fixed by stacking the badge below the balance instead of beside it.

## Diagnosed, not a bug — Merchant Intelligence "UNKNOWN" categories (2026-07-17)

`MerchantRegistry` is a deliberately small, curated allowlist (~19 major UAE/India chains). Real diagnostic evidence ("Al Madina Fresh Mart," "Cars Taxi," etc.) confirmed these are small independent businesses never in scope — not a bypass or failure.

**Follow-up built**: `feature/merchant/GenericCategoryKeywords.kt` — additive keyword→category fallback (TAXI→Transport, MART/GROCERY→Groceries, RESTAURANT/CAFE→Dining, FUEL→Fuel, PHARMACY/CLINIC→Health), consulted only when `MerchantResolver` returns `Unresolved`. Never overrides a resolved brand's category; still returns `null`/"UNKNOWN" if nothing matches.

## Added — Review Inbox wired to a real learned-categorization system, no LLM (2026-07-17)

User explicitly decided against an LLM for categorization — deterministic learned-mapping instead. Found `ReviewInboxScreen` already existed as a pure UI mockup (hardcoded preview data, zero ViewModel, unreachable) — reused that shell.

- DB migration 6→7: `merchant_category_overrides` table (additive).
- New `feature/merchant/LearnedMerchantCategoryStore` — third/final fallback tier (brand registry → learned override → generic keywords → `"UNKNOWN"`). In-memory cache synced with DB rather than making hot-path analytics functions suspend.
- New `ReviewInboxViewModel`; `ReviewCard.kt` gained a direct category-chip picker. Made reachable: Profile → "Review Uncategorized Transactions".
- Investigated and deliberately NOT reused: `core/database/entity/{BrandEntity,CategoryEntity,MerchantAliasEntity}` + `core.domain.service.transaction.MerchantResolver`/`CategoryResolver` (an older, only-partially-wired persistence layer — `CategoryResolver.resolve()` always returns null). This is the system RC8/RC9 later fully investigated — see `MERCHANT_ARCHITECTURE.md`.

## Added — premium launch sequence + new app icon (2026-07-17)

New launcher icon: bold "L" wordmark, near-black background, `LedgerV3Palette.Azure` blue, from a user-supplied mockup. New `presentation/splash/LedgerSplashScreen.kt` — 5-stage Compose animation (~1.6s, no spin/bounce/particles per the design brief), timings isolated in `SplashTimings`. `androidx.core.splashscreen` wired via `installSplashScreen()` before `super.onCreate()`. Overlaid on top of unconditionally-composed real content so the fade-out is a dissolve, not a cut. **Not visually verified** (no device connected that session).

## RC5 Part 1 — Financial Truth: RESOLVED — not a new bug (2026-07-20)

A diagnostic bundle confirmed a recurrence of the same already-fixed opening-balance pattern, not a new defect. Also found and fixed from a screenshot: `BalanceInspectorScreen`'s `CategoryRowView` unconditionally took `abs()` and only colored liability categories red, hiding a negative ASSET category (an overdrawn Checking total) — same class of bug as the Dashboard sign fix, reintroduced in new code.

## Resolved — a real currency-mixing bug found via live user data (2026-07-20)

User pushed back on "Adjust Starting Balance" as the whole story ("we are only fixing the disease not the root cause") and provided a third diagnostic bundle. Grepping raw `PERSISTING params=` log lines surfaced an HDFC Bank India SMS that `InstitutionRegistry` didn't recognize, falling back to the AED default account — `BalanceCalculator.effect()` had no currency check, so the INR amount was summed as if AED.

**Fixed**: `BalanceCalculator.effect()` gained an optional `accountCurrencyCode` param — contributes zero effect (logged as an error) on mismatch instead of mixing units. This was the root cause RC7 later closed properly at the institution-recognition layer — see `FINANCIAL_ENGINE.md`.

**Still open at the time**: whether this fully explained the user's remembered SMS-tally gap was unconfirmed. (RC7's `BalanceTraceReport.transactionContributions` later provided the itemized comparison capability.)

## RC5 Parts 2/3 — Balance Inspector + Balance Trace expansion (2026-07-18)

`BalanceInspectorViewModel` reuses (never recomputes) `FinancialTraceCollector.buildReport()` and `GetFinancialAnalyticsUseCase.computeNetWorth()`, diffing them (`mismatchMinor`) as the actual "Dashboard vs. traced calculation" explainability mechanism. New Developer Console page, reachable via a `DebugConsoleScreen` top-bar icon. "Pending Transactions"/"Hidden Accounts"/"Ignored Accounts" shown as "Not tracked in this version," never a fabricated zero. `BalanceTraceReport` gained `excludedAccounts` (soft-deleted, with reason) — required a new `AccountRepository.getDeletedAccounts()`, which touched 10 test fakes.

## RC5 Parts 4-12 — AI infrastructure foundation (2026-07-18)

Built in parallel with Part 1 staying open, per explicit user direction that AI infra doesn't depend on the balance issue. **Not wired into live capture** — `ProcessNotificationUseCase` untouched.

All new code under `feature/ai/`. `LLMProvider` interface — OpenAI/Groq/OpenRouter/Ollama/LM Studio share one wire format (`OpenAiCompatibleProvider`), Anthropic/Gemini have their own (`AnthropicProvider`/`GeminiProvider`), bound via Hilt `@IntoMap`. `AIContextBuilder` — the only place a `Transaction` is reduced to a minimal structured context. `PromptLibrary` — one prompt per `AICapability`, shared JSON-response contract. `CapabilityRegistry` — resolves provider per capability from settings. `AISuggestion` — one generic shape across every capability. `AIOrchestrator` — checks enabled → provider → API key → prompt → call → parse → audit log. `SecureApiKeyStore` via `EncryptedSharedPreferences`. New `ai_audit_log` table (excludes API key/prompt/response). Static `AiCostTracker`. AI Settings screen (Profile → "AI Settings", permanently reachable). New deps: `security-crypto`, `okhttp`. `AndroidManifest.xml` gained `INTERNET` permission (the one unavoidable footprint change to a previously fully-offline app).

**Caught before shipping**: first draft of the API key field used `LedgerAmountInputField` (money-only regex) — would have stripped every non-digit character from a real key. Switched to `LedgerTextField`. **Not verified against any live endpoint** — no API keys in this environment.

## RC6 — Ledger Intelligence Platform additions on top of RC5 (2026-07-20)

Hardening around the same advisory-only `AIOrchestrator` — no new domain-writing capability. `ProcessNotificationUseCase` still untouched.

Retry → fallback → cache → graceful-failure pipeline (`attemptWithRetry()`, 2 attempts, 1s delay; per-capability fallback provider; cache short-circuit; typed `AIOrchestratorResult` sealed result, never a thrown exception). `ConfidenceGate.shouldConsultAi()` — per-capability, user-configurable threshold (default 70). `AISuggestionValidator` — structural validation before any suggestion is returned. Temperature/max-tokens threaded through `LLMProvider.complete()` and all three provider implementations. `AiSuggestionCache` (`ConcurrentHashMap`, TTL, hit-rate tracking). `AiMetricsService` (aggregates over `ai_audit_log`). `AiDebugTraceStore` — deliberately in-memory only (last 20, never persisted) — holds what the DB-backed audit log deliberately excludes (context/prompt/response). Two new Developer Console pages (`AiMetricsScreen`, `AiDebugScreen`).

**Caught during writing**: `AiSettingsViewModel`'s draft `combine()` usage mixed incompatible flow types — fixed via two nested typed 3-way `combine()` calls instead of an unsafe `Array<Any?>` cast.

**Verification**: compiled clean on both variants, all tests passed. No new unit tests added for the AI layer itself (would need HTTP fakes) — a documented gap, not an oversight.

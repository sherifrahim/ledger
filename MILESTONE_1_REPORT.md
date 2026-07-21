# Milestone 1 — Foundation Sprint Report

**Scope:** alignment and cleanup only. No feature functionality, no Financial Event
migration, no speculative code. Guiding principle: brownfield evolution — preserve
working business logic (ADR-0000).
**Verification:** `./gradlew compileDebugKotlin compileReleaseKotlin
testDebugUnitTest` → **BUILD SUCCESSFUL**, all unit tests pass, after every change.

---

## 1. Completed Work

### ADR-000 — Brownfield Evolution Strategy
`docs/adr/0000-brownfield-evolution-strategy.md` (Accepted). Formalizes the
brownfield decision: preserve working logic and Financial Truth; refactor
incrementally; replace only with clear architectural justification; spec = intent,
codebase = status. Also codifies the process the product owner set: **ADRs for
architectural decisions, TDDs for unspecified algorithms** (propose → tradeoffs →
implement after approval); neither blocks independent foundation work.

### Navigation aligned to the specification (spec Chapter 34)
The bottom bar moved from a transaction-first IA (Home / Accounts / Activity /
Insights / Profile) to the **five canonical primary destinations**:

| Destination | Route | Screen |
|---|---|---|
| **Dashboard** | `home` | existing `DashboardScreen` |
| **Story** | `story` (new) | new `FinancialStoryScreen` — honest empty-state scaffold |
| **Review** | `review` | existing `ReviewInboxScreen` |
| **Search** | `search` (new) | new `UniversalSearchScreen` — honest empty-state scaffold |
| **Settings** | `profile` | existing `ProfileScreen` (the control hub) |

- New destinations are **navigation scaffolds with honest empty states** (spec
  Chapter 29/94 — empty states educate, never apologize), **not features**. Story
  timeline generation and universal search are explicitly deferred to later
  milestones. Files carry doc comments saying so.
- **No orphaned screens.** Accounts, Activity (Transactions), and Insights were
  demoted from the bottom bar but remain reachable via a new "Overview" section in
  the Settings/Profile hub (interim; their proper home is the Dashboard, which is
  Phase C product-experience work). Transactions is also still reachable from the
  Dashboard's Recent Activity. The Dashboard's existing `onSearchClick` is now
  wired to the Search destination.
- Every existing screen, route, and the debug navigation graph are **preserved**.
- Files: `LedgerRoute.kt`, `LedgerBottomBar.kt`, `LedgerNavHost.kt`,
  `ProfileScreen.kt`, new `feature/story/presentation/FinancialStoryScreen.kt`,
  new `feature/search/presentation/UniversalSearchScreen.kt`.

### Repository & dead-code cleanup
- Removed the **Budget stub**: empty `BudgetRepository` interface, empty
  `RoomBudgetRepository`, its Hilt binding, and the empty `feature/budgets/`
  scaffolding tree (`data/`/`domain/`/`presentation/`, no files). Zero consumers
  (verified). Budget remains a real future feature per spec — it will be built
  properly when scheduled, not carried as a dead placeholder.
- Fixed the stale, **false doc comment** on `BrandEntity.brandKey` ("Matches
  LedgerBrandRegistry" — it does not; `brandKey` is always `"manual"`).
- Confirmed the previously-flagged stray APK/`.dm` artifacts are already handled
  (`.gitignore` covers `app/release/`; nothing stray tracked).

### Design system / theme / DI audit
Confirmed **healthy — no changes required** (brownfield: don't touch what works):
- **Single design system** (`core/designsystem`: atmosphere / component / haptics /
  theme / tokens). LDL is mature and comprehensive.
- **Single theme entry** (`LedgerTheme`); no competing `MaterialTheme` wrappers
  anywhere in the app.
- **Clean DI**: 11 Hilt modules separated by concern; no duplicate bindings (Hilt
  would fail the build otherwise; it passes).

### Test infrastructure & build stability
- Added **CI** (`.github/workflows/ci.yml`, GitHub Actions): on push to
  main/develop/feature branches and on PRs, runs `compileReleaseKotlin` +
  `testDebugUnitTest` on Temurin JDK 17 with Gradle caching, uploads test reports.
  This closes the "no CI" risk flagged in the gap analysis before the large UI
  build begins. No secrets required (release signing self-skips without a keystore).
- **Build stability verified**: full debug + release compile + unit tests green
  after all Milestone 1 changes.

---

## 2. Remaining Debt

Carried forward deliberately (none blocks the next milestone):

1. **`core/domain → feature/*` coupling** — unchanged; a big-bang fix is out of
   scope for a foundation sprint and has no demonstrated bug. Address incrementally
   during the Financial Event migration and modularization.
2. **Two merchant/category systems** (`feature/merchant` vs
   `core/domain/service/transaction`) — untouched; the natural convergence point is
   the Financial Event migration (`MERCHANT_ARCHITECTURE.md`).
3. **Dead `categories` table** — kept (live FK from `transactions.category_id`);
   removed together with the Financial Event / category migration, not before.
4. **Settings ↔ Profile consolidation** — the Settings destination currently opens
   the Profile hub (titled "Profile"), and the "Overview" section (Accounts/Activity/
   Insights) lives there as an interim access point. Proper consolidation and moving
   those secondary destinations onto the Dashboard is Phase C product-experience work.
5. **Ledger Split** — full backend, still no UI/route (unreachable). Not removed;
   awaiting a product decision (Split is not in the v1.0 spec's primary scope).
6. **Debug-only Compose lists** missing `key = {}` — low-impact perf, unchanged.
7. **No Robolectric** — Android-lifecycle code (receivers/services/listener) remains
   unit-untestable; a larger effort deferred (CI now at least guards the JVM suite).

---

## 3. Blockers

**None for continuing.** Two items require your input before the *feature* work they
gate, but neither blocks the recommended next milestone's early steps:

- **Financial Event model (ADR-001)** — needs approval before the domain migration
  (Milestone 2 core). Recommended as the immediate next deliverable to draft.
- **Financial Story generation** — the defining feature has no algorithm in the
  frozen spec; needs a **TDD** (not an ADR) before the Story screen is built. Not
  needed for the early parts of Milestone 2.

Environment note (unchanged, not a code blocker): no emulator/device is available
in this environment, so the **navigation restructure is compile-verified but not
visually verified**. It should be run on a device before release — the change is
low-risk (routes/screens preserved, additive scaffolds) but the bottom-bar and
hub layout should be eyeballed.

---

## 4. Recommended Next Milestone

**Milestone 2 — Financial Event Foundation** (spec Phase 1 completion; gap analysis
Phase A), sequenced to front-load the gating architecture, then deliver visible value
by wiring engines that already exist:

1. **Draft & approve ADR-001 (Financial Event model)** — schema, one-event→
   many-records, immutability/correction mechanism, `Transaction` coexistence and
   incremental migration path. *Gating; do first.*
2. **Introduce `FinancialEvent` additively** above `Transaction`; balances derive
   from events; migrate reads before writes; keep the existing replay as the
   reconstruction engine. Preserve the frozen Financial-Truth core.
3. **Merchant/category consolidation** behind event resolution (ADR-009); retire the
   dead `categories` wrapper once category ownership is unified.
4. **Confidence-ladder alignment** to the canonical D5 scale (small).

Then, as fast wins that make the built intelligence visible (can begin in parallel
once events land): **wire `ForecastEngine` to a Forecast/Safe-to-Spend surface** and
**build the Merchant page** — both mostly UI over engines that already exist.

Defer the large Financial Story build until its generation **TDD** is approved.

---

## Appendix — Files touched this sprint

**Added:** `docs/adr/0000-brownfield-evolution-strategy.md`,
`feature/story/presentation/FinancialStoryScreen.kt`,
`feature/search/presentation/UniversalSearchScreen.kt`, `.github/workflows/ci.yml`,
this report.
**Modified:** `LedgerRoute.kt`, `LedgerBottomBar.kt`, `LedgerNavHost.kt`,
`ProfileScreen.kt`, `RepositoryModule.kt`, `BrandEntity.kt`.
**Removed:** `BudgetRepository.kt`, `RoomBudgetRepository.kt`, empty
`feature/budgets/` tree.

All changes are uncommitted. Nothing in the frozen specification set was modified.

# Mock-Parity & Delight Build Plan

Handoff doc so a fresh session can continue the UI build without the originating
chat. Read this + `CLAUDE.md` + the memory index, then continue at the next
unchecked item in **Ordered build plan**.

## Goal
Bring the app to parity with the target Figma mocks (dark + light, "Liquid Glass"
aesthetic; ~10 screens per theme). Current overall parity ≈ 55–65%; several
screens/features are missing. After parity, do the iOS-fy pass.

## Decisions (locked with the user)
- **Financial Story:** keep the mock's *analytical monthly-story* format (narrative
  + Income / Expenses / Savings with month-over-month deltas), but render **only
  metrics computable from real data**. Replace the mock's "Emergency Fund = 4.2
  months" with **"X months of expenses covered by balance"** (real). Narrative text
  generated **deterministically** from the real numbers (no fabrication). The
  day/night illustration = a generated gradient, not a stock asset.
- **Real brand logos: APPROVED** by the user (they handle the legal side). Wire a
  brand-logo asset system keyed by merchant; use it in transaction rows, merchant
  page, etc.
- **Charts:** interactive **line + pie**, with **labeled axes** (X labels+values, Y
  labels+values) and **touch-scrub** to reveal the value at a point; real data.
  Build one reusable component, reuse across Insights, Accounts (Balance Trend),
  Forecast.
- **iOS-fy** (continuous "squircle" corners + iOS-style controls): the **finishing
  layer, AFTER** mock parity.
- **Icons:** SF Symbols can't be shipped (proprietary) → Material icons stand in.

## Ordered build plan
1. [~] **Interactive charts** (line + pie, labeled axes, touch-scrub, real data) →
   apply to Insights, Accounts (Balance Trend), Forecast. *(user's explicit first item)*
   - **Done:** `LedgerInteractiveLineChart` + `LedgerInteractivePieChart` (reusable, in
     `core/designsystem/component`) built and wired into **Insights** — labeled axes,
     horizontal touch-scrub w/ callout + haptic, tap-to-select pie slices, real
     analytics data, both themes verified on-device. Commit on branch
     `claude/mock-parity-interactive-charts-f1e36f`.
   - **Deferred (reuse targets not yet buildable):** *Accounts Balance Trend* needs a
     real balance-over-time series that doesn't exist yet — do NOT hand-roll cumulative
     balance in a ViewModel (cross-currency arithmetic must go through
     AccountBalanceService / GetFinancialAnalyticsUseCase; see RC7 Phase C). *Forecast*
     is a whole missing screen (item 3). Drop both charts in as those surfaces land.
2. [ ] **Design-system polish pass** — colored category/brand icon chips, `+N%`
   metric-delta styling (green/red), progress bars, section headers, card spacing.
   Includes **brand logos** + **merchant-name resolution** (fixes the raw-SMS-as-title
   bug, see Follow-ups). This uplifts every screen toward the mock.
3. [ ] **Missing screens**, in value order:
   - [ ] **Forecast** screen — `ForecastEngine` already exists (debug-only); surface it.
   - [ ] **Budgets** — new feature + screen.
   - [ ] **Analytical Financial Story** — per the decision above.
   - [ ] **Dashboard**: Safe-to-Spend (needs an algorithm — propose an ADR first) +
     Upcoming (surface `RecurringScheduleAnalyzer`).
   - [ ] **Insights cards**: Savings Rate, Subscription Summary, AI Insight card.
   - [ ] **Settings sub-screens**: Notifications, Security & Privacy, Data & Sync, Help & Support.
   - [ ] **Merchant** (relationship stars/"since", related merchants) + **Review Queue**
     (All/Categorization/Duplicate tabs + Evidence block) polish.
4. [ ] **iOS-fy**: squircle corners + iOS controls.

## Per-screen gap snapshot (both themes)
| Screen | Parity | Main gaps |
|---|---|---|
| Dashboard | ~50% | Safe-to-Spend + progress bar, Financial Story card, Upcoming, metric-delta Insights rows |
| Financial Story | ~25% | We show a txn feed; mock is a narrative monthly story + metric cards + illustration |
| Merchant | ~70% | Relationship stars/"Since 2021", Related Merchants, richer insight bullets |
| Review Queue | ~65% | All/Categorization/Duplicate tabs, Evidence block (Merchant✓/Time/History) |
| Search | ~70% | Some quick-access (Categories, Forecast), "Try Searching" suggestions |
| Accounts | ~70% | Bank brand marks, interactive balance-trend chart |
| Transactions | ~80% | Brand icons, spacing polish, merchant names (not raw SMS) |
| Insights | ~45% | AI Insight card, Spending-vs-last-month, Savings Rate, Subscription Summary; interactive charts |
| **Forecast** | **0% (missing)** | Whole screen; engine exists, debug-only |
| Settings | ~50% | Budgets/Categories/Notifications/Security/Data&Sync/Help sub-screens + principles panel |

## Feature notes
- **Upcoming** should target **credit-card bills, Tabby/Tamara installments, rent/DEWA**
  — things that DO send SMS / have due dates. Streaming/Prime no longer SMS.
- Bank **credit-card-bill / statement SMS are currently deliberately IGNORED**
  (`AdcbStatementPattern`/`AdcbBillingPattern` → `ParseResult.Ignore`) — correct, a
  bill notice isn't a transaction. To power Upcoming, capture them as a **separate
  "upcoming bill" record** (due date + amount), NOT a transaction.
- **Safe-to-Spend** needs a defined algorithm (flagged missing in
  `DOCUMENTATION_REVIEW.md §3`) — propose an ADR before building.

## Open follow-ups / known issues
- **Raw-SMS row title:** the feed uses `Transaction.rawText` as the row title, so
  *real* captures show raw SMS text (dev-console test data masks this by putting
  clean names in `rawText`). `Transaction` has no merchant name field — resolve from
  `brandId` → Brand. Fold into polish pass #2.
- **Transfer capture:** parser is proven correct (`SmsIngestionTest` regression for
  the real ADCB transfer SMS). On-device miss is most likely environmental
  (live-capture timing/permission); to confirm, re-import an SMS range covering the
  date. `Transaction.isOutflow` now gives outgoing transfers the correct −red sign.
- **Debug-only engines to surface:** `ForecastEngine`, `RecurringScheduleAnalyzer`,
  `CategoryIntelligenceEngine` (reachable only from the debug Intelligence Inspector).

## Ground truth (state as of this handoff)
- Branch `feature/ldl-foundation`, real checkout `C:\ledger`.
- **Liquid Glass**: real **Haze 1.7.2** backdrop blur (`dev.chrisbanes.haze:haze` +
  `haze-materials`), applied to cards, nav island, top bars, grouped surfaces; off by
  default; toggle in Settings → Appearance. Theme is explicit **Light/Dark**. Two
  haze layers: nav island blurs scrolling content (`LocalNavHazeState`); cards/surfaces
  blur an ambient backdrop (`LocalCardHazeState`). See `core/designsystem/theme/LedgerGlass.kt`.
- **Charts today:** static line + donut on Insights, from `GetFinancialAnalyticsUseCase`
  (see `feature/analytics/presentation/InsightsScreen.kt` + its ViewModel).
- **Icon**: green stacked-layers adaptive icon; splash draws the same mark.
- **RC1 release-eng done:** R8 minify + `proguard-rules.pro`; signed release APK ~5.0 MB,
  smoke-tested; Privacy Policy + Open Source Licenses screens (Settings → About);
  dropped Groq `mixtral`. DB is v12.
- Key design tokens: `LedgerRadius`, `LedgerColors`, `LedgerSpacing`, `LedgerAnimations`.

## Still pending before public (user's, non-code)
- Privacy policy: set the contact address (placeholder in-app) + host a public URL for Play.
- Play SMS-permission use declaration (biggest external gate).

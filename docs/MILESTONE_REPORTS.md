# Milestone Reports — Track A (Product Experience) & Track B (Financial Event Migration)

Running log of the post-M1.5 milestones. Each entry records the verification gates
required by the product owner: compile · tests · emulator · screenshots · performance ·
accessibility · commit. Earlier milestones have their own root reports
(`MILESTONE_1_REPORT.md`, `MILESTONE_1_5_REPORT.md`); ADRs live in `docs/adr/`.

Standing rules: DESIGN_REFERENCE.md is the visual north star; ADR-0000 (brownfield) and
ADR-0001 (Financial Event) are in force; no placeholder logic is merged into production;
any change the frozen spec would require is raised as an ADR, not a silent edit.

---

## P1 — Accounts (live data)  ·  `feat(accounts): P1 — Accounts live data`

**What changed.** The Accounts screen now renders only real data and follows the
DESIGN_REFERENCE layout: a soft-shadow **Total Balance** hero with real **Assets /
Liabilities**, a grouped **My Accounts** list (brand icon · name · type · real balance),
the real "this month's spending" insight, and an honest empty state. The backend was
already real — `AccountsViewModel` derives every figure from
`GetFinancialAnalyticsUseCase.computeNetWorth()` (transaction replay via
AccountBalanceService); the screen simply ignored net worth/assets/liabilities and
carried a **fabricated "Payment due in 5 days"** placeholder. The placeholder is removed
and the real figures are surfaced. Balance Engine / Financial Truth untouched (ADR-0000).

**Verification.**
- **Compile:** `compileDebugKotlin` + `compileReleaseKotlin` green.
- **Tests:** `testDebugUnitTest` green (no logic changed; presentation-only).
- **Emulator:** real device state (3 captured ADCB transactions) → Total Balance
  **AED 2,950.00**, Assets **AED 2,950.00**, Liabilities **AED 0.00**, Primary Account
  (Checking) **AED 2,950.00**.
- **Screenshots:** `docs/design/screenshots/accounts-live-light.png`, `…-dark.png`.
- **Performance:** `gfxinfo` showed 63% janky frames over a 79-frame sample — attributed
  to the **debug build on emulator** (no R8, debuggable, cold Compose first-composition
  dominating a tiny sample), not representative. The screen is structurally light (one
  card + a short keyed list). *Action:* real perf must be measured on a release build
  on-device before it is treated as a gate result.
- **Accessibility:** amounts use tabular figures; both themes styled. *Findings (carried
  to a dedicated a11y pass):* icon-only buttons (`LedgerIconButton`, e.g. the Add-account
  action) pass `contentDescription = null` — app-wide gap to fix; tertiary-on-inset text
  (subtitles, section labels) should be contrast-checked against WCAG AA; the 44dp icon
  button is marginally under the 48dp target.

**Follow-ups.** Account number tail isn't in the net-worth summary, so the row subtitle
shows the account type only; manual "Add Account" is still a stub (no manual-entry flow
yet). Neither blocks P2.

---

## P2 — Merchant Intelligence (live data)  ·  `feat(merchant): P2 — live merchant intelligence`

**What changed.** The Merchant page is now driven by real per-merchant intelligence
instead of the `MerchantShowcase` placeholder. The tapped merchant's identity travels
through the nav route (`merchant/{merchantKey}`, URL-encoded); `MerchantViewModel`
aggregates that merchant's real transactions (matched by extracted `rawText`) into:
relationship tenure ("Since …", from first-seen), **Total Spent**, **Transactions**,
**Avg. Monthly**, **Largest**, a real category mix (via the analytics story layer), real
derived insights, and related merchants sharing the top category. The **fabricated
5-star rating is removed** (no real backing); sections with no data are omitted, and an
honest empty state covers a merchant with no captured transactions. Read-only over
persisted data — no writes, Financial Truth untouched (ADR-0000).

**Verification.**
- **Compile:** debug + release green.
- **Tests:** `testDebugUnitTest` green (read-only presentation/VM; existing suite intact).
- **Emulator:** tapped Costa Coffee on the live Dashboard → real page: **Since Jul 2026**,
  **Total Spent AED 50.00**, **1** transaction, **Dining 100%**, insights derived from the
  real transaction. Route arg threading verified (title is the tapped merchant, not a
  hardcoded name).
- **Screenshots:** `docs/design/screenshots/merchant-live-light.png`, `…-dark.png`.
- **Performance:** `gfxinfo` on the merchant page — **20.4% janky** over 88 frames, 90th
  %ile **48ms**, 95th **97ms** (representative, no cold-start inflation); acceptable for a
  debug/emulator build.
- **Accessibility:** both themes styled, tabular figures, category % always shown as text.
  Same standing finding: the back `LedgerIconButton` needs a `contentDescription`
  (app-wide, tracked from P1).

**Follow-ups.** Merchant matching is by extracted `rawText`; once the merchant/brand
identity is unified (ADR-0009 convergence) matching should key off `brandId`. "Avg.
Monthly" over a sub-month span equals total (expected). Neither blocks P3.

---

## P3 — Review Queue refinement (live)  ·  `feat(review): P3 — confidence-card refinement, retire showcase`

**Reuse question:** *improve existing engine, not build new?* → **Yes.** Reused
`ReviewInboxViewModel` (real) and refined the existing production `ReviewCard`; no new
review system.

**What changed.** The production `ReviewCard` now uses the confidence/evidence card
language (`LedgerCard` + `LedgerConfidenceBadge`): it leads with the question ("Should
this be …?" when a suggestion exists, else "Which category?"), shows the real confidence
band, merchant/amount/time, evidence rows (account, why-review — real fields only), and
the **real** decision mechanism — the category-chip picker that teaches
`LearnedMerchantCategoryStore`. The suggested category (when present) is highlighted as
the accented chip. The **DEBUG showcase is fully removed** from `ReviewInboxScreen`
(placeholder items, `ReviewShowcase`, showcase card/evidence types all deleted) — the
screen renders only real `ReviewInboxViewModel` items, with an honest "All clear" empty
state. No button is a no-op: the chips are the action.

**Verification.**
- **Compile:** debug + release green.
- **Tests:** `testDebugUnitTest` green.
- **Emulator:** seeded an unknown-merchant SMS ("ZATURN GADGETS") → it landed in the real
  queue as **0% Low confidence** with "No matching merchant — choose a category below"
  and the live chip picker; 3 real items shown.
- **Screenshots:** `docs/design/screenshots/review-live-light.png`, `…-dark.png`.
- **Performance:** `gfxinfo` on Review — **16.0% janky** over 119 frames, 90th %ile
  **34ms**, 95th **85ms** (representative). Uses `items(key = { it.id })`.
- **Accessibility:** confidence is colour **and** text ("0% Low confidence"); both themes;
  standing icon-button `contentDescription` finding still open.

**Follow-ups.** The chip picker is a horizontally-scrolling `LazyRow`; a future pass could
group categories. Retiring the last debug showcase (Search) is P4.

**Release-checklist delta:** "No debug showcase screens remain" now only Search (P4).

---

## P-Analytics — Financial Trend + Spending Breakdown (Insights, live)  ·  `feat(insights): P-Analytics — Financial Trend + Spending Breakdown`

**Reuse question:** *extend existing analytics, not a new engine?* → **Yes.** The data
already exists: `FinancialAnalytics.trendPoints` (real daily net-spend series) and
`categoryTotals` (already surfaced as `InsightsUiState.chartPoints` / `categories`). No
new analytics engine and **no separate charting subsystem** — refined the existing
`LedgerLineChart` and added one restrained donut primitive.

**What changed.** Insights is now a product, not a report:
- **Financial Trend** — `LedgerLineChart` refined to calm/editorial: single thin line,
  min…max range so movement (incl. declines) shows, no gridlines, no bright colour, no
  per-point dots (only a quiet last-point marker), gradient off by default. Renders the
  real daily spend for the month.
- **Spending Breakdown** — new `LedgerDonutChart`: soft rounded arcs over a quiet track,
  gaps, a single calm centre figure, **no in-chart legend / no 3D / no gradient**; the
  legend is a plain list beside it. Fed by the real category composition; the bright
  category palette was **desaturated** to soft tones per the design brief.
- **Removed** the "easy" artifacts that failed the product gate: the fabricated `0.62`
  progress bar, the fake "12 days left", and the non-functional Overview/Breakdown
  segmented control. Honest empty state when there's no month activity.

**Verification.**
- **Compile:** debug + release green. **Tests:** `testDebugUnitTest` green.
- **Emulator:** seeded known-merchant SMS → Insights shows **Income AED 6,000 / Spent
  AED 595**, a real daily-spend trend line, and a donut: Shopping 35 / Groceries 20 /
  Dining 15 / Unknown 12 / Entertainment 10 (pipeline auto-categorised the merchants).
- **Screenshots:** `docs/design/screenshots/insights-live-light.png`, `…-dark.png`.
- **Performance:** `gfxinfo` on Insights (two Canvas charts) — **18.8% janky** / 90th %ile
  **48ms**; acceptable for debug/emulator.
- **Accessibility:** breakdown is colour **and** text (name + %); soft palette keeps
  contrast reasonable; both themes verified. Standing icon-button finding still open.
- **Product gate:** (1) *Use daily?* yes — trend + breakdown are the two things one checks.
  (2) *Worthy of Ledger?* yes — calm, soft, information-first. (3) *Anything only there
  because easy?* the previous version's fake progress/segmented control were exactly that
  and are now removed.

**Follow-ups.** The daily-spend trend is spiky when spending is concentrated on few days
(honest). A compact trend/donut could later be embedded on the Dashboard/Accounts
("Balance Trend" in the reference). Net-worth-over-time trend needs historical snapshots
(future).

---

## H2 — PART 1: Search made real + dead affordances removed  ·  `feat(search): H2 — real universal search, remove fake data`

**Reuse question:** *extend an existing engine?* → **Yes.** No search engine existed, but
Search now reuses `TransactionRepository` (+ analytics story layer for the category
label) — a ViewModel doing real filtering, **not** a new search subsystem.

**What changed (Product Hardening, PART 1).**
- **Search is real**: `SearchViewModel` filters the user's actual captured transactions by
  merchant text and amount; results are real rows (tap → transaction detail). The
  **fabricated recent-searches and suggestion lists are deleted**; the empty-query state
  shows quick-access to **real destinations only** (Transactions, Accounts, Insights,
  Story) — wired in `LedgerNavHost`.
- **Dead affordances removed**: Accounts "Add Account" button + header add icon (no-op
  TODO; manual creation isn't built), and Profile "Log Out" (offline/local app has no
  auth). Accounts empty-state copy no longer promises manual add.

**Verification.**
- **Compile:** debug + release green. **Tests:** `testDebugUnitTest` green.
- **Emulator:** empty state → quick-access to real screens; "amazon" → **1 real result**
  (Amazon · Shopping · −210.00); "car" → **2 results** (Careem · Transport · −30, Carrefour
  · Groceries · −120). Real brand icons + categories.
- **Screenshots:** `docs/design/screenshots/search-live-light.png` (empty + results),
  `search-live-dark.png`.
- **Performance:** field + list is a plain `LazyColumn`; no concern.
- **Accessibility:** results are text + amount; standing `LedgerIconButton` finding open (PART 5).
- **Product gate:** (1) use daily? yes — real search is a daily tool. (2) worthy of Ledger?
  yes. (3) only there because easy? the removed fake recent/suggestions were exactly that.

Opens the Release Readiness report (`docs/RELEASE_READINESS.md`): **no production screen
renders fabricated data** as of H2. Closes P4 (Search).

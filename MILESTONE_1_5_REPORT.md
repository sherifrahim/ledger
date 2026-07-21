# Milestone 1.5 — Design Language Sprint Report

**Goal:** establish Ledger's visual identity before the major product features, using
`DESIGN_REFERENCE.md` as the visual north star — a *reference*, not a pixel spec.
Preserve the design language (editorial typography, calm, information-first, large
whitespace, soft surfaces, restrained accent, intent-first, explainability over
decoration); feel closer to Linear / Things 3 / Apple Wallet / Arc / Notion Calendar
than a banking app.

**Constraints honoured (ADR-0000 brownfield):** no Financial Event migration, no
Financial Story generation, no forecast or recommendation algorithms. Working business
logic was preserved; where a section has no backend yet it renders **realistic
placeholder data gated behind `BuildConfig.DEBUG`**, so a release build never fabricates
financial figures.

**Theme decision (product owner):** design *both* light and dark to the same premium
bar; the app remains system-adaptive. Every screen below was verified in both.

**Verification:** `compileDebugKotlin` ✓, `compileReleaseKotlin` ✓, `testDebugUnitTest`
✓ (full JVM suite incl. bank-SMS corpus regression). Screens captured on the running
Pixel 10 Pro emulator (`sdk_gphone16k`, 1280×2856) in light and dark.

---

## 1. Screens completed

| Screen | File | Notes |
|---|---|---|
| **Dashboard (flagship)** | `presentation/dashboard/DashboardScreen.kt` | Rebuilt to the canonical D4 order: Greeting → Safe to Spend → Financial Story → Upcoming → Insights → Review Queue → Accounts → Recent Activity. Real merchant tap → Merchant page. |
| **Review Queue** | `feature/review/presentation/ReviewInboxScreen.kt` | Redesigned around confidence cards: leads with the question ("Should this be Dining?"), confidence badge, evidence rows, Approve / Change. Real ViewModel wiring preserved. |
| **Search** | `feature/search/presentation/UniversalSearchScreen.kt` | Resting experience: field, Recent chips, Quick Access rows, "Try searching" suggestions. Query engine still deferred. |
| **Merchant page** | `feature/merchant/presentation/MerchantScreen.kt` (new) + route | Relationship header (rating, tenure), 2×2 stat grid, insights, top-category bars, related merchants. Reachable by tapping a merchant in Recent Activity. |
| **Settings / Profile** | (unchanged structurally) | Inherits the refreshed tokens/components automatically. |

Story remains the honest empty-state scaffold from M1 (Story generation is explicitly
out of scope). Screenshots: `docs/design/screenshots/`.

## 2. Components created

New LDL primitives (`core/designsystem/component/`):

- **`LedgerCard`** — the card philosophy primitive. Light: paper-white lifted off the
  page by a single soft shadow + whisper hairline. Dark: steps one level lighter than
  the page (shadows disabled — they read as mud on near-black). `LedgerCardDefaults`
  exposes `Elevation` / `ElevationLow`.
- **`LedgerConfidenceBadge`** + **`LedgerConfidencePill`** — tiered certainty chips bound
  to the canonical confidence ladder (spec D5): Deterministic / Very High / High /
  Needs Review / Low, coloured green→amber→red. Always pairs the number with a word.
- **`LedgerButtonStyle.Accent`** — brand-green primary CTA (e.g. Approve), completing the
  button hierarchy (Solid / Accent / Tonal / Ghost).

## 3. Design system changes (`core/designsystem`)

- **Colour (both themes):** refined palette — softer light border (`Smoke` → `#ECEDEE`),
  a premium near-black dark base with a faint cool cast and one-step-lighter cards. Added
  semantic helpers `surfaceCard`, `cardBorder`, `shadowColor`, `accent` as computed
  properties (no data-class churn).
- **Typography:** added `Hero` (52sp tabular) for the single largest figure; mapped the
  Material `titleLarge` / `titleMedium` / `labelMedium` slots that screens already
  referenced; button labels bumped to SemiBold.
- **Navigation:** bottom-bar glyphs aligned to the reference — house (Dashboard), open
  book (Story), inbox (Review), magnifier (Search), gear (Settings) — **outlined when
  idle, filled when selected**, with an animated colour transition.
- **Surfaces / radii / motion:** cards standardised on `LedgerRadius.Large` (24dp);
  progress and category bars use the `Full` radius; tab colour and category bars animate.

## 4. Before / after screenshots

All under `docs/design/screenshots/`:

- North star: `reference-northstar.png`
- **Before (Milestone 1 baseline):** `before.png` — flat grey cards, generic
  "Financial State 0.00" hero, honest-but-bare screens.
- **After (light):** `after-light.png` (Dashboard · Review · Search · Settings · Merchant)
- **After (dark):** `after-dark.png`
- Detail: `dashboard-light/-dark`, `dashboard-lower-dark`, `review-light`, `search-light`,
  `merchant-light`.

The transformation: from a functional-but-generic grey UI to calm, floating soft-shadow
cards with an editorial hierarchy, a single green accent, tabular figures, and
explainable review cards — recognisably the north star without copying it literally.

## 5. Remaining visual debt

1. **Dashboard showcase is DEBUG placeholder data.** Safe to Spend, Story text, Upcoming,
   Insights, Review count, Accounts and Recent are sample content until the engines feed
   them (M2+). The release path shows an honest "warming up" state instead.
2. **Merchant page uses a fixed title/data** ("Amazon.ae") — the tapped merchant's real
   name/id isn't threaded through the route yet (no nav argument). Wire in M2.
3. **Review Queue** renders showcase cards only when the real queue is empty (debug). The
   real `ReviewCard` was **not** restyled to the new confidence-card language yet.
4. **Accounts / Insights / Transactions** secondary screens inherit the new tokens but
   were not individually recomposed to the flagship standard this sprint.
5. **Config-change reset** — toggling system dark/light recreates the Activity and resets
   to the start destination (standard Compose behaviour; noted for QA, not a regression).
6. **Loading states**: the empty/loaded states are covered; a dedicated skeleton-shimmer
   loading component was scoped but not built.

## 6. Performance observations

- No jank observed on the emulator; scrolling the flagship `LazyColumn` is smooth.
- Card shadows are light-theme only and use `clip = false`; kept to two elevation levels
  to avoid overdraw. Dark theme draws **no** shadows.
- List items in the showcase paths lack stable `key`s in a couple of `items(count)` loops
  — negligible here (short static lists) but should get keys when wired to real, mutating
  data in M2.
- Build/tests unchanged in cost; no new heavy dependencies were added.

## 7. Accessibility observations

- **Good:** tab icons carry `contentDescription`; the mask toggle has show/hide
  descriptions; tabular figures keep numbers legible; text uses theme colours in both
  modes.
- **To improve:** confidence is currently colour + text (good) but the amber "Needs
  review/Medium" band on light should be contrast-checked; some tertiary text sits near
  the WCAG AA floor and wants a verify pass; touch targets on inline links (Read Story,
  See all) are text-sized and should be padded to 48dp; the Merchant star rating needs a
  combined `contentDescription` ("4.5 of 5").
- No dynamic-type / font-scale sweep was done this sprint.

## 8. Recommendations before Milestone 2

1. **Proceed to M2 (Financial Event Foundation, ADR-001).** The design language is now a
   stable target; wiring real data into these exact surfaces is the natural next step.
2. **Thread real data into the flagship** as events land: replace each DEBUG showcase
   block with engine output, and add a merchant-id nav argument.
3. **Restyle the production `ReviewCard`** to the new confidence-card component so real
   queue items match the design.
4. **Do one accessibility pass** (contrast tokens, 48dp targets, font-scale) before the
   design is considered locked.
5. **Keep both themes as first-class** — every new screen must ship verified in light and
   dark, as done here.

---

### Appendix — files touched

**Added:** `component/LedgerCard.kt`, `component/LedgerConfidenceBadge.kt`,
`feature/merchant/presentation/MerchantScreen.kt`,
`presentation/dashboard/DashboardShowcase.kt`, `docs/design/screenshots/*`, this report.
**Modified:** `theme/LedgerColors.kt`, `theme/LedgerTypography.kt`,
`component/LedgerButton.kt`, `navigation/LedgerBottomBar.kt`, `navigation/LedgerRoute.kt`,
`navigation/LedgerNavHost.kt`, `presentation/dashboard/DashboardScreen.kt`,
`feature/review/presentation/ReviewInboxScreen.kt`,
`feature/search/presentation/UniversalSearchScreen.kt`.

Nothing in the frozen specification set was modified.

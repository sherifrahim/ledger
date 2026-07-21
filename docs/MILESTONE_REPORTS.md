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

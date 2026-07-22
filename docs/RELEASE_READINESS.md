# Ledger — Release Readiness (living report)

Product Hardening phase. This is the categorized inventory the phase closes out; it is
updated as each hardening milestone lands and finalized at PART 8. Severity: **Critical**
(ship-blocking) · **High** · **Medium** · **Low**.

Standing rules in force: reuse-before-new-engine; treat every screen as a product;
ADR-0000 (brownfield) + ADR-0001 (Financial Event); never fabricate — honest empty state
when an engine is absent.

_Last updated: after the real-device responsive hotfix + Settings legacy removal._

---

## Fabricated data in production screens (PRIMARY objective)

| Screen | Was | Now | Sev |
|---|---|---|---|
| Dashboard | DEBUG showcase (Safe-to-Spend/Story/Upcoming/etc.) | Live: balance, MoM, spend, real insights, real recent activity; honest empty state | ✅ fixed |
| Accounts | fabricated "Payment due in 5 days"; ignored real net worth | Live net worth / assets / liabilities / per-account replay | ✅ fixed |
| Merchant | fabricated 5-star rating + sample stats | Real per-merchant aggregation from transactions | ✅ fixed |
| Review Queue | DEBUG showcase items | Real `ReviewInboxViewModel` items; honest "All clear" | ✅ fixed |
| Insights | fabricated 0.62 progress, "12 days left", dead segmented control | Real trend + donut from analytics | ✅ fixed |
| **Search** | fabricated recent searches + suggestions; dead quick-access | **Real search over `TransactionRepository`; quick-access → real destinations** | ✅ fixed (H2) |

**No production screen renders fabricated data as of H2.** Remaining fabrication: none
identified. (Continued vigilance in PART 4 screen-by-screen polish.)

## Responsive UI / dynamic type (real-device hotfix)

- ✅ **Large figures no longer wrap** on any width/font-scale — `LedgerAutoSizeText`
  (single-line, shrink-to-fit) on Dashboard/Accounts Total Balance + Merchant stat cards;
  `maxLines=1`+ellipsis on account/merchant/row names; **bottom-nav** equal-width tabs +
  single-line labels (fixes "Settings" stacking). Verified at font-scale 1.3 + 1080px.
- ✅ **Accounts net-worth sign bug fixed** (showed positive while Dashboard showed negative).
- ✅ **Sluggish launch** — read-parity harness gated to DEBUG (was running the analytics
  engines twice on every production launch).

## High

- **Dead affordances / TODO-navigation** still present (no-op `onClick`):
  `TransactionDetailsScreen` (2), `SearchFilterScreen` (1), Dashboard/greeting **notification
  bell**, Merchant/greeting bells. _`SettingsScreen`'s fabricated rows (Dark-mode toggle,
  Currency/Language/Default-account/Expense-reminders/Weekly-insights/Export/Delete) with TODO
  clicks were removed; Accounts "Add Account" & Profile "Log Out" removed earlier._ → H3.
- **Settings / Profile consolidation** (new): `ProfileScreen` still carries static preference
  rows (Currency/Theme/Language/Notifications/Data & Privacy) duplicating the (now-cleaned)
  gear → `SettingsScreen`. Decide the one home; `SettingsScreen` is now just a theme control.
  A full System/Light/Dark theme control needs a one-line `LedgerThemeType` Light option. → H3.
- **Accessibility** (PART 5): `LedgerIconButton` passes `contentDescription = null`
  app-wide; touch targets (44dp vs 48dp); tertiary-on-inset contrast; screen reader ordering.
  _Dynamic-type overflow now handled for figures/nav (above)._
- ~~**FinancialEvent backfill** (P5)~~ ✅ done — idempotent, resumable, safe, observable
  (`BackfillFinancialEventsUseCase`, auto-run at startup, on-device `verified=true`).

## Medium

- **Analytics on Dashboard** (PART 3): trend + breakdown live on Insights; a compact
  version is not yet embedded on the Dashboard ("Balance Trend" in the reference).
- ~~**Read-parity harness** (P6)~~ ✅ done — `proven=true`, 6/6 features PASS
  (`docs/READ_PARITY_REPORT.md`); transferDirection/origin/cardTail balance gaps documented.
- ~~**Event-first read migration** (P7)~~ ✅ done — product list reads originate from
  FinancialEvent via `TransactionReadSource`; balance/detail documented legacy;
  soft-delete voids the mirror; parity re-checked `proven=true`. `docs/EVENT_FIRST_READS.md`.
  **Phase A (finish the architecture) is complete.** Remaining balance-event-first work
  (extend event schema with `transferDirection`) is optional/Medium, not a blocker.
- **Legacy / V2 compatibility surface**: `LedgerColors`/`LedgerSpacing`/`LedgerTextStyles`
  carry "V2 Compatibility" aliases; `core.common.diagnostics.PipelineTracker` family is
  confirmed dead but retained so `app/src/debug` compiles. → PART 2 (prove-then-delete).
- **TODO backlog**: ~47 `TODO`s, concentrated in `core/database/repository` &
  `mapper` (mostly explanatory comments) — triage in PART 6.

## Low

- **Unused imports** after H2 removals (`AccountsScreen`: `Add`, `LedgerButton*`) — PART 6.
- Debug-only Compose lists missing `key = {}`; unused preview code — PART 6.
- Test byproduct `app/src/test/resources/financial-corpus/REPORT.md` regenerates — gitignore/triage.

## Architecture (PART 7 — to verify)

ADR-0000/0001 honored so far: no Balance Engine change; FinancialEvent additive +
best-effort dual-write; presentation-only screen changes; repository boundaries intact.
Formal audit pending.

## Definition-of-Done gates (per milestone)

compile (debug+release) · tests · emulator · screenshots (light+dark) · perf sanity ·
a11y sanity · docs · milestone report · commit · push · local sync. Tracked in
`MILESTONE_REPORTS.md`.

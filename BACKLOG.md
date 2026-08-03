# Ledger Backlog — deferred work, paused plans, roadmap

Everything not on the active build (`MOCK_PARITY_PLAN.md`). Living doc — when an
item is started, move it into the active plan; when done, delete it here.

---

## 1. Release-to-public gates (owner: user / legal — not code)
- **Play Store SMS-permission use declaration.** `READ_SMS`/`RECEIVE_SMS` need a
  justified declaration or Play rejects the listing. **Biggest external gate.**
- **Privacy policy finalization.** In-app screen exists (Settings → About) with an
  accurate draft; still needs (a) a real contact address (currently a marked
  placeholder) and (b) a **publicly-hosted URL** for the Play listing. Ideally a
  legal review (SMS + financial data).

## 2. Paused plans
- **iOS-fy pass** (continuous "squircle" corners + iOS-style controls: switches,
  segmented control, buttons; iOS sheets/dialogs; large-title screens). Deliberately
  the **finishing layer AFTER** mock parity — it's the last item in `MOCK_PARITY_PLAN.md`.
- **CSV import** — on hold per user. (An **export** may still be wanted.)

## 3. Deferred features (roadmap, not started)
- **Receipt capture + OCR** → auto-fill transaction → user reviews/approves (never
  auto-saves). Explicitly deferred to its own milestone. Enables the notification
  "attach receipt" action and receipts on Transaction Detail.
- **Budgets** — also listed in the mock plan (Settings → Budgets); a genuine new feature.
- **Financial Health score.**
- **Collaboration / shared splits** (shared trips/house). Split is local/single-device
  today; participant IDs are already UUIDs *for a future sync*, so the schema is ready.
- **Data & Sync / cloud backup + restore** (no server today; offline-first by design).
- **Privacy hardening:** biometric app lock, vault mode, stealth notifications.
- **Smart notifications:** salary received, budget exceeded, bill due (only capture
  confirmations exist today).
- **Split UI gaps:** custom / percentage / exact split (backend already supports all
  three via `SplitType`/`ShareInput`; UI only does EQUAL).
- **Notes:** rich notes + edit history (plain text note already works).
- **Transaction Timeline** (imported → learned → recategorized → receipt → split →
  comment). Substrate exists (`FinancialEvent` ACTIVE/SUPERSEDED/VOID + fingerprint);
  no per-transaction event history is recorded or shown yet.
- **Merchant intelligence depth:** subscription detection, visit frequency, related
  merchants, comparison (basic merchant page exists).
- **Search depth:** universal cross-entity search + natural-language.
- **Accounts/Goals:** net-worth history, asset allocation, debt payoff, savings/emergency goals.
- **Customizable dashboard widgets.**

## 4. Surface work — engines that exist but aren't user-facing
These are built and tested; they only need a screen/wiring (do NOT rebuild — see the
`reuse_before_new_engine` memory):
- **`ForecastEngine`** — reachable only from the debug Intelligence Inspector. (Mock has
  a full Forecast screen — in `MOCK_PARITY_PLAN.md`.)
- **`RecurringScheduleAnalyzer`** — powers Upcoming / subscriptions; unsurfaced.
- **`CategoryIntelligenceEngine`** — deterministic + AI-fallback categorization; only in
  the debug inspector. (An opt-in AI sweep is wired at startup, but live-capture AI in
  `ProcessNotificationUseCase` is still untouched — a later phase.)

## 5. Correctness / trust watch-items
- **Upcoming source:** bank **credit-card-bill / statement SMS are deliberately ignored**
  (`AdcbStatementPattern`/`AdcbBillingPattern` → `Ignore`) — correct, a bill notice isn't a
  transaction. To power Upcoming, capture them as a **separate "upcoming bill" record**
  (due date + amount), not a transaction. Streaming/Prime no longer SMS.
- **Safe-to-Spend** has no defined algorithm (flagged in `DOCUMENTATION_REVIEW.md §3`).
  Propose an ADR before building the dashboard hero.
- **Raw-SMS row title:** the feed uses `Transaction.rawText` as the row title, so real
  captures show raw SMS text (`Transaction` has no merchant-name field — resolve from
  `brandId`). Folded into the polish pass in `MOCK_PARITY_PLAN.md`.
- **Transfer capture (resolved-in-code):** the real ADCB transfer SMS parses correctly
  (`SmsIngestionTest` regression); `Transaction.isOutflow` now signs outgoing transfers
  −red. On-device miss was likely environmental (live-capture timing) — user to
  re-import an SMS range covering the date to confirm.
- **Light-mode Liquid Glass** was visually verified only in dark this session (code is
  symmetric); eyeball light before relying on it.
- **SMS-tally gap (open since RC6):** whether the HDFC currency-mixing fix fully explains
  the user's remembered SMS-tally gap is unconfirmed; `BalanceTraceReport.transactionContributions`
  (RC7) provides the itemized comparison to close it out.

## 6. Small / housekeeping
- `app/release/*.apk` and baseline-profile `.dm` files were committed by accident at
  some point — should be `.gitignore`'d.
- Merchant/category "System A vs System B" split (`feature/merchant` vs
  `core/domain/service/transaction`) is investigated and deliberately NOT merged (a merge
  would change behavior). One safe, narrow follow-up identified in `MERCHANT_ARCHITECTURE.md`
  (normalize System B's exact-text Brand lookup) — not done.
- Structured parser-failure tracking (a real `FinancialEvent`-style record of *why* a
  message didn't capture) — seams exist (`IngestionSource`, `SourceAdapter`); not built.

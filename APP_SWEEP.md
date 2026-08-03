# App Sweep — what works, what doesn't, what's missing

From real-device use and direct database inspection (2026-08-03), plus code
verification. Complements `DEVICE_FINDINGS.md` (capture bugs) and
`ACCOUNT_IDENTITY_PLAN.md` (account splitting).

## Works
- Capture pipeline end-to-end: notification listener → parse → dedup → persist.
- Balance replay per account (Primary Account computed **exactly** the user's real
  1,568.52 — the engine is sound).
- Dashboard, Story, Review, Search, Settings, Accounts, Transactions, Insights,
  Merchant, Split, Manual entry, Adjust Balance all render real data.
- Liquid Glass (real Haze backdrop blur), Light/Dark, splash, icon, haptics.
- Release build: R8 + signed APK, 5.0 MB, smoke-tested.

## Broken / wrong (ranked)
1. **Account identity split** — see `ACCOUNT_IDENTITY_PLAN.md`. Directly wrong balance.
2. **Duplicate capture** (bank app + Messages + Truecaller) — inflates spending.
3. **Junk accounts** from SMS sender IDs; one has a mojibake character in its name.
4. **`raw_text` overwritten with the merchant name** — the original message is lost,
   so nothing can be diagnosed from the database afterwards (the phantom-amount bug
   had to be diagnosed from `dumpsys notification` instead). Keep the raw message;
   add a separate merchant column.
5. **Bogus merchant names** — "Mid Kr", "Ak Jol", "Your", "Day", "The Next Day",
   "Standard Rate", "4 6 Riverwalk Citywest Bu Has Been Rejected". Merchant
   extraction grabs arbitrary fragments; the feed shows them as titles.
6. **Credit-card onboarding question** — asks the user to type a balance for each
   credit card, when the bank states it in every message ("Available limit: AED
   8,093.63"). The clause is already parsed (`BALANCE_CLAUSE_PATTERN`); use the most
   recent one to derive it and drop the question.
7. **`opening_balance_as_of` never set** — `SeedOpeningBalanceUseCase` reads
   `observeAllTransactions().first()`; if that first emission is empty the anchor is
   silently lost. Every account has NULL.
8. **Tabby posts two notifications per purchase** — the instalment/marketing line
   ("Pay As Low As 12") is captured as a second transaction.
9. **Negative totals don't always render a minus sign** in the hero.
10. **Not on the battery-optimisation whitelist** — the listener can be killed in
    the background; the onboarding step exists but the user is not actually exempt.

## Missing features worth having
**Trust / correctness (highest value — these are what makes it believable)**
- **Merge accounts** + **reassign transaction to another account** (backend for
  reassign already exists, no UI).
- **Delete / exclude a transaction** the user knows is wrong, with an audit trail.
- **"Why is my balance this?"** — a reconciliation view listing every contribution.
  `BalanceTraceReport.transactionContributions` already exists; it needs a screen.
- **Auto-derive balances** from the bank's own stated balance in each message,
  instead of asking. Also enables **balance drift detection**: if the bank says
  1,493.52 and Ledger computes 1,510.00, something was missed — surface it.

**Everyday use**
- Edit a captured transaction (amount, merchant, category, date).
- Bulk categorise / rules ("always categorise X as Y").
- Recurring & subscriptions surfaced (engine exists, debug-only).
- Forecast screen (engine exists, debug-only).
- Budgets.
- Export (CSV/JSON) + encrypted backup/restore.
- Search filters (date range, amount range, account, category).

**Polish**
- Interactive charts with labeled axes (already planned in `MOCK_PARITY_PLAN.md`).
- Real brand logos + proper merchant names.
- Per-account screens with their own trend.
- Biometric lock / privacy mode.

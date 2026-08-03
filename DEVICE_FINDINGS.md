# Real-Device Findings — 2026-08-03

Evidence gathered from the owner's physical phone (OnePlus CPH2651) with real
captured financial data: the app database was pulled and analysed directly, and
real bank notification text was recovered from `dumpsys notification`. Every item
below is grounded in that data, not inferred.

Snapshot analysed: 184 transactions, 7 accounts, Total Balance showing
**−AED 30,226.07** against an opening balance of AED 12,056.40.

---

## FIXED this session

### 1. Balance/limit captured as the transaction amount (CRITICAL) — fixed
Four "purchases" of ~AED 8,2xx were recorded, including **a KFC charge of
AED 8,225.16**. They were the card's *Available limit*. ~AED 33,000 of phantom
spending — about 75% of all recorded expenses, and the main driver of the
negative balance.

Root cause: the anchored-amount pattern only knew `AED|USD|INR|Rs|DIRHAM`. For a
purchase abroad in another currency it found no amount and fell through to the
first AED figure in the message — the running balance. Proven by the recovered
notification format and by the recorded limits decreasing by exactly the real
purchase amounts (8,225.16 → 8,209.87 → 8,204.63).

Fix: strip balance/limit clauses and card references before amount search;
derive currency from the token that anchors the amount; represent USD/EUR/GBP/
SAR/KZT; fail closed on an unrepresentable currency. Tests in
`BalanceClauseAmountTest` use the real captured text.

### 2. Declined transactions recorded as spending (CRITICAL) — fixed
A repeatedly-declined AED 27.30 payment ("... has been rejected") was booked
**eight times** as genuine spending. No decline guard existed anywhere.
Fix: `ExtractionHelpers.describesNonExecutedTransaction`, applied in
`GenericBankParser` and `HeuristicExtractor`.

### 3. Outgoing transfers displayed as income — fixed (earlier this session)
Screens computed the sign as `type == EXPENSE`, so an OUTGOING transfer showed as
a green **+**. Fixed via `Transaction.isOutflow`.

---

## FIXED 2026-08-03 (second pass) — verified by a fresh import on the device

Re-imported the owner's inbox on a clean install (1,156 messages scanned,
"Last 3 Months"). **378 transactions and 15 accounts, against 383 and 18
before.** Entering the real ADCB balance of AED 1,568.52 (and 0 for the
fallback "Primary Account", which holds only misattributed FAB/Mashreq
messages) produced **Total Balance = AED 1,568.52** on the dashboard.

### A. The same transaction captured 2–3 times — fixed
Two independent causes, both dead code rather than mistuning:
1. The fingerprint included `accountId`, but dedup runs BEFORE account
   attribution — candidates were fingerprinted with 0, every persisted row
   with a real id, so `ReconciliationEngine`'s exact-match branch could never
   fire at all, and the unique index could not catch one event landing on two
   accounts. `accountId` is now out of the fingerprint.
2. Scoring could not reach its own threshold for the commonest shape. The AED
   3,000.00 pair (ids 238/239, 85s apart, one tailed with a merchant, one
   with neither) scored 40+20+10 = 70 against a 90 threshold. The engine now
   matches structurally first — same amount/currency, within 5 minutes,
   compatible tails, same direction of travel — independent of wording.

A same-event scan over the new database finds **0 duplicate rows remaining
(was 1 cross-account)**. Item D below fell out of the same fix: Tabby's
"Pay As Low As 12" instalment line no longer becomes a second AED 160.00.

### B. Junk "Unrecognized Institution" accounts — fixed
`SenderClassifier` refuses an account to a transport app (Google Messages,
Truecaller, Samsung Messages) and to known non-financial senders (Smiles,
eand*, du). Both route to one shared per-currency "Unattributed Capture"
candidate account — excluded from balances exactly as other candidates are,
and deliberately not the fallback account, which carries the user's opening
balance. Ten junk accounts became two.

Genuinely financial senders Ledger doesn't recognise yet (MBANKAlert, Tabby,
Paytm, SBI UPI, e& money) deliberately keep their own reviewable, promotable
candidate account — the point is to refuse messengers and telecoms, not to
collapse every unmet bank into one bucket.

**The mojibake was a false alarm.** The stored name is
`Unrecognized Institution (MBANKAlert) •000001` with a correct UTF-8 U+2022
bullet (`E2 80 A2`, confirmed at the byte level in both the database and the
source). The `�` was the previous session's terminal failing to decode
UTF-8, not corrupt data.

### C. `raw_text` overwritten with the merchant name — fixed
`MIGRATION_12_13` adds a nullable `merchant_text` column; `raw_text` now
holds the captured message verbatim. All 378 rows in the fresh import carry
both. `Transaction.merchantOrRawText` prefers the new column and falls back
to the old meaning, so pre-migration rows and the frozen relationship/
recurring scoring paths read exactly what they read before.

---

## NOT yet fixed — ranked by trust impact

### A2. Account identity is still split (HIGH — now the top remaining issue)
"Primary Account" (the fallback) holds 4 tail-less FAB/Mashreq messages
totalling −3,381.47, while the real ADCB money sits on "ADCB Account". No
single per-account balance entry makes the dashboard total right on its own;
the 1,568.52 above was reached by telling Ledger the fallback account's real
balance is 0. See `ACCOUNT_IDENTITY_PLAN.md` — unchanged and now the clear
next increment.

### A. The same transaction is captured 2–3 times (HIGH) — FIXED, see above
One purchase arrives as a bank-app notification, as an SMS notification from
Google Messages, **and** as a Truecaller SMS preview. Each becomes its own
transaction, often under a *different account*, so reconciliation never matches
them. Real examples:
- AED 1.50 "Pick Up Grocery" — Mashreq app (acct 2) + Google Messages (acct 1)
- AED 55.00 — ADCB app (acct 1) + Truecaller (acct 6)
- AED 46.80 — Mashreq app (acct 2) + Google Messages (acct 5)
- AED 296.10 Adidas — Tabby app twice (acct 3) + Google Messages (acct 4)

Fix direction: dedup on (amount, ~timestamp, card tail) **across** accounts and
packages, before account attribution — reconciliation currently runs too late /
too narrowly. Consider ignoring known SMS-mirror packages (Truecaller, Messages)
when the bank's own app already reports, or treating them as confirmations.

### B. Junk "Unrecognized Institution" accounts (HIGH)
Accounts were auto-created for `com.google.android.apps.messaging`,
`com.truecaller`, `app.tabby.client`. One name contains a mojibake character:
`Unrecognized Institution (com.google.android.apps.messaging) �1959`.
Also a duplicate of the *same* card: "Mashreq Credit Card ···1959" (acct 2) and
"Unrecognized Institution (messaging) ···1959" (acct 5).
Fix direction: never create an account from a generic messenger package; resolve
by card tail first; sanitize names.

### C. `raw_text` is overwritten with the merchant name (MEDIUM)
The DB stores `raw_text` = "Kfc" / "Transfer" / "Income", not the original
message. This destroys diagnosability (the 8k bug could not be diagnosed from the
DB alone — the real text had to be recovered from `dumpsys`) and it is why the
feed shows odd titles. Keep the original text; add a separate merchant field.

### D. Tabby posts two notifications per purchase (MEDIUM)
"Ounass Uae" and "Pay As Low As 12" both became AED 160.00 transactions.
The marketing/instalment line must not be captured.

### E. Bogus merchant names (MEDIUM)
"Mid Kr", "Ak Jol", "4 6 Riverwalk Citywest Bu Has Been Rejected", "Your",
"Income", "Day", "The Next Day", "Standard Rate". Merchant extraction takes
arbitrary text fragments. Part of the mock-parity polish pass.

---

## Environment findings (not code bugs)
- **`READ_SMS` and `RECEIVE_SMS` were DENIED** on the device, so live SMS capture
  and SMS import could not run — this is the most likely reason the AED 2,770
  ADCB transfer was never captured (the parser handles that exact message
  correctly; there is a passing regression test for it). ColorOS blocks
  `adb pm grant`, so permissions must be granted through the app's own UI.
- Ledger is **not** on the battery-optimisation whitelist, which can kill the
  notification listener in the background.
- The notification listener itself was enabled and working.

## Verification note
`opening_balance_as_of` is NULL on every account even though the dated-anchor
feature exists. `SeedOpeningBalanceUseCase` sets it only from the earliest
transaction, via `observeAllTransactions().first()` — if that first emission is
empty, the anchor is silently lost. Worth confirming. The opening-balance
*semantics* are correct (it back-calculates so computed == the real balance the
user enters); the negative total was driven by items 1 and 2 above, which landed
after the anchor was set.

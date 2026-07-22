# RC1 — Trust & Verification Audit

_Companion to the RC1 Product Review. Produced 2026-07-22 at head `6af2864`
(after the RC1 fix batch). Answers the six additional audits requested plus the
Trust Checklist and a fresh-maintainer maintainability review. Grounded in the
current code and on-device behaviour, not prior reports._

---

## Audit 1 — Notification / SMS ingestion pipeline

Ledger starts at ingestion, so this is audited first. The end-to-end flow lives
in `ProcessNotificationUseCase.execute()` and is the **single** path any captured
message (SMS or notification) takes to becoming a persisted transaction:

```
Incoming envelope (SMS or notification)
        │
        ▼  NotificationFilter.evaluate       ← 1. gate: "looks financial at all?"  (reject → stop)
        ▼  ExtractionRegistry.extract        ← 2. produces DATA only, never routes
        ▼  FinancialIntentClassifier.classify← 3. SOLE routing authority
        ▼  Router (intent only)              ← 4. the only behavioural decision
        │     ├ FINANCIAL_CONFIRMATION → match to existing; NEVER persist
        │     ├ FINANCIAL_INFORMATION  → ignore; no insert
        │     ├ UNKNOWN                → ignore; no insert
        │     └ FINANCIAL_EVENT + extracted candidate present → continue
        ▼  ReconciliationEngine.reconcile    ← 5. New / Updated / Duplicate / Ignored
        ▼  AccountIdentityResolver.resolve   ← deterministic account, no silent default
        ▼  InsertTransactionUseCase          ← 6. persist (+ dual-write FinancialEvent, ADR-0001)
        ▼  Dashboard / Story / Search / Merchant / Review  (all read the same store)
```

**Trust guarantees verified in code (`ProcessNotificationUseCase`):**

| Risk | Guard | Location |
|---|---|---|
| Phantom transaction (money shown that the user never spent) | `FINANCIAL_EVENT` intent with **no extracted candidate** → no insert; "Never fabricate a candidate" | lines 185-197 |
| Double-count from a confirmation/"successful" message | Confirmations **never persist**; an unmatched confirmation explicitly does not invent an expense | lines 146-174 |
| Duplicate transaction | `ReconciliationResult.Duplicate` → not persisted, logged as merged | lines 280-285 |
| Wrong account | `AccountIdentityResolver` resolves from deterministic signals with an evidence trail — never a bare candidate field, never a silent default | lines 228-232 |
| A notification-posting failure corrupting a good persist | `notifyCaptured` wrapped in try/catch after the write already succeeded | lines 258-266 |
| Non-financial noise | `NotificationFilter` gate before any extraction | lines 107-114 |

**Conclusion:** the architecture is sound for "never a wrong transaction." The
one open extraction defect is L7 (below).

**L7 — known extraction edge case (Low, balance-neutral).** A specific HDFC
"credited" message yields `transferDirection = OUTGOING`. The currency guard
zeroes any balance effect, so it cannot corrupt a balance, but the stored
direction is semantically wrong. The fix belongs in the HDFC extractor's
direction assignment (tuned parser logic — `ExtractionRegistry`/parsers, flagged
frozen in CLAUDE.md), and needs the exact failing message as a regression fixture
before changing scoring-adjacent code. **Deliberately not blind-patched.** Tracked
for a parser-focused change with a corpus entry.

---

## Audit 2 — First-run experience

Gating lives in `MainActivity.onCreate` and is ordered, not assumed:

```
isProfileSetup == false        → ProfileSetupScreen (name/email, local only)
else isPermissionGranted==false→ NotificationAccessScreen
else isSmsImported == false    → SmsOnboardingScreen (historical import + range)
else                           → Dashboard
```

- **Empty database:** every list screen renders an **honest empty state**
  (Dashboard, Accounts, Insights, Review "All clear", and now Story). No sample
  data. Verified by reading each screen's `if (empty)` branch.
- **Permission denied / revoked:** `LifecycleEventEffect(ON_RESUME)` re-reads
  `PermissionUtils.isNotificationServiceEnabled` on every resume, so revoking
  access in system settings drops the app back to `NotificationAccessScreen` on
  return — state is not cached stale.
- **Gaps (recommended, not blockers):**
  - **Battery optimization / background-kill:** the app does not prompt to exempt
    itself from Doze/OEM battery optimization. On aggressive OEMs the
    `NotificationListenerService` can be killed, silently missing captures. This
    is the single biggest real-world ingestion-reliability risk and deserves a
    first-run "keep Ledger running" step + a periodic self-check.
  - **`POST_NOTIFICATIONS` (Android 13+)** is declared but never requested at
    runtime, so capture-confirmation notifications and their actions are
    effectively unreachable without a manual grant.

---

## Audit 3 — Persistence

- **Storage:** Room (`user_preferences` DataStore for settings; Room DB v11 for
  transactions/accounts/events). Both are on-disk and process-independent.
- **Verified this session:** the app was force-stopped and cold-started multiple
  times (density tests, RC review, fix verification); captured transactions,
  accounts, balances and the theme preference persisted across every restart.

```
Imported transaction → force-stop → cold start → still present   ✅ (observed repeatedly)
```

- **Reboot:** not explicitly exercised this session, but persistence is Room
  on-disk with no in-memory-only cache in the read path (balances are replayed
  from persisted rows, never held only in memory), so a reboot is equivalent to a
  cold start. Recommended as a one-line manual check before RC sign-off.

---

## Audit 4 — Large data

- **Read path is bounded, not full-table:** list screens observe **limited**
  queries — `observeRecentTransactions(20)` (Dashboard), `(50)` (Story), `(100)`
  (Transactions). The UI never renders an unbounded list, so row count does not
  linearly degrade scrolling.
- **Lists use `key = { it.id }`** in the main screens (Story, Transactions),
  enabling stable recomposition.
- **Balance/analytics** replay the full transaction set (`computeNetWorth`,
  `compute`) — these are O(n) and the honest scaling risk at 10k+. They run off
  the main thread in the ViewModel flows.
- **Not yet proven empirically.** Recommend a synthetic soak test (10 / 100 /
  1 000 / 10 000 rows) via the debug console's batch injection, watching (a)
  Story/Transactions scroll and (b) Dashboard net-worth recompute latency. The
  debug console already supports `InjectMultiple(100)`; a `1000`/`10000` button is
  a one-line addition for the test.

---

## Audit 5 — Attachments / future-compatibility of Transaction Detail

`TransactionDetailsUiState` is a flat, additive data class and the screen renders
a `LazyColumn` of independent sections. Adding note (done), receipt, timeline,
split, confidence, source, or audit rows is **additive** — each is a new `item {}`
and a new nullable state field, with no restructuring of what exists. The
underlying `Transaction`/`FinancialEvent` model already carries `note`, `source`,
`fingerprint`, `cardTail`, `transferDirection`, `origin`, and the Split backend
exists. **Conclusion: the screen can evolve without a redesign.**

---

## Audit 6 — Transaction Detail as Ledger's richest screen

Agreed direction. Current: merchant, amount, category, date, time, payment
method, card, reference, **note (now wired)**, merchant history. A sequenced path
to the "everything about this transaction" screen, each item already backed:

| Addition | Backing that already exists | Effort |
|---|---|---|
| Note | `Transaction.note` + `updateNote` (shipped this batch) | ✅ done |
| Source | `Transaction.source` (SMS / NOTIFICATION / MANUAL) | S |
| Confidence | capture confidence + Review confidence model | S |
| Timeline (capture → confirm → reconcile) | `PipelineTraceSink` per-transaction trace | M |
| Receipt (attachment) | new (needs a file store + a `receiptUri` column) | M |
| Split | `SplitRepository` backend exists; needs UI + route | M |
| Audit (event history) | `FinancialEvent` supersession chain (ACTIVE/SUPERSEDED/VOID) | M |

---

## Trust Checklist

The six questions that summarise almost everything for a finance app, answered
against the current build:

| Question | Answer | Basis |
|---|---|---|
| Can Ledger ever show **invented** information? | **NO** (as of this batch) | Last hard-coded fabrication — the "Abu Dhabi" location — is deleted; dead controls removed; every screen reads real repositories or shows an honest empty state. Dev-console injection is debug-only and cannot exist in release. |
| Can Ledger ever **lose** a transaction? | **NO, by design** | Immutable records; balances replayed from persisted rows; Room on-disk; FinancialEvent dual-write mirrors every insert. No delete in the capture path. |
| Can Ledger ever **duplicate** a transaction? | **NO, by design** | `ReconciliationEngine` Duplicate path + fingerprint idempotency; confirmations never persist. |
| Can Ledger **calculate an incorrect balance**? | **NO for known inputs** | Balance is a pure replay (`BalanceCalculator`/`AccountBalanceService`); event/legacy read parity is `proven=true` 6/6 on this build; currency guard prevents cross-currency corruption. Residual: L7 mis-labels one message's *direction* but the currency guard keeps it balance-neutral. |
| Can Ledger **silently ignore an import**? | **PARTIALLY** | Every message's outcome is traced (`PipelineTraceSink`) and visible in the debug console, and the SMS importer tallies created/merged/discarded. But there is **no user-facing** "we saw N messages, captured X, skipped Y" surface, and background-kill (Audit 2) can drop captures without the user knowing. This is the honest weak point. |
| Can Ledger **mislead** the user? | **NO for shown data; watch empty-vs-missing** | Shown figures are real. The remaining subtlety is *absence*: if a capture was silently missed (battery-kill), the totals are quietly incomplete — addressed by the Audit 2 battery step + a capture-coverage surface. |

**Net:** five of six are solid guarantees; the sixth ("silently ignore an
import") is the one to close before Ledger can promise complete capture — it is a
reliability/visibility gap, not a correctness bug.

---

## Fresh-maintainer maintainability audit

_Read as if inheriting the codebase cold. Where does it still feel transitional,
temporary, or overly complex? (Identification only — no code changed here.)_

1. **Two parallel merchant/category systems** — `feature/merchant` (System A) vs
   `core/domain/service/transaction` (System B, wired into the live write path).
   Fully documented (`MERCHANT_ARCHITECTURE.md`) and deliberately un-merged
   because a merge changes behaviour, but a new maintainer *will* be confused by
   two `MerchantResolver`/`MerchantRegistry` classes with the same names.
   **Highest-value consolidation** (ADR-0009). Until then, the naming collision is
   a foot-gun.
2. **Dead-but-retained diagnostics** — `core.common.diagnostics.PipelineEvent`
   survives only as the debug console's display model after the tracker itself was
   removed (H4). There are now **three** `PipelineEvent`-shaped types
   (`core.common.diagnostics`, `feature.diagnostics`, `designsystem.component.debug`).
   Transitional; collapsing to one debug display model would remove real confusion.
3. **"TEMPORARY" diagnostic scaffolding in a production ViewModel** —
   `DashboardViewModel` carries a `diagnosticHasRun` flag driving
   `FinancialTraceCollector.buildReport()` on first emission, commented as
   temporary. It changes nothing displayed, but temporary code in the flagship
   screen's ViewModel should be time-boxed or moved behind the debug build.
4. **`rawText` doubles as the display merchant name** — Dashboard, Story and
   Transactions all show `txn.rawText` as the merchant. It works today because the
   extractor populates it with a clean-ish name, but the field name says "raw" and
   the detail screen resolves a *real* brand name separately. One canonical
   "display merchant" resolution would remove the inconsistency (and the risk of a
   future raw SMS string leaking into a list).
5. **Lossy FinancialEvent mirror** — the event model omits `transferDirection`,
   `origin`, `cardTail`, `note`; balance and transaction-detail therefore still
   read legacy `Transaction`. This is documented and intentional, but it means
   "reads come from FinancialEvent" has real, load-bearing exceptions a maintainer
   must know before touching either path.
6. **Unreachable-but-present features** — Ledger Split (backend, no route) and the
   notification actions (permission never requested). Fine to keep if the roadmap
   commits to them; otherwise they read as half-built to a new maintainer.
7. **Frozen tuned logic** — scoring/matching in `ReconciliationEngine`,
   `RelationshipResolvers`, the parsers, etc. is deliberately frozen and only
   changeable with a demonstrated failing case + corpus fixture. This is a
   strength (protects tuned behaviour) but must be learned early; it is recorded
   in CLAUDE.md, which a new maintainer should read first.

**Overall:** the core (Financial Truth, ingestion, event model) is coherent and
well-documented. The maintainability drag is concentrated in the
diagnostics/merchant duplication and a few clearly-labelled transitional seams —
none blocking, all individually addressable, and each already named in the
canonical docs.

# Architecture Verification (pre–Release-Hardening audit)

**Purpose:** confirm the repository now has exactly **one architectural direction** before
Release Hardening. This is an audit — it documents findings and classifies them; it does
not change product behaviour. Removals happen in H4 (dead-code) after this proves them
unused. Contains three deliverables: **§A Source-of-Truth Inventory**, **§B Repository &
duplicate audit**, **§C Legacy Inventory**, plus the violation check and conclusion.

Sources: `EVENT_FIRST_READS.md`, `READ_PARITY_REPORT.md`, `MERCHANT_ARCHITECTURE.md`,
`ENGINEERING_STATUS.md`, ADR-0000/0001, and direct code inspection (2026-07-22).

---

## §A — Source-of-Truth Inventory (every production read)

Classification: **Event** = FinancialEvent · **Txn(intentional)** = Transaction on purpose ·
**Legacy(temporary)** = Transaction until the event model carries the missing field.

| Production read | Source | Class | Note |
|---|---|---|---|
| Dashboard recent activity + month analytics | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| Insights (trend + breakdown) | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| Accounts activity + month insight | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| Merchant relationship aggregation | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| Review Queue (uncategorised) | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| Search | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| Transactions list | `TransactionReadSource` → FinancialEvent | **Event** | P7 |
| **Balance / Net worth** (`AccountBalanceService` / `computeNetWorth`) | Transaction | **Txn(intentional)** | Needs `transferDirection` + `origin`/`cardTail`; frozen Balance Engine (ADR-000). Parity proven (P6). |
| **Transaction detail** record view | Transaction | **Txn(intentional)** | A record view — shows `transferDirection`/`note`/`cardTail`/`origin`. |
| Month-over-month change (analytics-internal) | Transaction | **Txn(intentional)** | Helper for balance-change %; not a list read. |
| Diagnostics, `ForecastEngine`, dedup/account-resolution | Transaction | **Txn(intentional)** | Debug / engine-internal / write-path support. |

**No unexplained Transaction reads.** Every one is either event-first or an intentional,
documented exception. Parity is proven (P6: `proven=true`, 6/6) and re-checked at debug startup.

---

## §B — Repository boundaries & One-Source-of-Truth (duplicate audit)

### Repositories (one domain each?)

| Repository | Owns | Boundary |
|---|---|---|
| `AccountRepository` / RoomAccount | accounts | ✅ single |
| `TransactionRepository` / RoomTransaction | transaction persistence (write + legacy read) | ✅ single (read side split to `TransactionReadSource`, intentional) |
| `TransactionReadSource` / EventSourced | read-only list projection over events | ✅ single (P7) |
| `FinancialEventRepository` / RoomFinancialEvent | canonical events | ✅ single |
| `MerchantRepository` / RoomMerchant | brands/aliases | ✅ single |
| `ParticipantRepository`, `SplitRepository`, `TransactionRunner` | split domain / tx boundary | ✅ single |
| **`InsightsRepository` / RoomInsights** | **nothing** | ❌ **empty placeholder** ("To be implemented"), **zero usages**, still bound in DI. **Obsolete → remove (H4).** |

### One Source of Truth for computed domains

| Domain | Authority | Duplicate? |
|---|---|---|
| Balance / net worth | `AccountBalanceService` + `BalanceCalculator` | ✅ single |
| Analytics (spend/income/categories/merchants/trend) | `GetFinancialAnalyticsUseCase` (its own doc: single source) | ✅ single |
| Story generation | `FinancialStoryPresenter` | ✅ single |
| Relationship logic | `RelationshipEngine` | ✅ single |
| Search | `SearchViewModel` over `TransactionReadSource` | ✅ single |
| **Merchant matching + category resolution** | **TWO systems** | ⚠️ **duplicate** (below) |

**Merchant/category duplication (known, documented — `MERCHANT_ARCHITECTURE.md`):**
- **System B** (write path): `core/domain/service/transaction/MerchantResolver.kt` +
  `CategoryResolver.kt` — wired into `InsertTransactionUseCase`.
- **System A** (UI/registry): `feature/merchant/MerchantResolver(s).kt`, `MerchantRegistry.kt`,
  `GenericCategoryKeywords.kt`, `LearnedMerchantCategoryStore.kt`; plus a UI-only
  `core/designsystem/component/LedgerBrandRegistry.kt`.
- Deliberately **not merged** in RC9 because merging changes real behaviour. Classified
  **Temporary** — the convergence point was the FinancialEvent migration, now complete.
  **Recommendation:** consolidate under one merchant/category authority via a dedicated ADR
  (ADR-0009) — not in this audit.

---

## §C — Legacy Inventory (every item classified)

**Intentional** (keep, documented) · **Temporary** (keep until X, then retire) · **Obsolete**
(no purpose — remove after proving unused).

| Item | Class | Disposition |
|---|---|---|
| `Transaction` model + write path | **Intentional** | ADR-0001 coexistence; retires after event schema carries `transferDirection`/`origin`/`cardTail` and event-first balance lands. |
| Balance/detail legacy reads | **Intentional** | Documented (§A, `EVENT_FIRST_READS.md`). |
| FinancialEvent lossy fields | **Intentional** | Documented (`READ_PARITY_REPORT.md`). |
| Two merchant/category systems | **Temporary** | Consolidate via ADR-0009 now the event architecture is done. |
| **`InsightsRepository` + `RoomInsightsRepository` + DI binding** | **Obsolete** | Empty placeholder, **zero usages** — remove in H4. |
| **`LedgerThemeType.Glass`** | **Obsolete** | No-op duplicate of `Classic`; removed from Settings UI; remains only in the no-op theme branch + `ThemePreview`. Remove the value + branch in H4. (`MidnightGlass` = "Dark", still used — keep, consider renaming.) |
| **`PipelineTracker` / `RealPipelineTracker` / `PipelineEvent`** (`core/common/diagnostics`) | **Obsolete** | Confirmed dead (nothing writes); kept only so `app/src/debug` compiles. Remove in H4 after cutting the debug dependency (live path is `feature.diagnostics.PipelineTraceSink`). |
| Notification actions (Split/Add Note/Undo) | **Temporary/Obsolete** | Built but effectively unreachable (`POST_NOTIFICATIONS` never requested). Decide in H3. |
| Ledger Split backend (no UI/route) | **Temporary** | Full backend, unreachable. Product decision in H3 (ship a route or remove). |
| `ProfileScreen` static preference rows (Currency/Theme/Language/Notifications/Data&Privacy) | **Obsolete** (as shown) | Fabricated/static; Settings/Profile consolidation in H3. |
| `ThemePreview.kt` | **Intentional** (debug) | Design-system preview tooling; trim `Glass` reference with the enum in H4. |

Nothing is left uncategorised.

---

## Violation check (ADR-000 / ADR-001 / Brownfield / Financial Truth)

| Constraint | Status |
|---|---|
| **ADR-0000 brownfield** (preserve working logic, additive) | ✅ Held — every migration was additive; the Balance Engine and write path were not rewritten. |
| **ADR-0001** (FinancialEvent alongside, coexistence) | ✅ Held — additive schema, dual-write, backfill, event-first reads with documented exceptions. |
| **Financial Truth** (immutable records, replay, currency isolation) | ✅ Held — events append-only + supersede/void (never mutated); balance still by replay; currency guard intact. |
| **Repository single responsibility** | ⚠️ One violation — empty `InsightsRepository` (obsolete). |
| **One architectural direction for reads** | ✅ Held — event-first with a single documented legacy set. |

No ADR-000/0001 or Financial-Truth violations. The two smells (empty `InsightsRepository`;
duplicate merchant/category systems) are catalogued with dispositions.

---

## Conclusion

The repository has **one architectural direction**: reads originate from FinancialEvent
behind `TransactionReadSource`, with a small, documented set of intentional Transaction reads
(balance, detail). Computed domains (balance, analytics, story, relationship, search) each have
a single authority. Two catalogued items need follow-up — **remove the obsolete
`InsightsRepository`, `LedgerThemeType.Glass`, and dead `PipelineTracker` in H4**, and
**consolidate the duplicate merchant/category systems via ADR-0009**. Neither blocks Release
Hardening; both are scheduled. **Architecture verification: PASS.**

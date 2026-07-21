# Event-first Reads (P7) — where production reads originate

Realizes ADR-0001: production reads now originate from **FinancialEvent**, behind the
`TransactionReadSource` interface, with legacy `Transaction` reads remaining **only** where
a read needs Transaction-only fields — each documented below. No functional regression
(parity proven in P6 and re-checked at every startup; see `READ_PARITY_REPORT.md`).

## Now sourced from FinancialEvent

All go through `TransactionReadSource`, bound to `EventSourcedTransactionReadSource`
(reads ACTIVE events → `toMirrorTransaction()` → same engines). ACTIVE excludes VOID
(soft-deleted) and SUPERSEDED, matching the legacy `is_deleted = 0` filter.

| Feature | ViewModel | Reads |
|---|---|---|
| Dashboard (recent activity + month analytics) | `DashboardViewModel` | recent + between |
| Insights (trend + breakdown) | `InsightsViewModel` | between |
| Accounts (activity trigger + month insight) | `AccountsViewModel` | between |
| Merchant relationship | `MerchantViewModel` | all |
| Review Queue | `ReviewInboxViewModel` | all |
| Search | `SearchViewModel` | all |
| Transactions list | `TransactionsViewModel` | recent |

## Documented intentional legacy Transaction reads

These stay on the Transaction source **on purpose** — they consume fields the FinancialEvent
mirror does not carry (`transferDirection`, `origin`, `cardTail`, `note`), or they are the
protected Balance Engine (ADR-0000).

| Read | Why it stays legacy |
|---|---|
| **Balance / Net worth** (`AccountBalanceService` → `BalanceCalculator`; `computeNetWorth`) | `BalanceCalculator` uses `transferDirection` (TRANSFER items); `AccountBalanceService` uses `origin`/`cardTail` for credit-card cross-account settlement. Frozen Balance Engine. Parity with the event path is proven for all current data. |
| **Transaction detail** record view (`TransactionDetailsViewModel`) | A record view of one transaction — intentionally shows `transferDirection`, `note`, `cardTail`, `origin`. |
| **Month-over-month change** internal prev-month read (`GetFinancialAnalyticsUseCase.computeMonthOverMonthChange`) | Analytics-internal helper for the balance-change %; not a product list read. |
| Diagnostics collectors, `ForecastEngine`, account-resolution/dedup use cases | Debug/engine-internal, not product-facing list reads. |

## Soft-delete integrity

`RoomTransactionRepository.deleteTransaction` now **voids** the mirror event
(`voidByTransactionId`, best-effort) so event-first reads drop a soft-deleted transaction,
keeping them consistent with the legacy `is_deleted` filter. (Delete is only reachable via
the notification "Undo" action, itself gated by an un-requested permission — wired for
correctness regardless.)

## Next

To move **balance** event-first too, extend the FinancialEvent schema with
`transferDirection` (+ the cross-account fields), then re-run the parity harness. Until
then, balance is a documented legacy read — not an unexplained one.

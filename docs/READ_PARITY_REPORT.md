# Read Parity Report — FinancialEvent vs Transaction (P6)

**Question:** can FinancialEvent reads completely replace Transaction reads as the
production source? **Method:** `ReadParityHarness` maps the ACTIVE FinancialEvents back to
`Transaction`-shaped domain objects (a deliberately lossy reconstruction) and runs the
**same existing engines** on both sets, comparing **domain results** (not UI, not
screenshots). Reuse-first: no new analytics engine.

The harness runs off-thread at every startup and logs the report; it is also exercised by
`ReadParityHarnessTest`.

## Parity matrix (on-device, real captured data)

| Feature | Legacy (Transaction) | Event (FinancialEvent) | Status |
|---|---|---|---|
| Balance / Accounts | `240500` | `240500` | **PASS** |
| Analytics (Dashboard / Insights) | `netSpend=59500 income=600000 cats=6 merchants=7 trend=1` | *identical* | **PASS** |
| Stories (categories / explanations) | `n=9` | `n=9` | **PASS** |
| Merchant | `total=600000 count=1` | *identical* | **PASS** |
| Review Queue (uncategorized) | `9` | `9` | **PASS** |
| Search (`q='you'`) | `1` | `1` | **PASS** |

**Summary:** total = 6 · passed = 6 · failed = 0 · intentional differences = 0 ·
unexpected differences = 0 · **proven = true**.

Balance `240500` = income 6,000.00 − expenses 3,595.00 = **AED 2,405.00** of transaction
contribution; identical from both sources. Every feature matches at the domain level.

## Documented structural differences (classified)

These do not manifest on the current data (so every row above PASSES), but they are real
properties of the lossy mirror and are recorded so P7 does not switch a read blind. The
harness detects them (unit-tested) and classifies them:

| Field omitted by FinancialEvent | Read that uses it | Impact | Classification |
|---|---|---|---|
| `transferDirection` | `BalanceCalculator.effect` (TRANSFER items only) | A TRANSFER's balance effect becomes 0 from the event path. No TRANSFER-typed transactions exist in current data → no effect today. | **Intentional / known gap** — resolve before P7 flips *balance* reads: either extend the FinancialEvent schema to carry `transferDirection`, or keep balance on the transaction source (documented legacy read). Proven by `ReadParityHarnessTest`. |
| `origin` (package), `cardTail` | `AccountBalanceService` credit-card-payment cross-account replay | Cross-account settlement resolution uses these. Not exercised by the current data. | **Intentional / known gap** — same options as above; scope for P7's balance decision. |
| `note` | none (never consulted by Financial Truth / analytics) | none | Not a parity concern. |

There are **no unexplained differences.**

## Conclusion

For every production read on the current data, FinancialEvent reproduces the Transaction
result exactly. The only structural dependencies on Transaction-only fields are
`transferDirection` (and `origin`/`cardTail` for credit-card cross-account replay), both
confined to **balance** and both classified above. P7 may therefore migrate reads
event-first, treating balance's transfer/cross-account path explicitly (extend the event
schema or document it as an intentional legacy read) rather than flipping it blind.

_Parity is objectively proven; P7 (Event-first Reads) is unblocked with the balance caveat documented._

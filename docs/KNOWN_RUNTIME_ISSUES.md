# Ledger Alpha
# Known Runtime Issues
Version: Alpha
Status: Active Investigation

---

# Purpose

This document tracks VERIFIED runtime issues observed on physical devices.

This document intentionally distinguishes between:

- VERIFIED runtime behaviour
- Confirmed source code findings
- Hypotheses
- Falsified hypotheses

Only reproducible runtime bugs belong here.

---

# Current Runtime Bugs

## ISSUE-001
### Phantom Transaction on Fresh Install

Status:
OPEN

Severity:
Critical

Observed:

Fresh install.

Launch Ledger.

Dashboard immediately shows:

+AED 1000
Merchant: Mashreq

No notifications were received.
No debug injection occurred.
Database was expected to be empty.

Expected:

Fresh installation must show

• zero transactions
• zero spending
• zero balance (or onboarding state)

Actual:

Dashboard contains an existing transaction.

---

## ISSUE-002
### Cross-screen Transaction Divergence

Status:
OPEN

Severity:
Critical

Observed:

Inject one notification.

Each screen displays different information.

Dashboard:
Version A

Transactions:
Version B

Transaction Details:
Version C

Accounts:
Version D

Expected:

Every screen must represent the exact same persisted transaction.

The repository/database must be the single source of truth.

---

## ISSUE-003
### Empty Amount in Transaction Details

Status:
OPEN

Severity:
Critical

Observed:

Opening a transaction displays

AED

without a numeric value.

Expected

AED 50.00

Actual

AED

Observed repeatedly.

---

## ISSUE-004
### ADCB Injection Failure

Status:
OPEN

Severity:
High

Observed:

Certain ADCB fixtures fail to appear.

Behaviour is inconsistent.

Mashreq-related fixtures appear more frequently.

Requires runtime verification.

---

## ISSUE-005
### Runtime Changes Not Reflected

Status:
OPEN

Severity:
Critical

Observed:

Multiple APKs claiming fixes exhibit identical runtime behaviour.

No observable UI or logic changes despite extensive source modifications.

Possible causes include:

- stale build
- stale APK
- incorrect runtime path
- instrumentation not executing
- source/runtime mismatch

Not yet proven.

---

# Confirmed Facts

These are verified through repository inspection.

✓ One Room database instance.

✓ Repository bindings are Singleton.

✓ TransactionDetails route exists.

✓ Dashboard route exists.

✓ Accounts route exists.

✓ Transactions route exists.

✓ Review Inbox currently unreachable.

✓ AmountHero composable is dead code.

✓ CategoryResolver currently returns hardcoded IDs.

✓ Categories are never seeded.

---

# Confirmed Runtime Evidence

Observed on physical device:

✓ Dashboard mismatch.

✓ Accounts mismatch.

✓ Transactions mismatch.

✓ Transaction Details mismatch.

✓ Empty AED amount.

✓ Phantom Mashreq transaction after clean install.

---

# Hypotheses Rejected

The following explanations were investigated and are currently rejected.

- Duplicate Room database instances
- Repository singleton mismatch
- Navigation graph registration failure
- Build variant shadowing
- Source-set replacement
- Foreign-key insert failure as primary cause
- Review Inbox affecting runtime bugs
- AmountHero affecting runtime bugs

---

# Still Under Investigation

- Build artifact mismatch
- Stale generated sources
- Runtime projection inconsistency
- Mapper inconsistency
- Debug injector fidelity
- Notification capture fidelity
- Compose recomposition timing
- StateFlow propagation
- APK provenance

---

# Rules Going Forward

Every bug investigation must include:

1. Exact reproduction steps

2. Runtime evidence

3. Relevant logcat output

4. Repository location

5. Root cause

6. Fix

7. Verification

No fix is considered complete until verified on a physical device.

Claims of success without runtime verification are not accepted.

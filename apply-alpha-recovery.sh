#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Alpha Recovery — 3 surgical fixes for P2, P3, P4..."

# ============================================================
# P2 — Costa SQLite foreign-key failure
# CategoryResolver returns hardcoded IDs (1/2/3) for a categories
# table that is never seeded. Under FK enforcement this makes the
# insert throw SQLiteConstraintException -> caught as DatabaseFailure
# -> transaction never persists. A NULL category_id is exempt from
# the FK constraint. Return null until categories are actually seeded.
# ============================================================
cat > "$B/core/domain/service/transaction/CategoryResolver.kt" << 'EOF'
package com.sherif.ledger.core.domain.service.transaction

import javax.inject.Inject

/**
 * Service responsible for deterministic category assignment based on merchant context.
 *
 * NOTE: Returns null until the categories table is seeded. Returning a hardcoded
 * category id for a row that does not exist in the categories table causes a
 * foreign-key constraint violation on insert (transactions.category_id references
 * categories.id). A null reference is exempt from the constraint, so transactions
 * persist correctly and remain uncategorized until real category seeding exists.
 */
class CategoryResolver @Inject constructor() {

    fun resolve(rawMerchantText: String, brandId: Long?): Long? {
        return null
    }
}
EOF
echo "P2 fixed: CategoryResolver returns null (no dangling FK reference)."

# ============================================================
# P3 — Same transaction shows different amount per screen
# DashboardViewModel and TransactionDetailsViewModel format with
# includeSymbol=false ("1000.00"). TransactionsViewModel line 39
# omitted the argument, defaulting to includeSymbol=true
# ("AED 1000.00"). The row composable (LedgerTransactionRow) renders
# the string verbatim, so the list showed "AED 1000.00" while other
# screens showed "1000.00". Align the per-transaction call site to
# includeSymbol=false so all three screens match.
# ============================================================
python3 - << 'PYEOF'
import re
p = "app/src/main/java/com/sherif/ledger/feature/transactions/presentation/viewmodel/TransactionsViewModel.kt"
s = open(p, encoding="utf-8").read()
old = "                            amount = MoneyFormatter.format(txn.amount),"
new = "                            amount = MoneyFormatter.format(txn.amount, includeSymbol = false),"
assert s.count(old) == 1, f"expected exactly 1 match, found {s.count(old)}"
s = s.replace(old, new)
open(p, "w", encoding="utf-8").write(s)
print("P3 fixed: TransactionsViewModel per-transaction amount now includeSymbol=false.")
PYEOF

# ============================================================
# P4 — Details screen renders "AED " with no number
# Line 163 builds "${state.sign}AED ${state.amount}". Fixing P2 removes
# the malformed-row source, but add a defensive guard so a blank amount
# can never render as a bare "AED ". If amount is blank, show "0.00".
# ============================================================
python3 - << 'PYEOF'
p = "app/src/main/java/com/sherif/ledger/feature/transactions/presentation/detail/TransactionDetailsScreen.kt"
s = open(p, encoding="utf-8").read()
old = 'amount = "${state.sign}AED ${state.amount}",'
new = 'amount = "${state.sign}AED ${state.amount.ifBlank { "0.00" }}",'
assert s.count(old) == 1, f"expected exactly 1 match, found {s.count(old)}"
s = s.replace(old, new)
open(p, "w", encoding="utf-8").write(s)
print("P4 fixed: Details amount guards against blank string.")
PYEOF

echo ""
echo "Done. 3 files changed:"
echo "  CategoryResolver.kt        (P2 — FK failure)"
echo "  TransactionsViewModel.kt   (P3 — amount consistency)"
echo "  TransactionDetailsScreen.kt (P4 — empty AED guard)"
echo ""
echo "Run: ./gradlew assembleDebug"

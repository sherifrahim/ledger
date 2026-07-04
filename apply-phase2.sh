#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Phase 2 — P1 (Salary category) + P5 (SMS source admission)..."

# ---- P1: Salary shown as Grocery ----
# ROOT: TransactionsViewModel hardcodes category = MerchantCategory.Grocery for
# every row. MerchantCategory has no Income/Uncategorized member, but it HAS
# a dedicated Salary member. Map income -> Salary, known merchants -> their
# category, everything else -> Bills is wrong; use the closest neutral existing
# member. Since no "Uncategorized" exists, fall back by merchant, else Salary
# only for income. For unknown expense we keep the merchant-based mapping and
# default to Shopping (a neutral spending bucket) rather than lying with Grocery.
python3 - << 'PYEOF'
p = "app/src/main/java/com/sherif/ledger/feature/transactions/presentation/viewmodel/TransactionsViewModel.kt"
s = open(p, encoding="utf-8").read()

old_row = "                            category = MerchantCategory.Grocery, // Placeholder"
new_row = "                            category = categoryFor(txn),"
assert s.count(old_row) == 1, f"P1 row: {s.count(old_row)}"
s = s.replace(old_row, new_row)

helper = '''
    private fun categoryFor(txn: com.sherif.ledger.core.domain.model.Transaction): MerchantCategory {
        val name = (txn.rawText ?: "").uppercase()
        return when {
            txn.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME -> MerchantCategory.Salary
            "COSTA" in name -> MerchantCategory.Coffee
            "CARREFOUR" in name -> MerchantCategory.Grocery
            "AMAZON" in name -> MerchantCategory.Shopping
            else -> MerchantCategory.Shopping
        }
    }
'''
idx = s.rstrip().rfind("}")
s = s[:idx] + helper + "\n}" + s[idx+1:]

old_dom = "                            dominantCategory = MerchantCategory.Grocery"
new_dom = "                            dominantCategory = if (incomeUnits > 0 && expenseUnits == 0L) MerchantCategory.Salary else MerchantCategory.Shopping"
assert s.count(old_dom) == 1, f"P1 dom: {s.count(old_dom)}"
s = s.replace(old_dom, new_dom)

open(p, "w", encoding="utf-8").write(s)
print("P1 fixed: income rows map to Salary category, not Grocery.")
PYEOF

# ---- P5: admit SMS messaging packages ----
python3 - << 'PYEOF'
p = "app/src/main/java/com/sherif/ledger/feature/capture/notification/NotificationFilter.kt"
s = open(p, encoding="utf-8").read()
import re
# match the emiratesnbd line regardless of CR, up to the closing paren of the set
old = '        "com.emiratesnbd.mobile" // Emirates NBD'
new = ('        "com.emiratesnbd.mobile", // Emirates NBD\r\n'
       '        "com.google.android.apps.messaging", // Google Messages (SMS bank alerts)\r\n'
       '        "com.samsung.android.messaging" // Samsung Messages')
assert s.count(old) == 1, f"P5: {s.count(old)}"
s = s.replace(old, new)
open(p, "w", encoding="utf-8").write(s)
print("P5 fixed: Google/Samsung Messages admitted; keyword gate unchanged.")
PYEOF

echo ""
echo "Done. 2 files changed. Run: ./gradlew assembleDebug"

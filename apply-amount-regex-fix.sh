#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying minimal AMOUNT_PATTERN fix..."
python3 - << 'PYEOF'
p = "app/src/main/java/com/sherif/ledger/feature/capture/parsing/extraction/ExtractionHelpers.kt"
s = open(p, encoding="utf-8").read()
old = r'private val AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{1,2})?|\\d+\\.\\d{1,2}|\\d+)")'
new = r'private val AMOUNT_PATTERN = Pattern.compile("(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?|\\d+(?:\\.\\d{1,2})?)")'
assert s.count(old) == 1, f"expected 1 match, found {s.count(old)}"
s = s.replace(old, new)
open(p, "w", encoding="utf-8").write(s)
print("AMOUNT_PATTERN updated.")
PYEOF
echo "Done. 1 file, 1 line changed."
echo "Run: ./gradlew assembleDebug"

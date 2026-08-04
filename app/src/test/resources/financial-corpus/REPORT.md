# Financial Extraction Report

## Overall
- Fixtures: 64
- Passed: 64
- Accuracy: 100.0% (known gaps counted as failures)
- Known gaps: 0 (documented; excluded from the regression gate)
- Untagged failures: 0
- False positives: 0
- False negatives: 0

## Accuracy by bank
- india/axis: 100% (2/2)
- india/bob: 100% (2/2)
- india/canara: 100% (2/2)
- india/federal: 100% (2/2)
- india/hdfc: 100% (9/9)
- india/icici: 100% (4/4)
- india/idfc: 100% (2/2)
- india/kotak: 100% (2/2)
- india/pnb: 100% (2/2)
- india/sbi: 100% (5/5)
- uae/adcb: 100% (13/13)
- uae/adib: 100% (2/2)
- uae/cbd: 100% (2/2)
- uae/dib: 100% (2/2)
- uae/enbd: 100% (3/3)
- uae/fab: 100% (5/5)
- uae/mashreq: 100% (3/3)
- uae/wio: 100% (2/2)

## Accuracy by transaction type
- Expense: 100% (22/22)
- Income: 100% (7/7)
- Transfer: 100% (16/16)

## Latency
- Average: 1.92 ms
- P95: 1.94 ms
- P99: 3.00 ms
- Max: 68.41 ms

## Confidence calibration
- heuristic: claims 96, actual 100.0% (n=61)
- known-bank: claims 0, actual 100.0% (n=3)

## Top failures (max 20)

## Most common failure reason
- none (0)

## Recommendations
- No action; corpus fully green.

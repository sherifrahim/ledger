# Financial Extraction Report

## Overall
- Fixtures: 61
- Passed: 58
- Accuracy: 95.1% (known gaps counted as failures)
- Known gaps: 0 (documented; excluded from the regression gate)
- Untagged failures: 3
- False positives: 0
- False negatives: 0

## Accuracy by bank
- india/axis: 100% (2/2)
- india/bob: 100% (2/2)
- india/canara: 100% (2/2)
- india/federal: 100% (2/2)
- india/hdfc: 89% (8/9)
- india/icici: 100% (4/4)
- india/idfc: 100% (2/2)
- india/kotak: 100% (2/2)
- india/pnb: 100% (2/2)
- india/sbi: 100% (5/5)
- uae/adcb: 90% (9/10)
- uae/adib: 100% (2/2)
- uae/cbd: 100% (2/2)
- uae/dib: 100% (2/2)
- uae/enbd: 67% (2/3)
- uae/fab: 100% (5/5)
- uae/mashreq: 100% (3/3)
- uae/wio: 100% (2/2)

## Accuracy by transaction type
- Expense: 100% (22/22)
- Income: 100% (6/6)
- Transfer: 100% (14/14)

## Latency
- Average: 1.40 ms
- P95: 1.11 ms
- P99: 1.82 ms
- Max: 54.48 ms

## Confidence calibration
- heuristic: claims 95, actual 100.0% (n=58)
- known-bank: claims 0, actual 0.0% (n=3)

## Top failures (max 20)
- [india/hdfc] Your HDFC OTP is 112233. Valid for 5 minutes.
  - category Statement != OTP
- [uae/adcb] Your OTP for ADCB login is 445566. Do not share 
  - category Statement != OTP
- [uae/enbd] Your Emirates NBD verification code is 998877. D
  - category Statement != OTP

## Most common failure reason
- category (3)

## Recommendations
- No action; corpus fully green.

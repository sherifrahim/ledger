# Account Identity — diagnosis and plan

**Status: all 5 steps done (2026-08-05).** Step 1 (explicit `isDefault`
identity) shipped earlier; Steps 2-5 (lazy default-account creation, untailed-
account adoption, the Merge Accounts screen, and opening-balance recombination
on merge) shipped together, each verified by unit tests plus a full
`--rerun-tasks` regression pass and on-device confirmation. Kept as historical
record of the diagnosis — the mechanism it describes is why the fix looks the
way it does.

The last correctness gap behind wrong balances. Evidence is real-device data
(see `DEVICE_FINDINGS.md`).

## Symptom
Dashboard shows AED 1,104.21; the user holds AED 1,568.52.
`1568.52 − 464.31 = 1104.21`. The ADCB account exists **twice**:

| id | name | tail | txns | balance |
|----|------|------|------|---------|
| 1 | Primary Account | *(none)* | 11 | **+1,568.52** (seeded opening balance) |
| 5 | ADCB Account | 920001 | 25 | **−464.31** |
| 13 | ADCB Account | 5986 (USD) | 3 | −46.99 |

The opening balance the user entered was seeded onto the untailed "Primary
Account", while the real ADCB transactions accumulated on a separate account.

## Root cause (verified, and why the obvious fix fails)
`DeterministicAccountIdentityResolver.createAccount()` always **inserts** a new
account; it never adopts the untailed default account created by
`EnsureDefaultAccountUseCase`.

The obvious fix — adopt the untailed default account instead of creating a
duplicate — was implemented and **reverted**, because it breaks a real
invariant:

- `EnsureDefaultAccountUseCase.execute()` returns `accounts.first().id` — the
  default account is simply "whatever account exists first".
- The resolver deliberately **refuses to bind an unrecognised institution to the
  default account** (RC7 Phase B: that behaviour was the confirmed HDFC
  currency-mixing bug).
- So if the default account is *also* the real ADCB account, the resolver won't
  bind to it, falls through to creation again, and a duplicate reappears.
  Confirmed by `AccountIdentityResolverTest` → `BOUND_EXISTING` became
  `CREATED_NEW`.

**The conflict is conceptual: "the default/fallback account" and "the user's real
bank account" are currently the same row, and the resolver needs them to be
different things.**

## Plan (in order)
1. **Give the default account an explicit identity.** Stop deriving it from
   `accounts.first()`. Either persist a `defaultAccountId` preference, or add an
   `isDefault`/`isFallback` flag on the account. Then "is this the fallback?" is a
   property, not a position, and a real account can never accidentally *be* the
   fallback.
2. **Onboarding should not pre-create a generic "Primary Account".** Import
   discovers the real institutions; the balance-confirmation step should then seed
   onto those. If nothing is discovered, create the fallback lazily.
3. **Only then** allow adoption/merge: when a recognised institution+tail matches
   an existing untailed, non-fallback account of the same type and currency, adopt
   it rather than creating a sibling.
4. **Add a user-facing "Merge accounts" action** (Accounts screen) for data that
   already split, and to correct any future mistake. Reassignment already exists
   (`TransactionRepository.reassignTransactions`) — it needs UI.
5. Re-seed/recompute opening balances after a merge.

## Related, same area
- **Junk accounts** — 12 created from SMS sender IDs (`Smiles`, `eandINF`,
  `eandUAE`, `Tabby`, `MBANKAlert`, `AX-iPaytm-S`, `JK-SBIUPI-S`, `com.truecaller`,
  `com.google.android.apps.messaging`). A generic messenger or telecom sender must
  never become an account. One name even contains a mojibake char (`�1959`).
- **Same purchase captured 2–3×** — bank app + Google Messages + Truecaller, each
  landing on a *different* account so reconciliation never matches them. Dedup must
  run on (amount, ~timestamp, tail) **before** account attribution.
- **INR data present** — the user has Indian bank SMS (₹35,800 / ₹15,000). Cash
  total is now currency-scoped, but multi-currency accounts need a real design.

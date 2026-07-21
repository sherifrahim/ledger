# Merchant Architecture — Investigation & Merge Decision

RC9 Phase A. This document corrects an inaccurate assumption made during RC8:
RC8's own summary described the two merchant/category systems below as "two
unrelated systems [where System B is] largely a stub... auto-creates a
generic Brand row... unrelated to the curated MerchantRegistry." That
characterization is **wrong on one point**, discovered only by tracing every
real call site rather than the DAO/repository declarations: System B's Brand
mechanism is not dead weight — a real, live screen reads it back. This
document is the corrected, fully-evidenced picture.

## The three actual concerns

There are not two duplicate systems doing the same job. There are **three
distinct concerns**, two of which happen to live in confusingly similarly-named
classes:

| # | Concern | Owner | Status |
|---|---|---|---|
| 1 | Spending **category** classification | System A (`feature/merchant/` + `CategoryIntelligenceEngine`) | Live, real, the only category source anywhere |
| 2 | **Merchant identity dedup** for "other purchases from this merchant" history | System B's Brand mechanism (`core/domain/service/transaction/`) | Live, real, but crude (exact raw-text match, no canonicalization) |
| 3 | **Merchant canonicalization** for display ("AMZN MKTPLACE" → "Amazon", with category/logo/color/country) | System A (`feature/merchant/MerchantRegistry`/`MerchantResolver`) | Live, real, the only canonicalization anywhere |

System A does both #1 and #3 today. System B only ever did #2, and everyone
(including RC8's own investigation) assumed it did nothing at all.

## Ownership & responsibilities

**System A — `feature/merchant/`**: `MerchantRegistry` (curated list of ~19
`MerchantProfile`s: canonical name, aliases, category, optional subcategory,
brand color, country, confidence), `MerchantResolver` (alias matching:
exact-token → longest-substring → unresolved, never invents), `GenericCategoryKeywords`
(keyword fallback), `LearnedMerchantCategoryStore` (Room-backed, user-taught
category overrides), `MerchantResolvers.kt` (`MerchantCategoryResolver`/
`MerchantBrandResolver`/`MerchantDiagnostics` — thin read-only facades).
Doc comment on `MerchantResolver.kt` (System A) explicitly states: "This
layer is additive and standalone: nothing in the frozen extraction/persistence
pipeline calls it yet" — true for the WRITE path, but it is very much called
for the READ path (see below).

**System B — `core/domain/service/transaction/`**: `MerchantResolver.kt`
(different class, same name, different package — 24 lines, does ONE thing:
exact-raw-text lookup in `brands`/`merchant_aliases`, auto-creates a
`Brand(name = rawMerchantText, brandKey = "manual")` on miss),
`CategoryResolver.kt` (14 lines, `resolve()` unconditionally `return null`,
with its own doc comment explaining why — the `categories` table is never
seeded, so returning anything else would violate the `transactions.category_id`
foreign key). Backed by Room: `BrandEntity`/`CategoryEntity`/`MerchantAliasEntity`,
`BrandDao`/`CategoryDao`, `core/domain/repository/{MerchantRepository,CategoryRepository}`,
`RoomMerchantRepository`/`RoomCategoryRepository`.

## Complete call graph

**System A (System A's own files only, i.e. `feature/merchant/*`) is
imported by** (`grep "com.sherif.ledger.feature.merchant\."` across
`app/src/main`, 13 files, excluding System A's own internal files):
- `core/domain/usecase/analytics/GetFinancialAnalyticsUseCase.kt` — the real analytics/Dashboard category+merchant-total source (`MerchantResolver.resolve()`, `GenericCategoryKeywords`, `LearnedMerchantCategoryStore` — the documented 4-tier read-time chain).
- `core/domain/service/intelligence/CategoryIntelligenceEngine.kt` (RC8) — same 3 tiers, restructured with confidence/reason/subcategory, plus AI fallback.
- `core/domain/service/intelligence/LearnedDecisionStore.kt` — unrelated, just happens to be adjacent (RC8's generic learning store, not merchant-specific).
- `feature/ai/validation/AISuggestionValidator.kt` — validates an AI merchant-classification suggestion's `category` field against `MerchantCategory` enum values.
- `feature/review/presentation/{components/ReviewCard.kt,viewmodel/ReviewInboxViewModel.kt}` — the Review Inbox, where a user teaches `LearnedMerchantCategoryStore` a category.
- `core/domain/model/TransactionStory.kt` — the `category: String` field `GetFinancialAnalyticsUseCase.transactionStories()` returns, which `TransactionDetailsViewModel` also consumes (see below).
- `feature/relationship/{RelationshipEngine.kt,RelationshipResolver.kt}` — read-only use of `MerchantResolver` (System A) for canonical-merchant matching inside relationship resolvers (e.g. grouping recurring charges by canonical merchant, not raw text).
- `core/domain/service/account/InstitutionRegistry.kt` — false positive, this file only shares the word "merchant" in a comment; no real coupling.

**System B (`core/domain/service/transaction/{MerchantResolver,CategoryResolver}`)
is imported by exactly one production file**: `core/domain/usecase/transaction/InsertTransactionUseCase.kt`
(lines 39-40, 65-66) — the real write path. Confirmed via grep; no other
production file references these two classes by name.

## Write path (traced through `InsertTransactionUseCase.kt`)

Every insert (`InsertTransactionUseCase.execute()`, lines 63-68): `brandId =
merchantResolver.resolve(params.rawMerchantText)` (System B — exact-text
lookup/create), `categoryId = categoryResolver.resolve(params.rawMerchantText,
brandId)` (System B — always `null`). Both land on the persisted `Transaction`
row (`brandId`/`categoryId` fields, `TransactionEntity`'s matching columns).

**`categoryId` is always `null` for every transaction ever inserted** — a
direct, unavoidable consequence of `CategoryResolver.resolve()`'s hardcoded
`return null`. Confirmed no other code path ever sets it.

**`brandId` is always non-null** (barring a DB failure) — every transaction
gets a real `Brand` row, deduplicated by EXACT raw text match only. `BrandDao.getBrandByAlias`
(line 23): `WHERE rawText = :rawText` — no `UPPER()`, no trim, no
normalization of any kind. This means "Purchase at AMAZON.AE" and "purchase
at amazon.ae " (trailing space, different case — both plausible across two
capture channels, SMS vs push notification, for the literal same real-world
purchase) would create **two separate `Brand` rows**, not one.

## Read path

**`categoryId` (System B) is confirmed NEVER read by anything** — grep for
`.categoryId` across `app/src/main` returns zero hits outside System B's own
write-path files. Every category shown anywhere in the app (Dashboard,
Insights, Transaction Details, Review Inbox) comes from System A, computed
at read time from `Transaction.rawText`, never from the stored `categoryId`
column. This is unambiguous, corroborated dead weight.

**`brandId` (System B) IS read — a real, live feature**:
`feature/transactions/presentation/detail/viewmodel/TransactionDetailsViewModel.kt`,
lines 76-81 and 124-137. On the Transaction Details screen:
1. `txn.brandId?.let { merchantRepository.getAllBrands().find { it.id == brandId }?.name }` — the displayed "merchant" name falls back to this Brand's `name` (which, per the write path above, is just the raw text echoed back — `Brand.name` is never canonicalized) before falling back to `txn.rawText` itself. In practice this rarely changes what's displayed (`Brand.name == rawText` almost always, since that's what it was created from) — but it IS a real read of System B data, not inert.
2. **"Merchant history"** (other purchases from the same brand, up to 5, most recent first) — `all.filter { it.brandId == txn.brandId }`. This is the one place System B's Brand mechanism does real, user-visible work: grouping past purchases by merchant identity for the Transaction Details screen's history list.

**The category shown on that same screen** comes from System A
(`getFinancialAnalyticsUseCase.transactionStories(listOf(txn))[txn.id]?.category`,
line 85) — confirming the category/merchant-identity split holds even within
a single screen: one field from System A, one from System B.

## Dead code (confirmed, not assumed)

- **`CategoryEntity`/`CategoryDao`/`core/domain/model/Category.kt`/`CategoryRepository`/`RoomCategoryRepository`**: fully dead. `categoryRepository.insertCategory(...)` and `.getAllCategories()` have ZERO call sites anywhere outside `RoomCategoryRepository`'s own declaration and `RepositoryModule`'s Hilt binding (confirmed via grep). The `categories` table exists in the schema, is bound in DI, and is never written to or read from by anything.
- **`core/domain/service/transaction/CategoryResolver.kt`**: functionally dead (always returns `null`), but its FILE is not removable in isolation — it's still a required constructor param of `InsertTransactionUseCase` and exists specifically to keep `categoryId` `null` (its own doc comment explains the FK-violation risk of doing anything else). Removing it means also removing the `categoryId` column/param, not just the class.

## A real documentation inaccuracy found

`BrandEntity.kt`'s `brandKey` field comment says "Matches LedgerBrandRegistry"
(`core/designsystem/component/LedgerBrandRegistry.kt`). This is **not true** —
System B's `MerchantResolver` (the only writer of `brandKey`) hardcodes
`brandKey = "manual"` for every single row, always. `LedgerBrandRegistry` is
a completely unrelated, THIRD concept: a UI-only brand-icon/color resolver
(`LedgerBrandIcon.kt`) that resolves fresh from the merchant NAME string at
render time — it never reads the persisted `brandKey` column at all. The
comment describes an aspiration that was never implemented, not current
behavior. Corrected here so a future session doesn't trust the stale comment.

## Duplicate logic

Both System A's `MerchantResolver.resolve()` and System B's
`MerchantResolver.resolve()` do "raw text → merchant identity," but they are
NOT interchangeable:

- System A: alias-registry lookup (exact-token then substring, ~19 known
  merchants), returns a CANONICAL name ("Amazon") + category + confidence +
  reason; unresolved raw text is never persisted anywhere, just displayed
  as a title-cased fallback.
- System B: exact-raw-text lookup against a growing, unbounded, self-populating
  `brands` table; returns an internal `Long` id; the "name" is never
  canonicalized, it's the raw text verbatim.

This means System B's merchant grouping is measurably WORSE than System A's
for the same real-world merchant appearing with slightly different raw text
across capture channels (SMS wording vs. push-notification wording for the
same bank event) — a real, demonstrated limitation, not a hypothetical one.

## Risk of merging

**Recommendation: do NOT merge in this RC.** Evidence:

1. **Functional risk, not just schema risk**: replacing System B's exact-match
   Brand grouping with System A's alias-based canonical grouping in
   `TransactionDetailsViewModel` would CHANGE what "merchant history" shows
   (more transactions would group together) — this fails the RC9 instruction's
   own bar ("only merge if functionality remains identical"). It would likely
   be a real improvement, but it is a behavior change, not a safe merge.
2. **Schema risk**: `brands`/`merchant_aliases`/`categories` are real Room
   tables with a migration history. Dropping or restructuring them requires a
   new migration touching live user data (every existing transaction's
   `brandId` foreign key), not a pure code change.
3. **Test risk**: 6 test files construct `InsertTransactionUseCase` (and
   therefore both System B resolvers) directly:
   `LiveCaptureIntegrationTest.kt`, `ProcessNotificationUseCaseIntentRoutingTest.kt`,
   `SmsIngestionTest.kt`, `ProcessNotificationUseCaseTest.kt`,
   `InsertTransactionUseCaseTest.kt`, plus a dedicated `MerchantResolverTest.kt`
   for System B specifically. A merge touches all of them.
4. **No diagnostics improvement from merging alone** — the RC9 bar requires
   diagnostics to improve, not just stay flat; a same-behavior refactor
   doesn't clear that bar by itself.

**What WOULD be safe, narrow, and worth doing in a future RC** (not done
here — flagged, not attempted, per the instruction to document rather than
guess): normalize `BrandDao.getBrandByAlias`'s lookup the same way System A
and `LearnedMerchantCategoryStore` already normalize (uppercase + collapse
whitespace) before the exact match. This fixes the demonstrated
casing/whitespace fragmentation bug WITHOUT touching the schema, WITHOUT
merging the two systems, and WITHOUT changing which system owns what —
purely tightening System B's existing exact-match rule to be
whitespace/case-insensitive, matching the normalization every other merchant
lookup in the codebase already does.

## `CategoryIntelligenceEngine` (RC8) — is it a third partial system?

No — confirmed by re-reading its own doc comment and code: it deliberately
duplicates System A's 4 deterministic tiers rather than introducing new
persistence, specifically because `GetFinancialAnalyticsUseCase` (which also
has those 4 tiers inline) is a frozen file. It has no database table of its
own, reads from the same `MerchantRegistry`/`LearnedMerchantCategoryStore`/
`GenericCategoryKeywords` System A already uses, and adds one on-demand AI
tier gated by `ConfidenceGate`. It is accurately described as "a second
read-time consumer of System A," not a new system.

## Conclusion

No code changed as a result of this investigation (per the instruction: only
merge if safe, and it isn't). The concrete, evidenced findings above —
System B's Brand mechanism being real and load-bearing, `CategoryEntity`/
`CategoryDao`/`CategoryRepository` being safely removable dead code, the
stale `LedgerBrandRegistry` comment, and the exact-match casing bug — feed
directly into RC9's Phase E (dead code removal) and the "Recommendations"
section of the overall RC9 report.

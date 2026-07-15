package com.sherif.ledger.core.domain.service.diagnostic

/**
 * TEMPORARY. Structured report for one investigation: why does Total Balance
 * show an incorrect figure. Every field maps directly to a specific question
 * asked. Not the deferred Explainability domain model — disposable once the
 * investigation concludes.
 */
data class BalanceTraceReport(
    val accounts: List<AccountTrace>,
    val netWorthMinor: Long,
    val assetsMinor: Long,
    val liabilitiesMinor: Long,
    val crossAccountContributions: List<CrossAccountContribution>,
    val duplicateFingerprints: List<DuplicateFingerprintFinding>,
    val duplicateAccountIdentities: List<com.sherif.ledger.core.domain.model.DuplicateAccountFinding>,
    val impossibleGrowthEvents: List<ImpossibleGrowthFinding>,
    val typeConflicts: List<TypeConflictFinding>,
)

/** One row per account — items 1 through 8 of the requested report. */
data class AccountTrace(
    val accountId: Long,
    val accountName: String,
    val accountType: String,
    val cardOrAccountTail: String?,
    val transactionCount: Int,
    val sumOfTransactionEffectsMinor: Long,
    val sumOfLiabilityAdjustmentsMinor: Long,
    val openingBalanceMinor: Long,
    val finalBalanceMinor: Long,
    val liveBalanceMinor: Long?, // what AccountBalanceService actually returns for this account, for cross-check
)

/** A single credit-card-payment transaction whose (institution, tail, currency)
 *  identity matched more than one liability account — its adjustment would be
 *  applied once per match, i.e. more than once total. */
data class CrossAccountContribution(
    val paymentTransactionId: Long,
    val matchedAccountIds: List<Long>,
)

data class DuplicateFingerprintFinding(
    val fingerprint: String,
    val transactionIds: List<Long>,
)

/** A single replay step whose effect magnitude exceeds the transaction's own
 *  amount — cannot happen under correct arithmetic, so any entry here is
 *  definitive evidence of a computation bug, not a possible one. */
data class ImpossibleGrowthFinding(
    val accountId: Long,
    val transactionId: Long,
    val transactionAmountMinor: Long,
    val computedEffectMinor: Long,
)

/** An account whose declared type doesn't match the pattern of transactions
 *  actually observed on it — e.g. salary deposits on a CREDIT account, or
 *  repeated card-payment-shaped wording on a CHECKING account. */
data class TypeConflictFinding(
    val accountId: Long,
    val accountName: String,
    val declaredType: String,
    val conflictDescription: String,
    val exampleTransactionIds: List<Long>,
)




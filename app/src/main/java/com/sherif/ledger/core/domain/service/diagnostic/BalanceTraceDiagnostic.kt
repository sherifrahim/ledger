package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.account.AccountMatching
import com.sherif.ledger.core.domain.service.account.InstitutionRegistry
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.service.transaction.BalanceCalculator
import com.sherif.ledger.core.domain.usecase.account.DetectDuplicateAccountIdentitiesUseCase
import com.sherif.ledger.feature.relationship.RelationshipEngine
import com.sherif.ledger.feature.relationship.RelationshipType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * TEMPORARY. Deterministic Financial Balance Trace — produces the exact
 * structured [BalanceTraceReport] requested for this investigation, not
 * generic logging. Reuses [DetectDuplicateAccountIdentitiesUseCase] (RC2)
 * rather than rebuilding duplicate-identity detection. Computes nothing new —
 * replays the same BalanceCalculator/RelationshipEngine/AccountMatching logic
 * AccountBalanceService already uses, capturing every intermediate value this
 * one time instead of discarding it. Disposable once the investigation
 * concludes; not the deferred Explainability model.
 */
class BalanceTraceDiagnostic @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val relationshipEngine: RelationshipEngine,
    private val institutionRegistry: InstitutionRegistry,
    private val balanceCalculator: BalanceCalculator,
    private val accountBalanceService: AccountBalanceService,
    private val detectDuplicateAccountIdentitiesUseCase: DetectDuplicateAccountIdentitiesUseCase,
) {

    suspend fun run(): BalanceTraceReport {
        val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val allTransactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data ?: emptyList()
        val txnById = allTransactions.associateBy { it.id }
        val byAccount = allTransactions.groupBy { it.accountId }

        val relationships = if (allTransactions.isNotEmpty()) relationshipEngine.analyze(allTransactions) else emptyList()
        val creditCardPayments = relationships.filter { it.type == RelationshipType.CREDIT_CARD_PAYMENT }

        val liveBalances = accountBalanceService.currentBalances().associateBy { it.account.id }

        // ---- Per-account trace (items 1-8) ----
        val accountTraces = accounts.map { account ->
            var effectSum = 0L
            val ownTxns = byAccount[account.id].orEmpty()
            ownTxns.forEach { effectSum += balanceCalculator.effect(it, account.type) }

            var liabilityAdjustmentSum = 0L
            if (account.type.isLiability) {
                creditCardPayments.forEach { rel ->
                    val payment = txnById[rel.sourceTransactionId] ?: return@forEach
                    val institution = institutionRegistry.resolve(payment.origin?.packageName)
                    val tail = payment.cardTail
                    if (tail != null && AccountMatching.matches(institution, tail, payment.amount.currencyCode, account)) {
                        liabilityAdjustmentSum += balanceCalculator.liabilityPaymentEffect(payment.amount)
                    }
                }
            }

            AccountTrace(
                accountId = account.id,
                accountName = account.name,
                accountType = account.type.name,
                cardOrAccountTail = account.accountNumberTail,
                transactionCount = ownTxns.size,
                sumOfTransactionEffectsMinor = effectSum,
                sumOfLiabilityAdjustmentsMinor = liabilityAdjustmentSum,
                openingBalanceMinor = account.openingBalance.minorUnits,
                finalBalanceMinor = account.openingBalance.minorUnits + effectSum + liabilityAdjustmentSum,
                liveBalanceMinor = liveBalances[account.id]?.balance?.minorUnits,
            )
        }

        val accountById = accounts.associateBy { it.id }
        val assets = accountTraces.filter { at -> accountById[at.accountId]?.type?.isLiability == false }.sumOf { it.finalBalanceMinor }
        val liabilities = accountTraces.filter { at -> accountById[at.accountId]?.type?.isLiability == true }.sumOf { it.finalBalanceMinor }

        // ---- Cross-account contribution: same payment matching >1 liability account ----
        val paymentToMatchedAccounts = mutableMapOf<Long, MutableList<Long>>()
        accounts.filter { it.type.isLiability }.forEach { account ->
            creditCardPayments.forEach { rel ->
                val payment = txnById[rel.sourceTransactionId] ?: return@forEach
                val institution = institutionRegistry.resolve(payment.origin?.packageName)
                val tail = payment.cardTail
                if (tail != null && AccountMatching.matches(institution, tail, payment.amount.currencyCode, account)) {
                    paymentToMatchedAccounts.getOrPut(payment.id) { mutableListOf() }.add(account.id)
                }
            }
        }
        val crossAccountContributions = paymentToMatchedAccounts
            .filter { it.value.size > 1 }
            .map { CrossAccountContribution(it.key, it.value) }

        // ---- Duplicate fingerprints (should be structurally impossible given the
        //      DB unique constraint, but verified directly against live data rather
        //      than assumed) ----
        val duplicateFingerprints = allTransactions.groupBy { it.fingerprint }
            .filter { it.value.size > 1 }
            .map { DuplicateFingerprintFinding(it.key, it.value.map { t -> t.id }) }

        // ---- Duplicate account identities (RC2, reused not rebuilt) ----
        val duplicateAccountIdentities = detectDuplicateAccountIdentitiesUseCase.execute()

        // ---- Impossible balance growth: any single step whose effect exceeds the
        //      transaction's own amount is definitive evidence of a computation
        //      bug, not a judgment call ----
        val impossibleGrowth = mutableListOf<ImpossibleGrowthFinding>()
        accounts.forEach { account ->
            byAccount[account.id].orEmpty().forEach { txn ->
                val effect = balanceCalculator.effect(txn, account.type)
                if (kotlin.math.abs(effect) > txn.amount.minorUnits) {
                    impossibleGrowth += ImpossibleGrowthFinding(account.id, txn.id, txn.amount.minorUnits, effect)
                }
            }
        }

        // ---- Type conflicts: declared account type vs observed transaction pattern ----
        val typeConflicts = mutableListOf<TypeConflictFinding>()
        val salaryPhrases = listOf("salary", "payroll", "wps", "end of service", "gratuity")
        val cardPaymentWords = listOf("credit card", "card payment", "towards", "outstanding")
        accounts.forEach { account ->
            val ownTxns = byAccount[account.id].orEmpty()
            if (account.type.isLiability) {
                val salaryTxns = ownTxns.filter { t -> salaryPhrases.any { p -> t.rawText?.lowercase()?.contains(p) == true } }
                if (salaryTxns.isNotEmpty()) {
                    typeConflicts += TypeConflictFinding(
                        account.id, account.name, account.type.name,
                        "Liability account has ${salaryTxns.size} salary-worded transaction(s) — salary does not belong on a credit account",
                        salaryTxns.map { it.id },
                    )
                }
            } else {
                val cardPaymentTxns = ownTxns.filter { t ->
                    t.type == TransactionType.EXPENSE && cardPaymentWords.any { w -> t.rawText?.lowercase()?.contains(w) == true }
                }
                if (cardPaymentTxns.size >= 2) {
                    typeConflicts += TypeConflictFinding(
                        account.id, account.name, account.type.name,
                        "Asset account has ${cardPaymentTxns.size} card-payment-worded EXPENSE transaction(s) — possible misrouted credit activity",
                        cardPaymentTxns.map { it.id },
                    )
                }
            }
        }

        val report = BalanceTraceReport(
            accounts = accountTraces,
            netWorthMinor = assets - liabilities,
            assetsMinor = assets,
            liabilitiesMinor = liabilities,
            crossAccountContributions = crossAccountContributions,
            duplicateFingerprints = duplicateFingerprints,
            duplicateAccountIdentities = duplicateAccountIdentities,
            impossibleGrowthEvents = impossibleGrowth,
            typeConflicts = typeConflicts,
        )

        logReport(report)
        return report
    }

    private fun logReport(report: BalanceTraceReport) {
        LedgerLogger.d("===== FINANCIAL BALANCE TRACE =====")
        report.accounts.forEach { a ->
            LedgerLogger.d(
                "ACCOUNT id=${a.accountId} name='${a.accountName}' type=${a.accountType} tail=${a.cardOrAccountTail} " +
                    "txnCount=${a.transactionCount} sumTxnEffects=${a.sumOfTransactionEffectsMinor} " +
                    "sumLiabilityAdj=${a.sumOfLiabilityAdjustmentsMinor} opening=${a.openingBalanceMinor} " +
                    "final=${a.finalBalanceMinor} liveReported=${a.liveBalanceMinor} " +
                    "match=${a.finalBalanceMinor == a.liveBalanceMinor}"
            )
        }
        LedgerLogger.d("NET WORTH = assets(${report.assetsMinor}) - liabilities(${report.liabilitiesMinor}) = ${report.netWorthMinor}")

        if (report.crossAccountContributions.isEmpty()) {
            LedgerLogger.d("Cross-account contributions: none found.")
        } else report.crossAccountContributions.forEach {
            LedgerLogger.e("CROSS-ACCOUNT: payment txn#${it.paymentTransactionId} matched accounts ${it.matchedAccountIds} — adjustment applied ${it.matchedAccountIds.size} times")
        }

        if (report.duplicateFingerprints.isEmpty()) {
            LedgerLogger.d("Duplicate fingerprints: none found.")
        } else report.duplicateFingerprints.forEach {
            LedgerLogger.e("DUPLICATE FINGERPRINT: ${it.fingerprint.take(12)} shared by txns ${it.transactionIds}")
        }

        if (report.duplicateAccountIdentities.isEmpty()) {
            LedgerLogger.d("Duplicate account identities: none found.")
        } else report.duplicateAccountIdentities.forEach {
            LedgerLogger.e("DUPLICATE ACCOUNT IDENTITY: package=${it.packageName} tail=${it.cardTail} accounts=${it.accountIds.zip(it.accountNames)}")
        }

        if (report.impossibleGrowthEvents.isEmpty()) {
            LedgerLogger.d("Impossible balance growth: none found.")
        } else report.impossibleGrowthEvents.forEach {
            LedgerLogger.e("IMPOSSIBLE GROWTH: account=${it.accountId} txn#${it.transactionId} amount=${it.transactionAmountMinor} but effect=${it.computedEffectMinor}")
        }

        if (report.typeConflicts.isEmpty()) {
            LedgerLogger.d("Type conflicts: none found.")
        } else report.typeConflicts.forEach {
            LedgerLogger.e("TYPE CONFLICT: account=${it.accountId} '${it.accountName}' (${it.declaredType}): ${it.conflictDescription} e.g. txns ${it.exampleTransactionIds}")
        }
        LedgerLogger.d("===== END TRACE =====")
    }
}



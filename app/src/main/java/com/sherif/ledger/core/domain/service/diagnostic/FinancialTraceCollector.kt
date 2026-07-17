package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.LedgerResult
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
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * RC4: the permanent Financial Trace, replacing the disposable RC2/RC3
 * BalanceTraceDiagnostic — same computation, promoted to a standing
 * DiagnosticCollector rather than a one-off investigation tool. Computes
 * nothing new: replays the same BalanceCalculator/RelationshipEngine/
 * AccountMatching logic AccountBalanceService already uses in production,
 * capturing every intermediate value instead of discarding it, exactly as
 * the RC2/RC3 version did. Reuses DetectDuplicateAccountIdentitiesUseCase
 * rather than rebuilding duplicate-identity detection.
 */
class FinancialTraceCollector @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val relationshipEngine: RelationshipEngine,
    private val institutionRegistry: InstitutionRegistry,
    private val balanceCalculator: BalanceCalculator,
    private val accountBalanceService: AccountBalanceService,
    private val detectDuplicateAccountIdentitiesUseCase: DetectDuplicateAccountIdentitiesUseCase,
) : DiagnosticCollector {

    override val id: String = "financial_trace"

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    override suspend fun collect(): DiagnosticSection {
        val report = buildReport()
        return DiagnosticSection.Json(id, json.encodeToString(BalanceTraceReport.serializer(), report))
    }

    /** Kept callable directly (not only through [collect]) so the Developer
     *  Console's "Financial Trace" tab can render the structured report as a
     *  readable table, not just raw JSON text. */
    suspend fun buildReport(): BalanceTraceReport {
        val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val allTransactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data ?: emptyList()
        val txnById = allTransactions.associateBy { it.id }
        val byAccount = allTransactions.groupBy { it.accountId }

        val relationships = if (allTransactions.isNotEmpty()) relationshipEngine.analyze(allTransactions) else emptyList()
        val creditCardPayments = relationships.filter { it.type == RelationshipType.CREDIT_CARD_PAYMENT }

        val liveBalances = accountBalanceService.currentBalances().associateBy { it.account.id }

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
                    if (tail != null && payment.accountId != account.id &&
                        AccountMatching.matches(institution, tail, payment.amount.currencyCode, account)
                    ) {
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

        val duplicateFingerprints = allTransactions.groupBy { it.fingerprint }
            .filter { it.value.size > 1 }
            .map { DuplicateFingerprintFinding(it.key, it.value.map { t -> t.id }) }

        val duplicateAccountIdentities = detectDuplicateAccountIdentitiesUseCase.execute()

        val impossibleGrowth = mutableListOf<ImpossibleGrowthFinding>()
        accounts.forEach { account ->
            byAccount[account.id].orEmpty().forEach { txn ->
                val effect = balanceCalculator.effect(txn, account.type)
                if (kotlin.math.abs(effect) > txn.amount.minorUnits) {
                    impossibleGrowth += ImpossibleGrowthFinding(account.id, txn.id, txn.amount.minorUnits, effect)
                }
            }
        }

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
        LedgerLogger.d("===== FINANCIAL TRACE =====")
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
        if (report.crossAccountContributions.isNotEmpty()) {
            report.crossAccountContributions.forEach {
                LedgerLogger.e("CROSS-ACCOUNT: payment txn#${it.paymentTransactionId} matched accounts ${it.matchedAccountIds} — adjustment applied ${it.matchedAccountIds.size} times")
            }
        }
        if (report.duplicateFingerprints.isNotEmpty()) {
            report.duplicateFingerprints.forEach {
                LedgerLogger.e("DUPLICATE FINGERPRINT: ${it.fingerprint.take(12)} shared by txns ${it.transactionIds}")
            }
        }
        if (report.duplicateAccountIdentities.isNotEmpty()) {
            report.duplicateAccountIdentities.forEach {
                LedgerLogger.e("DUPLICATE ACCOUNT IDENTITY: package=${it.packageName} tail=${it.cardTail} accounts=${it.accountIds.zip(it.accountNames)}")
            }
        }
        if (report.impossibleGrowthEvents.isNotEmpty()) {
            report.impossibleGrowthEvents.forEach {
                LedgerLogger.e("IMPOSSIBLE GROWTH: account=${it.accountId} txn#${it.transactionId} amount=${it.transactionAmountMinor} but effect=${it.computedEffectMinor}")
            }
        }
        if (report.typeConflicts.isNotEmpty()) {
            report.typeConflicts.forEach {
                LedgerLogger.e("TYPE CONFLICT: account=${it.accountId} '${it.accountName}' (${it.declaredType}): ${it.conflictDescription} e.g. txns ${it.exampleTransactionIds}")
            }
        }
        LedgerLogger.d("===== END TRACE =====")
    }
}




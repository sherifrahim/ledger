package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.diagnostics.PipelineResult
import com.sherif.ledger.feature.diagnostics.PipelineTraceSink
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class HealthCheck(val name: String, val passed: Boolean, val details: String)

@Serializable
data class DatabaseHealthReport(val checks: List<HealthCheck>)

/**
 * The health checks RC4 asked for, run against real data. Reuses
 * [FinancialTraceCollector.buildReport] rather than recomputing duplicate
 * detection, duplicate fingerprints, cross-account contribution, and
 * impossible-growth checks a second time — those are already computed there
 * from the exact same source data, so this collector derives its verdicts
 * from that one shared computation instead of running it twice.
 *
 * Two checks are reported honestly rather than faked:
 * - "Orphaned relationships" is structurally not applicable — relationships
 *   are never persisted, always recomputed fresh from currently-valid
 *   transactions, so an orphaned one referencing a deleted transaction
 *   cannot exist by construction.
 * - "Failed reconciliations" is a proxy, not an exact measurement — the
 *   pipeline trace model doesn't isolate reconciliation as its own stage
 *   (see PipelineCollector's own note on this), so this counts traces that
 *   ended REJECTED for any reason, not specifically reconciliation failures.
 */
class DatabaseHealthCollector @Inject constructor(
    private val financialTraceCollector: FinancialTraceCollector,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val pipelineTraceSink: PipelineTraceSink,
) : DiagnosticCollector {

    override val id: String = "database_health"

    override suspend fun collect(): DiagnosticSection {
        val report = financialTraceCollector.buildReport()
        val checks = mutableListOf<HealthCheck>()

        checks += HealthCheck(
            "duplicate_accounts",
            report.duplicateAccountIdentities.isEmpty(),
            if (report.duplicateAccountIdentities.isEmpty()) "No duplicate account identities found."
            else "${report.duplicateAccountIdentities.size} duplicate identity(ies): " +
                report.duplicateAccountIdentities.joinToString { "${it.packageName}/${it.cardTail} -> accounts ${it.accountIds}" },
        )

        checks += HealthCheck(
            "duplicate_fingerprints",
            report.duplicateFingerprints.isEmpty(),
            if (report.duplicateFingerprints.isEmpty()) "No duplicate transaction fingerprints found — the DB unique constraint holds."
            else "${report.duplicateFingerprints.size} duplicate fingerprint(s) — should be structurally impossible given the DB unique constraint: " +
                report.duplicateFingerprints.joinToString { "${it.fingerprint.take(12)} -> txns ${it.transactionIds}" },
        )

        checks += HealthCheck(
            "orphaned_relationships",
            true,
            "Not applicable: relationships are never persisted, always recomputed fresh from currently-valid transactions — an orphaned relationship referencing a deleted transaction cannot exist by construction.",
        )

        val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val accountIds = accounts.map { it.id }.toSet()
        val allTransactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data ?: emptyList()
        val orphanedTransactions = allTransactions.filter { it.accountId !in accountIds }
        checks += HealthCheck(
            "orphaned_transactions",
            orphanedTransactions.isEmpty(),
            if (orphanedTransactions.isEmpty()) "No transactions reference a non-existent account."
            else "${orphanedTransactions.size} transaction(s) reference an account that no longer exists: ${orphanedTransactions.map { it.id }}",
        )

        val failedReconciliations = pipelineTraceSink.recent().count { it.result == PipelineResult.REJECTED }
        checks += HealthCheck(
            "failed_reconciliations_proxy",
            true, // informational count, not a pass/fail — rejections are often correct behavior, not corruption
            "$failedReconciliations recent notification(s) ended REJECTED (proxy count — the trace model doesn't isolate reconciliation specifically from other rejection causes; not all rejections indicate a problem).",
        )

        val balanceMismatches = report.accounts.filter { it.finalBalanceMinor != it.liveBalanceMinor }
        checks += HealthCheck(
            "balance_reconstruction",
            balanceMismatches.isEmpty(),
            if (balanceMismatches.isEmpty()) "Every account's independently-replayed balance matches what AccountBalanceService reports live."
            else "${balanceMismatches.size} account(s) where the independent replay diverges from the live balance: ${balanceMismatches.map { it.accountId }}",
        )

        checks += HealthCheck(
            "cross_account_liability_contribution",
            report.crossAccountContributions.isEmpty(),
            if (report.crossAccountContributions.isEmpty()) "No payment transaction matched more than one liability account."
            else "${report.crossAccountContributions.size} payment(s) matched more than one account: " +
                report.crossAccountContributions.joinToString { "txn#${it.paymentTransactionId} -> ${it.matchedAccountIds}" },
        )

        checks += HealthCheck(
            "impossible_balance_growth",
            report.impossibleGrowthEvents.isEmpty(),
            if (report.impossibleGrowthEvents.isEmpty()) "No single transaction's effect exceeded its own amount."
            else "${report.impossibleGrowthEvents.size} transaction(s) with an effect exceeding their own amount — definitive evidence of a computation bug: " +
                report.impossibleGrowthEvents.joinToString { "txn#${it.transactionId} amount=${it.transactionAmountMinor} effect=${it.computedEffectMinor}" },
        )

        checks += HealthCheck(
            "type_conflicts",
            report.typeConflicts.isEmpty(),
            if (report.typeConflicts.isEmpty()) "No account's declared type conflicts with its observed transaction pattern."
            else "${report.typeConflicts.size} account(s) with a type/behavior mismatch: " +
                report.typeConflicts.joinToString { "${it.accountName}: ${it.conflictDescription}" },
        )

        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(DatabaseHealthReport(checks)))
    }
}




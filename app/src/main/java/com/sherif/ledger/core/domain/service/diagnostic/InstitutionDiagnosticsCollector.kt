package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class UnknownInstitutionEntry(
    val candidateAccountId: Long,
    val candidateAccountName: String,
    val currency: String,
    val transactionCount: Int,
    val balanceMinor: Long,
)

@Serializable
data class InstitutionDiagnosticsDto(
    /** RC7 Phase B: every Candidate Account — an institution InstitutionRegistry never recognized. Empty is the healthy state. */
    val unknownInstitutions: List<UnknownInstitutionEntry>,
    /**
     * Approximation, not an exact resolver-decision count: AccountIdentityResult
     * is transient (never persisted per-transaction), so this proxies "fallback
     * usage" as "transactions on the default account" — the resolver only binds
     * to a DIFFERENTLY-named, institution-matched account, so this is accurate
     * in the common case but not a precise replay of every historical decision.
     */
    val transactionsOnDefaultAccount: Long,
    val totalTransactions: Long,
    val defaultAccountFallbackRatePercent: Int,
    /** Transactions whose accountId matches no existing account at all — should always be empty; a real data-integrity check, not a fabricated zero. */
    val orphanTransactionIds: List<Long>,
    /** Count of transaction-account pairs where BalanceCalculator.effect() contributed zero due to a currency mismatch (RC6/RC7 guard actually firing). */
    val currencyMismatchCount: Int,
    val parserFailureTrackingNote: String,
)

/**
 * RC7 Phase F: surfaces exactly what Phase A-C exist to eventually drive to
 * zero — unknown institutions, default-account fallback usage, orphaned
 * transactions, and currency mismatches the guard actually caught. Computed
 * from data already persisted; no new instrumentation added to the live
 * capture path for this collector specifically (see parserFailureTrackingNote
 * for the one gap that would require touching the hot path).
 */
class InstitutionDiagnosticsCollector @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val accountBalanceService: AccountBalanceService,
    private val ensureDefaultAccountUseCase: EnsureDefaultAccountUseCase,
    private val financialTraceCollector: FinancialTraceCollector,
) : DiagnosticCollector {

    override val id: String = "institution_diagnostics"

    override suspend fun collect(): DiagnosticSection {
        val accounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val allTransactions = (transactionRepository.observeAllTransactions().first() as? LedgerResult.Success)?.data ?: emptyList()
        val accountIds = accounts.map { it.id }.toSet()
        val defaultAccountId = ensureDefaultAccountUseCase.execute()

        val candidateBalances = accountBalanceService.candidateBalances()
        val byCandidateAccount = allTransactions.groupBy { it.accountId }
        val unknownInstitutions = candidateBalances.map {
            UnknownInstitutionEntry(
                candidateAccountId = it.account.id,
                candidateAccountName = it.account.name,
                currency = it.balance.currencyCode.name,
                transactionCount = byCandidateAccount[it.account.id].orEmpty().size,
                balanceMinor = it.balance.minorUnits,
            )
        }

        val transactionsOnDefault = allTransactions.count { it.accountId == defaultAccountId }.toLong()
        val total = allTransactions.size.toLong()
        val fallbackRate = if (total == 0L) 0 else ((transactionsOnDefault * 100) / total).toInt()

        val orphanIds = allTransactions.filter { it.accountId !in accountIds }.map { it.id }

        val report = financialTraceCollector.buildReport()
        val currencyMismatchCount = report.transactionContributions.count { !it.included }

        val dto = InstitutionDiagnosticsDto(
            unknownInstitutions = unknownInstitutions,
            transactionsOnDefaultAccount = transactionsOnDefault,
            totalTransactions = total,
            defaultAccountFallbackRatePercent = fallbackRate,
            orphanTransactionIds = orphanIds,
            currencyMismatchCount = currencyMismatchCount,
            parserFailureTrackingNote = "Not tracked in this version — ParseResult.Failed outcomes are not currently recorded to any structured sink; would require instrumenting ParserRegistry/ExtractionRegistry on the live capture path, not attempted this RC.",
        )
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(dto))
    }
}

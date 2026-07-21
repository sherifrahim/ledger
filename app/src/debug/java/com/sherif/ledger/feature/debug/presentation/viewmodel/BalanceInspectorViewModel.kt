package com.sherif.ledger.feature.debug.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.service.diagnostic.FinancialTraceCollector
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.account.DismissCandidateAccountUseCase
import com.sherif.ledger.core.domain.usecase.account.PromoteCandidateAccountUseCase
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryRow(val label: String, val balanceMinor: Long, val isLiability: Boolean)

data class IncludedAccountRow(
    val id: Long,
    val name: String,
    val type: String,
    val balanceMinor: Long,
    val runningTotalMinor: Long,
)

data class ExcludedAccountRow(val id: Long, val name: String, val type: String, val reason: String)

/** RC7 Phase B/D: an unrecognized-institution account awaiting a user's decision. */
data class CandidateAccountRow(
    val id: Long,
    val name: String,
    val type: String,
    val currencyCode: CurrencyCode,
    val balanceMinor: Long,
)

/** RC7 Phase C: a real account balance excluded from the primary net-worth figure because its currency differs. */
data class NonPrimaryCurrencyRow(val id: Long, val name: String, val currencyCode: CurrencyCode, val balanceMinor: Long)

data class BalanceInspectorUiState(
    val loading: Boolean = true,
    val currencyCode: CurrencyCode = CurrencyCode.AED,
    /** What GetFinancialAnalyticsUseCase.computeNetWorth() returns — i.e. exactly what the Dashboard displays. */
    val dashboardDisplayedMinor: Long = 0,
    /** What FinancialTraceCollector's independent replay computes — same underlying service, re-presented with full transparency. */
    val calculatedTotalMinor: Long = 0,
    val categories: List<CategoryRow> = emptyList(),
    val includedAccounts: List<IncludedAccountRow> = emptyList(),
    val excludedAccounts: List<ExcludedAccountRow> = emptyList(),
    /** RC7 Phase B: unresolved-institution accounts, reviewable/actionable here — never silently merged, never silently included. */
    val candidateAccounts: List<CandidateAccountRow> = emptyList(),
    /** RC7 Phase C: real balances in a non-primary currency, excluded from dashboardDisplayedMinor/calculatedTotalMinor — never converted, never mixed in. */
    val nonPrimaryCurrencyAccounts: List<NonPrimaryCurrencyRow> = emptyList(),
    /** Concepts the spec's example lists but which have no backing data model yet — shown honestly as "not tracked," never a fabricated zero. */
    val untrackedLabels: List<String> = listOf("Pending Transactions", "Hidden Accounts (beyond soft-deleted)", "Ignored Accounts"),
) {
    val mismatchMinor: Long get() = dashboardDisplayedMinor - calculatedTotalMinor
}

/**
 * RC5 Part 2/3: a permanent Developer Console page that explains every AED
 * in the Dashboard's Financial State. Deliberately does NOT recompute
 * anything — it calls the exact same [FinancialTraceCollector.buildReport]
 * (RC4, already the single balance-replay implementation used by the
 * exported diagnostic bundle) and the exact same
 * [GetFinancialAnalyticsUseCase.computeNetWorth] the Dashboard itself calls.
 * If those two ever disagree, [BalanceInspectorUiState.mismatchMinor] is the
 * proof, not a guess.
 */
@HiltViewModel
class BalanceInspectorViewModel @Inject constructor(
    private val financialTraceCollector: FinancialTraceCollector,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
    private val accountBalanceService: AccountBalanceService,
    private val promoteCandidateAccountUseCase: PromoteCandidateAccountUseCase,
    private val dismissCandidateAccountUseCase: DismissCandidateAccountUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(BalanceInspectorUiState())
    val state: StateFlow<BalanceInspectorUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)

            val report = financialTraceCollector.buildReport()
            val netWorth = getFinancialAnalyticsUseCase.computeNetWorth()
            val candidateBalances = accountBalanceService.candidateBalances()
            val nonPrimaryCurrencyBalances = accountBalanceService.nonPrimaryCurrencyBalances()

            val categories = netWorth.accountBalances
                .groupBy { it.accountType }
                .map { (type, accounts) ->
                    CategoryRow(
                        label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        balanceMinor = accounts.sumOf { it.balanceMinor },
                        isLiability = type.isLiability,
                    )
                }
                .sortedBy { it.label }

            var running = 0L
            val includedAccounts = report.accounts.sortedBy { it.accountId }.map { a ->
                running += a.finalBalanceMinor
                IncludedAccountRow(
                    id = a.accountId,
                    name = a.accountName,
                    type = a.accountType,
                    balanceMinor = a.finalBalanceMinor,
                    runningTotalMinor = running,
                )
            }

            val excludedAccounts = report.excludedAccounts.map {
                ExcludedAccountRow(it.accountId, it.accountName, it.accountType, it.reason)
            }

            val candidateAccounts = candidateBalances.map {
                CandidateAccountRow(
                    id = it.account.id,
                    name = it.account.name,
                    type = it.account.type.name,
                    currencyCode = it.balance.currencyCode,
                    balanceMinor = it.balance.minorUnits,
                )
            }

            val nonPrimaryCurrencyAccounts = nonPrimaryCurrencyBalances.map {
                NonPrimaryCurrencyRow(it.account.id, it.account.name, it.balance.currencyCode, it.balance.minorUnits)
            }

            _state.value = BalanceInspectorUiState(
                loading = false,
                currencyCode = netWorth.currency,
                dashboardDisplayedMinor = netWorth.netWorthMinor,
                calculatedTotalMinor = report.netWorthMinor,
                categories = categories,
                includedAccounts = includedAccounts,
                excludedAccounts = excludedAccounts,
                candidateAccounts = candidateAccounts,
                nonPrimaryCurrencyAccounts = nonPrimaryCurrencyAccounts,
            )
        }
    }

    /** RC7 Phase D: a user, not the resolver, decides this account is real. Developer-Console-only action. */
    fun promoteCandidate(accountId: Long) {
        viewModelScope.launch {
            promoteCandidateAccountUseCase.execute(accountId)
            refresh()
        }
    }

    /** RC7 Phase D: a user decides this candidate is noise (e.g. a one-off promo message), not a real account. */
    fun dismissCandidate(accountId: Long) {
        viewModelScope.launch {
            dismissCandidateAccountUseCase.execute(accountId)
            refresh()
        }
    }
}

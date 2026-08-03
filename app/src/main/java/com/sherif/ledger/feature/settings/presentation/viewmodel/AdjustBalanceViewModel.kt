package com.sherif.ledger.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.account.SeedOpeningBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class AdjustBalanceAccountUi(
    val accountId: Long,
    val accountName: String,
    val computedBalanceMinor: Long,
    val currencyCode: CurrencyCode,
    /** What the account is assumed to have held before Ledger's tracking window. */
    val openingBalanceMinor: Long,
    /** Net of everything Ledger captured since it started tracking (computed - opening). */
    val capturedNetMinor: Long,
    /** Epoch millis the opening balance is anchored to (per-account), or null if never corrected. */
    val openingBalanceAsOfMillis: Long? = null,
    /** True for a credit card / loan, where the meaningful question is the limit, not a balance. */
    val isLiability: Boolean = false,
    /** The card's total credit limit, once the user has given it. */
    val creditLimitMinor: Long? = null,
)

/**
 * Standalone, reachable-anytime version of the same correction the onboarding
 * "Confirm Starting Balance" step offers — see SeedOpeningBalanceUseCase.
 *
 * Also exposes a plain-language **reconciliation** of each account's balance
 * (opening + captured = current) and the window Ledger tracked, so the figure is
 * explainable rather than a black box: a user can see that, e.g., the opening
 * balance is 0 and the captured net is what's driving a surprising number.
 */
@HiltViewModel
class AdjustBalanceViewModel @Inject constructor(
    private val accountBalanceService: AccountBalanceService,
    private val seedOpeningBalanceUseCase: SeedOpeningBalanceUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountRepository: com.sherif.ledger.core.domain.repository.AccountRepository,
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<AdjustBalanceAccountUi>>(emptyList())
    val accounts: StateFlow<List<AdjustBalanceAccountUi>> = _accounts.asStateFlow()

    private val _trackedSince = MutableStateFlow<String?>(null)
    /** e.g. "This Month · since 21 Jun 2026", or null if no import has run. */
    val trackedSince: StateFlow<String?> = _trackedSince.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _saved.value = false

            val summary = userPreferencesRepository.importSummary.first()
            _trackedSince.value = if (summary.windowStartMillis > 0L) {
                val date = Instant.ofEpochMilli(summary.windowStartMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                val label = summary.windowLabel.ifBlank { "your import" }
                "$label · since $date"
            } else {
                null
            }

            val balances = accountBalanceService.currentBalances()
            _accounts.value = balances.map {
                val opening = it.account.openingBalance.minorUnits
                AdjustBalanceAccountUi(
                    accountId = it.account.id,
                    accountName = it.account.name,
                    computedBalanceMinor = it.balance.minorUnits,
                    currencyCode = it.balance.currencyCode,
                    openingBalanceMinor = opening,
                    capturedNetMinor = it.balance.minorUnits - opening,
                    openingBalanceAsOfMillis = it.account.openingBalanceAsOf?.toEpochMilli(),
                    isLiability = it.account.type.isLiability,
                    creditLimitMinor = it.account.creditLimitMinor,
                )
            }
        }
    }

    /**
     * Records each card's total credit limit.
     *
     * This is the one number a card's outstanding balance cannot be derived
     * without: the bank restates the REMAINING limit in every message but never
     * the total. It is also a number the user actually knows and that essentially
     * never changes — unlike the card *balance* the onboarding step used to ask
     * for, which the user does not know offhand and which is stale the moment the
     * next purchase lands.
     */
    fun applyCreditLimits(limitsMinor: Map<Long, Long>) {
        viewModelScope.launch {
            limitsMinor.forEach { (accountId, limitMinor) ->
                val existing = accountRepository.getAccountById(accountId)
                if (existing is com.sherif.ledger.core.domain.model.LedgerResult.Success) {
                    accountRepository.updateAccount(existing.data.copy(creditLimitMinor = limitMinor))
                }
            }
            _saved.value = true
            refresh()
        }
    }

    /** [actualBalancesMinor] maps accountId -> user-entered real balance; an account absent from the map is left uncorrected. */
    fun applyCorrections(actualBalancesMinor: Map<Long, Long>) {
        viewModelScope.launch {
            actualBalancesMinor.forEach { (accountId, actualMinor) ->
                seedOpeningBalanceUseCase.execute(accountId, actualMinor)
            }
            _saved.value = true
            refresh()
        }
    }
}

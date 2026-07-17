package com.sherif.ledger.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.account.SeedOpeningBalanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdjustBalanceAccountUi(
    val accountId: Long,
    val accountName: String,
    val computedBalanceMinor: Long,
    val currencyCode: CurrencyCode,
)

/**
 * Standalone, reachable-anytime version of the same correction the
 * onboarding "Confirm Starting Balance" step offers — see
 * SeedOpeningBalanceUseCase for why this exists at all. The onboarding step
 * only ever runs once per install; this exists so a balance can still be
 * corrected afterward (or the first time, if onboarding already ran before
 * this existed) without needing to reinstall the app.
 */
@HiltViewModel
class AdjustBalanceViewModel @Inject constructor(
    private val accountBalanceService: AccountBalanceService,
    private val seedOpeningBalanceUseCase: SeedOpeningBalanceUseCase,
) : ViewModel() {

    private val _accounts = MutableStateFlow<List<AdjustBalanceAccountUi>>(emptyList())
    val accounts: StateFlow<List<AdjustBalanceAccountUi>> = _accounts.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _saved.value = false
            val balances = accountBalanceService.currentBalances()
            _accounts.value = balances.map {
                AdjustBalanceAccountUi(
                    accountId = it.account.id,
                    accountName = it.account.name,
                    computedBalanceMinor = it.balance.minorUnits,
                    currencyCode = it.balance.currencyCode,
                )
            }
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

package com.sherif.ledger.feature.accounts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.feature.accounts.presentation.AccountSectionUi
import com.sherif.ledger.feature.accounts.presentation.AccountUi
import com.sherif.ledger.feature.accounts.presentation.AccountsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = accountRepository.observeAllAccounts()
        .map { result ->
            if (result is LedgerResult.Success) {
                val accounts = result.data.map { account ->
                    AccountUi(
                        id = account.id.toString(),
                        name = account.name,
                        subtitle = account.type.name,
                        balance = account.balance.minorUnits.toString(),
                        isNegative = account.balance.minorUnits < 0
                    )
                }
                
                val total = result.data.sumOf { it.balance.minorUnits }.toString()

                AccountsUiState(
                    netWorth = total,
                    netWorthCurrency = "AED",
                    assetsTotal = total,
                    liabilitiesTotal = "0",
                    sections = if (accounts.isNotEmpty()) listOf(
                        AccountSectionUi("My Accounts", total, accounts)
                    ) else emptyList()
                )
            } else {
                EMPTY_STATE
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EMPTY_STATE
        )

    companion object {
        private val EMPTY_STATE = AccountsUiState(
            netWorth = "0",
            netWorthCurrency = "AED",
            assetsTotal = "0",
            liabilitiesTotal = "0",
            sections = emptyList()
        )
    }
}

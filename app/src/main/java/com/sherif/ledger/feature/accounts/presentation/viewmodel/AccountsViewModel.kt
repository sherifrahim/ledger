package com.sherif.ledger.feature.accounts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.accounts.presentation.AccountSectionUi
import com.sherif.ledger.feature.accounts.presentation.AccountUi
import com.sherif.ledger.feature.accounts.presentation.AccountsUiState
import com.sherif.ledger.presentation.dashboard.InsightUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.observeAllAccounts(),
        transactionRepository.observeRecentTransactions(10)
    ) { accountsResult, txnsResult ->
        if (accountsResult is LedgerResult.Success) {
            val accounts = accountsResult.data.map { account ->
                AccountUi(
                    id = account.id.toString(),
                    name = account.name,
                    subtitle = account.type.name,
                    balance = com.sherif.ledger.core.domain.util.MoneyFormatter.format(account.balance, includeSymbol = false),
                    isNegative = account.balance.minorUnits < 0
                )
            }
            
            val primaryCurrency = accountsResult.data.firstOrNull()?.balance?.currencyCode ?: com.sherif.ledger.core.domain.model.CurrencyCode.AED
            val assetsUnits = accountsResult.data.filter { it.balance.minorUnits >= 0 }.sumOf { it.balance.minorUnits }
            val liabilitiesUnits = accountsResult.data.filter { it.balance.minorUnits < 0 }.sumOf { it.balance.minorUnits }
            val netWorthUnits = assetsUnits + liabilitiesUnits
            
            val txnsCount = (txnsResult as? LedgerResult.Success)?.data?.size ?: 0
            val insight = if (txnsCount >= 5) {
                InsightUiModel(
                    title = "Monthly Spending",
                    subtitle = "Spending is stable compared to last week",
                    indicator = "→ 0%"
                )
            } else null

            AccountsUiState(
                netWorth = com.sherif.ledger.core.domain.util.MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(netWorthUnits, primaryCurrency), includeSymbol = false),
                netWorthCurrency = primaryCurrency.name,
                assetsTotal = com.sherif.ledger.core.domain.util.MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(assetsUnits, primaryCurrency), includeSymbol = false),
                liabilitiesTotal = com.sherif.ledger.core.domain.util.MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(liabilitiesUnits, primaryCurrency), includeSymbol = false),
                sections = if (accounts.isNotEmpty()) listOf(
                    AccountSectionUi("My Accounts", com.sherif.ledger.core.domain.util.MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(netWorthUnits, primaryCurrency), includeSymbol = false), accounts)
                ) else emptyList(),
                insight = insight
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
            sections = emptyList(),
            insight = null
        )
    }
}

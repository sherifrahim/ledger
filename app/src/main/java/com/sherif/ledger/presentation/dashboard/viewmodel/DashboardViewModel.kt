package com.sherif.ledger.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.presentation.dashboard.DashboardUiState
import com.sherif.ledger.presentation.dashboard.TransactionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeRecentTransactions(10),
        accountRepository.observeAllAccounts()
    ) { transactionsResult, accountsResult ->
        
        val recentTransactions = if (transactionsResult is LedgerResult.Success) {
            transactionsResult.data.map { txn ->
                TransactionUiModel(
                    merchant = txn.rawText ?: "Unknown",
                    category = "Other",
                    amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                    isExpense = txn.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE
                )
            }
        } else emptyList()

        val accounts = (accountsResult as? LedgerResult.Success)?.data ?: emptyList()
        val totalBalanceUnits = accounts.sumOf { it.balance.minorUnits }
        val primaryCurrency = accounts.firstOrNull()?.balance?.currencyCode ?: com.sherif.ledger.core.domain.model.CurrencyCode.AED
        
        val transactions = (transactionsResult as? LedgerResult.Success)?.data ?: emptyList()
        val incomeUnits = transactions.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME }.sumOf { it.amount.minorUnits }
        val expenseUnits = transactions.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
        val savingsUnits = incomeUnits - expenseUnits

        DashboardUiState(
            greeting = "Good morning",
            userName = "",
            currentMonth = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
            totalSpent = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(expenseUnits, primaryCurrency), includeSymbol = false),
            balanceAmount = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(totalBalanceUnits, primaryCurrency), includeSymbol = false),
            balanceCurrency = primaryCurrency.name,
            budgetProgress = 0f,
            expense = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(expenseUnits, primaryCurrency), includeSymbol = true),
            income = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(incomeUnits, primaryCurrency), includeSymbol = true),
            savings = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(savingsUnits, primaryCurrency), includeSymbol = true),
            recentTransactions = recentTransactions,
            insights = emptyList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EMPTY_STATE
    )

    companion object {
        private val EMPTY_STATE = DashboardUiState(
            greeting = "",
            userName = "",
            currentMonth = "",
            totalSpent = "0.00",
            budgetProgress = 0f,
            expense = "0.00",
            income = "0.00",
            savings = "0.00",
            recentTransactions = emptyList(),
            insights = emptyList()
        )
    }
}

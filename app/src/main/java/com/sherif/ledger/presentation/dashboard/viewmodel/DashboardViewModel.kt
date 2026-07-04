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

    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: DashboardViewModel")
    }

    private val currentMonthRange = run {
        val now = java.time.ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999).toInstant()
        start to end
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeRecentTransactions(10),
        transactionRepository.observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second),
        accountRepository.observeAllAccounts()
    ) { recentResult, monthResult, accountsResult ->
        
        val recentTransactions = if (recentResult is LedgerResult.Success) {
            recentResult.data.map { txn ->
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
        
        val monthTransactions = (monthResult as? LedgerResult.Success)?.data ?: emptyList()
        val incomeUnits = monthTransactions.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME }.sumOf { it.amount.minorUnits }
        val expenseUnits = monthTransactions.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
        val savingsUnits = incomeUnits - expenseUnits

        DashboardUiState(
            greeting = "Welcome back",
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
        ).also { 
            com.sherif.ledger.core.common.logging.LedgerLogger.d("DashboardViewModel: EMITTING uiState. TotalSpent=${it.totalSpent}, Balance=${it.balanceAmount}, RecentCount=${it.recentTransactions.size}")
        }
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

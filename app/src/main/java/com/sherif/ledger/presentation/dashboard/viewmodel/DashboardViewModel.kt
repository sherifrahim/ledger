package com.sherif.ledger.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
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
                    category = "Other", // Resolution handled by pipeline
                    amount = txn.amount.minorUnits.toString(), // TODO: Real formatting
                    isExpense = txn.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE
                )
            }
        } else emptyList()

        val totalBalance = if (accountsResult is LedgerResult.Success) {
            accountsResult.data.sumOf { it.balance.minorUnits }
        } else 0L

        DashboardUiState(
            greeting = "Good morning",
            userName = "",
            currentMonth = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(),
            totalSpent = "0.00", // TODO: Stats engine integration
            balanceAmount = totalBalance.toString(),
            budgetProgress = 0f,
            expense = "0.00",
            income = "0.00",
            savings = "0.00",
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

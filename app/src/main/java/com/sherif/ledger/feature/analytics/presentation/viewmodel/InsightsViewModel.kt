package com.sherif.ledger.feature.analytics.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.analytics.presentation.InsightsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> = transactionRepository.observeRecentTransactions(100)
        .map { result ->
            if (result is LedgerResult.Success && result.data.isNotEmpty()) {
                val expenseUnits = result.data.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE }
                    .sumOf { it.amount.minorUnits }
                
                val primaryCurrency = result.data.firstOrNull()?.amount?.currencyCode ?: com.sherif.ledger.core.domain.model.CurrencyCode.AED

                InsightsUiState(
                    totalSpent = com.sherif.ledger.core.domain.util.MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(expenseUnits, primaryCurrency), includeSymbol = false),
                    dateRange = "This Month",
                    percentChange = "Insufficient data for trends",
                    categories = emptyList(),
                    currency = primaryCurrency.name
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
        private val EMPTY_STATE = InsightsUiState(
            totalSpent = "0.00",
            dateRange = "This Month",
            percentChange = "0% vs last month",
            categories = emptyList()
        )
    }
}

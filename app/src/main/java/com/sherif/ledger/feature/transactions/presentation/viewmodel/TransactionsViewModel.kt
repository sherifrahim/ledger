package com.sherif.ledger.feature.transactions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.transactions.presentation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val uiState: StateFlow<TransactionsUiState> = transactionRepository.observeRecentTransactions(100)
        .map { result ->
            if (result is LedgerResult.Success) {
                val groups = result.data.groupBy { 
                    it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                }.map { (date, txns) ->
                    val transactionUiModels = txns.map { txn ->
                        TransactionUi(
                            id = txn.id.toString(),
                            merchant = txn.rawText ?: "Unknown",
                            category = MerchantCategory.Grocery, // Placeholder
                            amount = BigDecimal.valueOf(txn.amount.minorUnits).movePointLeft(2),
                            subtitle = txn.source.name
                        )
                    }

                    TransactionGroupUi(
                        id = date.toString(),
                        title = formatDate(date),
                        summary = DaySummaryUi(
                            spent = BigDecimal.ZERO, // TODO
                            income = BigDecimal.ZERO,
                            transactionCount = txns.size,
                            dominantCategory = MerchantCategory.Grocery
                        ),
                        transactions = transactionUiModels
                    )
                }
                TransactionsUiState(groups)
            } else {
                TransactionsUiState(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransactionsUiState(emptyList())
        )

    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }
}

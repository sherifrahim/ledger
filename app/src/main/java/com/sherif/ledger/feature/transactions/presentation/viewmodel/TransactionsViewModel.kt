package com.sherif.ledger.feature.transactions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.transactions.presentation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: TransactionsViewModel")
    }

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
                            amount = MoneyFormatter.format(txn.amount),
                            subtitle = txn.source.name
                        )
                    }

                    val firstTxn = txns.first()
                    val primaryCurrency = firstTxn.amount.currencyCode
                    val expenseUnits = txns.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
                    val incomeUnits = txns.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME }.sumOf { it.amount.minorUnits }

                    TransactionGroupUi(
                        id = date.toString(),
                        title = formatDate(date),
                        summary = DaySummaryUi(
                            spent = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(expenseUnits, primaryCurrency)),
                            income = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(incomeUnits, primaryCurrency)),
                            transactionCount = txns.size,
                            dominantCategory = MerchantCategory.Grocery
                        ),
                        transactions = transactionUiModels
                    )
                }
                TransactionsUiState(groups).also {
                    val count = it.groups.sumOf { g -> g.transactions.size }
                    com.sherif.ledger.core.common.logging.LedgerLogger.d("TransactionsViewModel: EMITTING uiState with $count transactions in ${it.groups.size} groups")
                    it.groups.firstOrNull()?.transactions?.firstOrNull()?.let { t ->
                        com.sherif.ledger.core.common.logging.LedgerLogger.d("Latest Txn in emission: ID=${t.id}, Merchant=${t.merchant}, Amount=${t.amount}")
                    }
                }
            } else {
                TransactionsUiState(emptyList()).also {
                    com.sherif.ledger.core.common.logging.LedgerLogger.d("TransactionsViewModel: EMITTING EMPTY_STATE")
                }
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

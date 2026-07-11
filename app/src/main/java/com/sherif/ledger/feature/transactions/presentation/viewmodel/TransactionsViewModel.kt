package com.sherif.ledger.feature.transactions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
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
    private val transactionRepository: TransactionRepository,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
) : ViewModel() {

    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: TransactionsViewModel")
    }

    val uiState: StateFlow<TransactionsUiState> = transactionRepository.observeRecentTransactions(100)
        .map { result ->
            if (result is LedgerResult.Success) {
                // Single relationship-analysis pass over the whole fetched list, so
                // per-day subtotals below are consistent with the monthly aggregate
                // shown elsewhere — never a second, independent aggregation.
                val effectiveSpend = getFinancialAnalyticsUseCase.effectiveSpendByTransactionId(result.data)

                val groups = result.data.groupBy { 
                    it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                }.map { (date, txns) ->
                    val transactionUiModels = txns.map { txn ->
                        TransactionUi(
                            id = txn.id.toString(),
                            merchant = txn.rawText ?: "Unknown",
                            category = categoryFor(txn),
                            amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                            subtitle = txn.source.name
                        )
                    }

                    val firstTxn = txns.first()
                    val primaryCurrency = firstTxn.amount.currencyCode
                    // Real spend: excludes credit-card payments / cash withdrawals,
                    // nets matched refunds. Never raw EXPENSE-type sum.
                    val expenseUnits = txns.sumOf { effectiveSpend[it.id] ?: 0L }
                    val incomeUnits = txns.filter { it.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME }.sumOf { it.amount.minorUnits }

                    TransactionGroupUi(
                        id = date.toString(),
                        title = formatDate(date),
                        summary = DaySummaryUi(
                            spent = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(expenseUnits, primaryCurrency)),
                            income = MoneyFormatter.format(com.sherif.ledger.core.domain.model.Money(incomeUnits, primaryCurrency)),
                            transactionCount = txns.size,
                            dominantCategory = if (incomeUnits > 0 && expenseUnits == 0L) MerchantCategory.Salary else MerchantCategory.Shopping
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

    private fun categoryFor(txn: com.sherif.ledger.core.domain.model.Transaction): MerchantCategory {
        val name = (txn.rawText ?: "").uppercase()
        return when {
            txn.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME -> MerchantCategory.Salary
            "COSTA" in name -> MerchantCategory.Coffee
            "CARREFOUR" in name -> MerchantCategory.Grocery
            "AMAZON" in name -> MerchantCategory.Shopping
            else -> MerchantCategory.Shopping
        }
    }

}



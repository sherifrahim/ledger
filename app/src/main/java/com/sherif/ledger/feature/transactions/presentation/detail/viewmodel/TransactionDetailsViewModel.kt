package com.sherif.ledger.feature.transactions.presentation.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val merchantRepository: MerchantRepository
) : ViewModel() {

    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: TransactionDetailsViewModel")
    }

    private val transactionId: String? = savedStateHandle["transactionId"]

    private val _uiState = MutableStateFlow<TransactionDetailsUiState?>(null)
    val uiState: StateFlow<TransactionDetailsUiState?> = _uiState.asStateFlow()

    init {
        loadTransactionDetails()
    }

    private fun loadTransactionDetails() {
        val id = transactionId?.toLongOrNull() ?: return
        com.sherif.ledger.core.common.logging.LedgerLogger.d("TransactionDetailsViewModel.loadTransactionDetails(id=$id)")
        viewModelScope.launch {
            val transactionResult = transactionRepository.getTransactionById(id)
            com.sherif.ledger.core.common.logging.LedgerLogger.d("TransactionDetailsViewModel: transactionResult=$transactionResult")
            if (transactionResult is LedgerResult.Success) {
                val txn = transactionResult.data
                val accountResult = accountRepository.getAccountById(txn.accountId)
                val account = (accountResult as? LedgerResult.Success)?.data
                
                val brandName = txn.brandId?.let { id ->
                    val brandResult = merchantRepository.getAllBrands()
                    if (brandResult is LedgerResult.Success) {
                        brandResult.data.find { it.id == id }?.name
                    } else null
                } ?: txn.rawText ?: "Unknown"

                // Prefer the transaction's own captured card tail as the payment mode.
                // Falls back to the account name when no tail was captured (older rows / non-card sources).
                val cardTail = txn.cardTail?.takeIf { it.isNotBlank() }
                val isCredit = txn.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME
                val resolvedPaymentMethod = when {
                    cardTail != null && isCredit -> "Account •••• $cardTail"
                    cardTail != null -> "•••• $cardTail"
                    else -> account?.name ?: "Unknown"
                }

                val zonedDateTime = txn.timestamp.atZone(ZoneId.systemDefault())
                val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                _uiState.update {
                    TransactionDetailsUiState(
                        merchant = brandName,
                        merchantCategory = "Other",
                        merchantAccentHue = 0xFF8A8A8A,
                        amount = com.sherif.ledger.core.domain.util.MoneyFormatter.format(txn.amount, includeSymbol = false),
                        sign = if (txn.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE) "-" else "+",
                        isIncome = txn.type == com.sherif.ledger.core.domain.model.TransactionType.INCOME,
                        date = zonedDateTime.format(dateFormatter),
                        time = zonedDateTime.format(timeFormatter),
                        status = "Completed",
                        paymentMethod = resolvedPaymentMethod,
                        accountName = account?.name ?: "Unknown",
                        accountNumber = cardTail ?: account?.accountNumberTail ?: "",
                        reference = txn.fingerprint.take(8).uppercase(),
                        history = emptyList(),
                        notes = txn.rawText
                    ).also { state ->
                        com.sherif.ledger.core.common.logging.LedgerLogger.d("TransactionDetailsViewModel: EMITTING uiState. Merchant=${state.merchant}, Amount=${state.amount}")
                    }
                }
            }
        }
    }
}

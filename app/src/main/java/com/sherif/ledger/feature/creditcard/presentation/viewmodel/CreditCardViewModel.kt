package com.sherif.ledger.feature.creditcard.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.merchantOrRawText
import com.sherif.ledger.core.domain.usecase.creditcard.GetCreditCardDetailsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CreditCardTransactionUi(
    val id: Long,
    val merchant: String,
    val amount: String,
    val date: String,
    val isExpense: Boolean,
)

data class CreditCardUiState(
    val isLoading: Boolean = true,
    val found: Boolean = true,
    val cardName: String = "",
    val cardTail: String? = null,
    val currency: String = "AED",
    val outstanding: String = "0",
    val limit: String? = null,
    val available: String? = null,
    val monthSpend: String = "0",
    val transactions: List<CreditCardTransactionUi> = emptyList(),
)

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCreditCardDetailsUseCase: GetCreditCardDetailsUseCase,
) : ViewModel() {

    private val accountId: Long? = savedStateHandle.get<String>("accountId")?.toLongOrNull()

    private val _uiState = MutableStateFlow(CreditCardUiState())
    val uiState: StateFlow<CreditCardUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        val id = accountId
        if (id == null) {
            _uiState.value = CreditCardUiState(isLoading = false, found = false)
            return
        }
        viewModelScope.launch {
            val details = getCreditCardDetailsUseCase.execute(id)
            if (details == null) {
                _uiState.value = CreditCardUiState(isLoading = false, found = false)
                return@launch
            }
            val formatter = DateTimeFormatter.ofPattern("d MMM")
            val zone = ZoneId.systemDefault()
            _uiState.value = CreditCardUiState(
                isLoading = false,
                found = true,
                cardName = details.account.name,
                cardTail = details.account.accountNumberTail,
                currency = details.currency.name,
                outstanding = MoneyFormatter.format(Money(details.outstandingMinor, details.currency), includeSymbol = false),
                limit = details.limitMinor?.let { MoneyFormatter.format(Money(it, details.currency), includeSymbol = false) },
                available = details.availableMinor?.let { MoneyFormatter.format(Money(it, details.currency), includeSymbol = false) },
                monthSpend = MoneyFormatter.format(Money(details.monthSpendMinor, details.currency), includeSymbol = false),
                transactions = details.transactions.map { txn ->
                    CreditCardTransactionUi(
                        id = txn.id,
                        merchant = txn.merchantOrRawText ?: "Transaction",
                        amount = MoneyFormatter.format(txn.amount, includeSymbol = false),
                        date = txn.timestamp.atZone(zone).format(formatter),
                        isExpense = txn.type == com.sherif.ledger.core.domain.model.TransactionType.EXPENSE,
                    )
                },
            )
        }
    }
}

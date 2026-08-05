package com.sherif.ledger.feature.creditcard.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.merchantOrRawText
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.creditcard.GetCreditCardDetailsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
    /** outstanding / limit, 0f..1f — null when the limit hasn't been set. Kept as
     *  a raw fraction (not re-derived from the formatted strings) so the bar and
     *  its colour are exact rather than parsed back out of display text. */
    val utilization: Float? = null,
    val transactions: List<CreditCardTransactionUi> = emptyList(),
)

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val transactionReadSource: TransactionReadSource,
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
            // Triggers only — observed so a credit limit entered on Adjust Balance
            // (a different screen, same account row) reflects here on return
            // without the user needing to back out and re-enter this screen.
            combine(
                accountRepository.observeAllAccounts(),
                transactionReadSource.observeAllTransactions(),
            ) { _, _ -> getCreditCardDetailsUseCase.execute(id) }
                .flowOn(Dispatchers.Default)
                .collect { details ->
                    if (details == null) {
                        _uiState.value = CreditCardUiState(isLoading = false, found = false)
                        return@collect
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
                        utilization = details.limitMinor?.takeIf { it > 0 }
                            ?.let { (details.outstandingMinor.toFloat() / it.toFloat()).coerceIn(0f, 1f) },
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
}

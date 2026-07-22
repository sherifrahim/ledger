package com.sherif.ledger.feature.transactions.presentation.entry.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ManualAccountOption(
    val id: Long,
    val name: String,
    val currency: CurrencyCode,
)

/**
 * Manual transaction entry (v1.2). Lets the user record something the automated
 * SMS/notification capture missed — cash, a transfer, anything without an alert —
 * so the balance stays complete. Goes through the SAME [InsertTransactionUseCase]
 * write path as captured transactions (validation, fingerprint, FinancialEvent
 * mirror), just with `source = MANUAL`. No balance is written directly; the
 * persisted transaction is the whole effect, replayed like every other.
 */
@HiltViewModel
class ManualEntryViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val insertTransactionUseCase: InsertTransactionUseCase,
) : ViewModel() {

    val accounts: StateFlow<List<ManualAccountOption>> = accountRepository.observeAllAccounts()
        .map { result ->
            (result as? LedgerResult.Success)?.data
                ?.map { ManualAccountOption(it.id, it.name, it.openingBalance.currencyCode) }
                ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun save(
        accountId: Long,
        amountMinor: Long,
        currency: CurrencyCode,
        type: TransactionType,
        timestamp: Instant,
        description: String,
    ) {
        if (_saving.value) return
        _saving.value = true
        _error.value = null
        viewModelScope.launch {
            val result = insertTransactionUseCase.execute(
                InsertTransactionUseCase.Params(
                    accountId = accountId,
                    amountMinor = amountMinor,
                    currencyCode = currency,
                    type = type,
                    timestamp = timestamp,
                    source = IngestionSource.MANUAL,
                    rawMerchantText = description.ifBlank { "Manual entry" },
                ),
            )
            _saving.value = false
            when (result) {
                is LedgerResult.Success -> _saved.value = true
                is LedgerResult.Failure -> _error.value = "Couldn't save. Check the amount and try again."
            }
        }
    }
}

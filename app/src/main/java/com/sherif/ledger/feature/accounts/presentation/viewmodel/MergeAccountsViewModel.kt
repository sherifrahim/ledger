package com.sherif.ledger.feature.accounts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.account.MergeAccountsResult
import com.sherif.ledger.core.domain.usecase.account.MergeAccountsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MergeAccountOptionUi(
    val id: Long,
    val name: String,
    val subtitle: String,
    val balance: String,
)

data class MergeAccountsUiState(
    val accounts: List<MergeAccountOptionUi> = emptyList(),
    val keepAccountId: Long? = null,
    val mergeAccountId: Long? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val merged: Boolean = false,
) {
    val canConfirm: Boolean
        get() = keepAccountId != null && mergeAccountId != null && keepAccountId != mergeAccountId && !isSubmitting
}

/**
 * ACCOUNT_IDENTITY_PLAN Steps 4-5's screen: the explicit, user-driven fix for
 * accounts that already split before (or despite) the resolver's own
 * duplicate-prevention (Steps 1-3). Never triggered automatically.
 */
@HiltViewModel
class MergeAccountsViewModel @Inject constructor(
    private val accountBalanceService: AccountBalanceService,
    private val mergeAccountsUseCase: MergeAccountsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergeAccountsUiState())
    val uiState: StateFlow<MergeAccountsUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val options = accountBalanceService.currentBalances()
                .sortedBy { it.account.name }
                .map { balance ->
                    MergeAccountOptionUi(
                        id = balance.account.id,
                        name = balance.account.name,
                        subtitle = listOfNotNull(
                            balance.account.type.name.lowercase().replaceFirstChar { it.uppercase() },
                            balance.account.accountNumberTail?.let { "···$it" },
                        ).joinToString(" "),
                        balance = MoneyFormatter.format(balance.balance, includeSymbol = true),
                    )
                }
            _uiState.value = _uiState.value.copy(accounts = options)
        }
    }

    fun selectKeep(accountId: Long) {
        _uiState.value = _uiState.value.copy(keepAccountId = accountId, errorMessage = null)
    }

    fun selectMerge(accountId: Long) {
        _uiState.value = _uiState.value.copy(mergeAccountId = accountId, errorMessage = null)
    }

    fun confirmMerge() {
        val state = _uiState.value
        val keep = state.keepAccountId ?: return
        val merge = state.mergeAccountId ?: return
        if (keep == merge) return

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = mergeAccountsUseCase.execute(keep, merge)) {
                is MergeAccountsResult.Success ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false, merged = true)
                is MergeAccountsResult.Failed ->
                    _uiState.value = _uiState.value.copy(isSubmitting = false, errorMessage = result.reason)
            }
        }
    }
}

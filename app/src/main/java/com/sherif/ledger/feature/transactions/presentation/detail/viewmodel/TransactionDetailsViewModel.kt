package com.sherif.ledger.feature.transactions.presentation.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.MerchantRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.transactions.presentation.detail.MerchantHistoryItem
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * RC1: category comes from [GetFinancialAnalyticsUseCase.transactionStories]
 * (real Merchant Intelligence) instead of a hardcoded "Other". Sign respects
 * [Transaction.transferDirection] for TRANSFER-typed transactions instead of
 * defaulting every non-EXPENSE to "+". Amount is fully formatted via
 * [MoneyFormatter] (with symbol) so the screen never reconstructs a currency
 * symbol itself. History is real — other transactions sharing the same brand —
 * reusing the existing [TransactionRepository.observeAllTransactions] rather
 * than adding new repository surface.
 *
 * Feature audit (Split & Notes): notes come from the real, persisted
 * [Transaction.note] column — never the raw captured message text — and
 * [updateNote] writes through [TransactionRepository.updateNote], the same
 * path the notification's inline "Add Note" reply uses.
 */
@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val merchantRepository: MerchantRepository,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
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

                val brandName = txn.brandId?.let { brandId ->
                    val brandResult = merchantRepository.getAllBrands()
                    if (brandResult is LedgerResult.Success) {
                        brandResult.data.find { it.id == brandId }?.name
                    } else null
                } ?: txn.rawText ?: "Unknown"

                // Real category, from the same Merchant Intelligence resolution
                // every other screen uses — never a hardcoded placeholder.
                val story = getFinancialAnalyticsUseCase.transactionStories(listOf(txn))[txn.id]
                val merchantCategory = story?.category
                    ?.lowercase()
                    ?.replaceFirstChar { it.uppercase() }
                    ?: "Other"

                // Prefer the transaction's own captured card tail as the payment mode.
                // Falls back to the account name when no tail was captured (older rows / non-card sources).
                val cardTail = txn.cardTail?.takeIf { it.isNotBlank() }
                val isCredit = txn.type == TransactionType.INCOME
                val resolvedPaymentMethod = when {
                    cardTail != null && isCredit -> "Account •••• $cardTail"
                    cardTail != null -> "•••• $cardTail"
                    else -> account?.name ?: "Unknown"
                }

                // Sign reflects the real direction of money movement. EXPENSE and
                // an OUTGOING transfer both reduce the account; INCOME, REFUND,
                // and an INCOMING transfer both increase it. A TRANSFER with no
                // normalized direction shows neither sign, matching
                // BalanceCalculator's own "don't guess" rule for that gap.
                val sign = when (txn.type) {
                    TransactionType.EXPENSE -> "-"
                    TransactionType.INCOME, TransactionType.REFUND -> "+"
                    TransactionType.TRANSFER -> when (txn.transferDirection) {
                        TransferDirection.OUTGOING -> "-"
                        TransferDirection.INCOMING -> "+"
                        null -> ""
                    }
                }
                val isIncomeDisplay = sign == "+"

                val zonedDateTime = txn.timestamp.atZone(ZoneId.systemDefault())
                val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                // Real merchant history: other transactions sharing this brand,
                // most recent first, this one excluded. Empty (not fabricated)
                // when there is no brand or no other transactions.
                val history = if (txn.brandId != null) {
                    val allResult = transactionRepository.observeAllTransactions().first()
                    val all = (allResult as? LedgerResult.Success)?.data ?: emptyList()
                    all.filter { it.brandId == txn.brandId && it.id != txn.id }
                        .sortedByDescending { it.timestamp }
                        .take(5)
                        .map {
                            MerchantHistoryItem(
                                amount = MoneyFormatter.format(it.amount, includeSymbol = true),
                                date = it.timestamp.atZone(ZoneId.systemDefault()).format(dateFormatter),
                                isIncome = it.type == TransactionType.INCOME || it.type == TransactionType.REFUND,
                            )
                        }
                } else emptyList()

                _uiState.update {
                    TransactionDetailsUiState(
                        merchant = brandName,
                        merchantCategory = merchantCategory,
                        merchantAccentHue = 0xFF8A8A8A,
                        amount = MoneyFormatter.format(txn.amount, includeSymbol = true),
                        sign = sign,
                        isIncome = isIncomeDisplay,
                        date = zonedDateTime.format(dateFormatter),
                        time = zonedDateTime.format(timeFormatter),
                        status = "Completed",
                        paymentMethod = resolvedPaymentMethod,
                        accountName = account?.name ?: "Unknown",
                        accountNumber = cardTail ?: account?.accountNumberTail ?: "",
                        reference = txn.fingerprint.take(8).uppercase(),
                        history = history,
                        notes = txn.note
                    ).also { state ->
                        com.sherif.ledger.core.common.logging.LedgerLogger.d("TransactionDetailsViewModel: EMITTING uiState. Merchant=${state.merchant}, Amount=${state.amount}")
                    }
                }
            }
        }
    }

    /** Sets or clears this transaction's note, then reloads state so the screen
     *  reflects the change. Passing blank/empty clears the note. */
    fun updateNote(text: String) {
        val id = transactionId?.toLongOrNull() ?: return
        viewModelScope.launch {
            transactionRepository.updateNote(id, text.trim().ifBlank { null })
            loadTransactionDetails()
        }
    }
}



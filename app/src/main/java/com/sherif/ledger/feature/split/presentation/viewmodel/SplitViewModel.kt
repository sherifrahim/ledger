package com.sherif.ledger.feature.split.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.SplitType
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.repository.ParticipantRepository
import com.sherif.ledger.core.domain.repository.ShareInput
import com.sherif.ledger.core.domain.repository.SplitRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.split.presentation.SplitParticipantUi
import com.sherif.ledger.feature.split.presentation.SplitUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ledger Split (v1) — wires the existing, fully-built Split backend
 * ([SplitRepository]/[ParticipantRepository]/SplitCalculator) to the UI. Isolated
 * from Financial Truth: reads the transaction's amount as the total to divide,
 * never touches a balance. V1 does EQUAL splits (you + the people you pick);
 * settle/unsettle each share; remove to redo.
 */
@HiltViewModel
class SplitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val participantRepository: ParticipantRepository,
    private val splitRepository: SplitRepository,
) : ViewModel() {

    private val transactionId: Long = savedStateHandle.get<String>("transactionId")?.toLongOrNull() ?: -1L

    private val transaction = MutableStateFlow<Transaction?>(null)
    private val selectedIds = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            participantRepository.getOrCreateSelf() // ensure self exists for the divisor
            (transactionRepository.getTransactionById(transactionId) as? LedgerResult.Success)?.let {
                transaction.value = it.data
            }
        }
    }

    val uiState: StateFlow<SplitUiState> = combine(
        transaction,
        participantRepository.observeAll(),
        splitRepository.observeSplitForTransaction(transactionId),
        selectedIds,
    ) { txn, participantsResult, splitResult, selected ->
        if (txn == null) return@combine SplitUiState(loading = true)

        val total = txn.amount.minorUnits
        val currency = txn.amount.currencyCode
        val merchant = txn.rawText ?: "Transaction"
        val split = (splitResult as? LedgerResult.Success)?.data

        if (split != null) {
            // A split exists — show each person's owed share to settle.
            val rows = split.shares.map { swp ->
                SplitParticipantUi(
                    id = swp.participant.id,
                    name = swp.participant.name,
                    isSelf = swp.participant.isSelf,
                    selected = true,
                    shareId = swp.share.id,
                    shareMinor = swp.share.shareAmountMinor,
                    settled = swp.share.isSettled,
                )
            }
            SplitUiState(
                loading = false,
                merchant = merchant,
                totalMinor = total,
                currency = currency,
                participants = rows,
                hasSplit = true,
                splitId = split.split.id,
                yourShareMinor = total - split.totalOwedMinor,
                outstandingMinor = split.outstandingMinor,
            )
        } else {
            // No split yet — the picker. Non-self participants the user can include.
            val all = (participantsResult as? LedgerResult.Success)?.data ?: emptyList()
            val others = all.filter { !it.isSelf }
            val perShare = if (selected.isEmpty()) 0L else total / (selected.size + 1)
            val rows = others.map {
                SplitParticipantUi(
                    id = it.id,
                    name = it.name,
                    isSelf = false,
                    selected = it.id in selected,
                    shareMinor = if (it.id in selected) perShare else 0L,
                )
            }
            SplitUiState(
                loading = false,
                merchant = merchant,
                totalMinor = total,
                currency = currency,
                participants = rows,
                hasSplit = false,
                yourShareMinor = if (selected.isEmpty()) total else total - perShare * selected.size,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SplitUiState())

    fun toggleParticipant(id: String) {
        selectedIds.value = if (id in selectedIds.value) selectedIds.value - id else selectedIds.value + id
    }

    fun addParticipant(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = participantRepository.createParticipant(name.trim())
            if (result is LedgerResult.Success) {
                selectedIds.value = selectedIds.value + result.data.id // auto-include a just-added person
            }
        }
    }

    fun createEqualSplit() {
        val selected = selectedIds.value
        if (selected.isEmpty()) return
        viewModelScope.launch {
            splitRepository.createSplit(
                transactionId = transactionId,
                splitType = SplitType.EQUAL,
                participantShares = selected.associateWith { ShareInput.Auto },
            )
        }
    }

    fun setSettled(shareId: String, settled: Boolean) {
        viewModelScope.launch { splitRepository.markSettled(shareId, settled) }
    }

    fun removeSplit() {
        val id = uiState.value.splitId ?: return
        viewModelScope.launch {
            splitRepository.deleteSplit(id)
            selectedIds.value = emptySet()
        }
    }
}

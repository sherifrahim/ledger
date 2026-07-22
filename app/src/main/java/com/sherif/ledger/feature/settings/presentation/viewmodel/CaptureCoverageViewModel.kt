package com.sherif.ledger.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.ImportSummary
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Surfaces what the last historical import actually did — the honest answer to
 * "did Ledger silently ignore anything?". Every figure is the real persisted
 * [ImportSummary]; `null` until an import has run. This is the user-facing
 * version of the same coverage the Developer Console shows, so a user can see
 * (not just a developer) how many messages were scanned, captured, and skipped.
 */
@HiltViewModel
class CaptureCoverageViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val summary: StateFlow<ImportSummary?> = userPreferencesRepository.importSummary
        .map { s -> if (s.smsScanned > 0L || s.transactionsCreated > 0L) s else null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )
}

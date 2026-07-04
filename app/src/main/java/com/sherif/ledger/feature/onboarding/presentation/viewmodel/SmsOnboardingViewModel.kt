package com.sherif.ledger.feature.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.feature.capture.sms.SmsImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsOnboardingViewModel @Inject constructor(
    private val smsImporter: SmsImporter,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun startImport(onComplete: () -> Unit) {
        viewModelScope.launch {
            com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsOnboardingViewModel: SMS Permission Granted. Starting Import.")
            _isImporting.value = true
            smsImporter.importHistoricalSms()
            userPreferencesRepository.setSmsImported(true)
            _isImporting.value = false
            com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsOnboardingViewModel: Import Completed.")
            onComplete()
        }
    }

    fun skipImport(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setSmsImported(true) // Mark as "handled"
            onComplete()
        }
    }
}

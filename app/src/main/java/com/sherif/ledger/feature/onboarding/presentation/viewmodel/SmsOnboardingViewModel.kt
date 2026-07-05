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

    // null = not run yet; >=0 = count found; -1 = error
    private val _importResult = MutableStateFlow<Int?>(null)
    val importResult: StateFlow<Int?> = _importResult.asStateFlow()

    fun startImport() {
        viewModelScope.launch {
            com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsOnboardingViewModel: SMS Permission Granted. Starting Import.")
            _isImporting.value = true
            try {
                val result = smsImporter.importHistoricalSms()
                userPreferencesRepository.setSmsImported(true)
                _importResult.value = result.found
                com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsOnboardingViewModel: Import Completed. Found=${result.found}")
            } catch (e: Exception) {
                userPreferencesRepository.setSmsImported(true)
                _importResult.value = -1
                com.sherif.ledger.core.common.logging.LedgerLogger.e("SmsOnboardingViewModel: Import Failed", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun skipImport(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setSmsImported(true) // Mark as "handled"
            onComplete()
        }
    }
}

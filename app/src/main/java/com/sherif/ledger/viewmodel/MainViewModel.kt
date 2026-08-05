package com.sherif.ledger.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.core.designsystem.theme.LedgerThemeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val isSmsImported: StateFlow<Boolean> = userPreferencesRepository.isSmsImported
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val isNotificationAccessSkipped: StateFlow<Boolean> = userPreferencesRepository.isNotificationAccessSkipped
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    fun skipNotificationAccess() {
        viewModelScope.launch { userPreferencesRepository.setNotificationAccessSkipped(true) }
    }

    val isProfileSetup: StateFlow<Boolean> = userPreferencesRepository.isProfileSetup
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val themeType: StateFlow<LedgerThemeType> = userPreferencesRepository.themeType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LedgerThemeType.Dark
        )

    val liquidGlass: StateFlow<Boolean> = userPreferencesRepository.isLiquidGlassEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}

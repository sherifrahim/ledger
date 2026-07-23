package com.sherif.ledger.feature.settings.presentation.viewmodel

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
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val themeType: StateFlow<LedgerThemeType> = userPreferencesRepository.themeType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LedgerThemeType.Classic
        )

    val liquidGlass: StateFlow<Boolean> = userPreferencesRepository.isLiquidGlassEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setThemeType(themeType: LedgerThemeType) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeType(themeType)
        }
    }

    fun setLiquidGlass(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setLiquidGlassEnabled(enabled)
        }
    }
}

package com.sherif.ledger.feature.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Same ViewModel backs both first-launch setup (fields start blank) and
 * Settings' "Edit Profile" (fields start pre-filled with whatever was saved
 * before) — reusing one screen/ViewModel rather than duplicating the form.
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    init {
        viewModelScope.launch {
            _name.value = userPreferencesRepository.userName.first()
            _email.value = userPreferencesRepository.userEmail.first()
        }
    }

    fun setName(value: String) { _name.value = value }
    fun setEmail(value: String) { _email.value = value }

    /** Deliberately minimal: non-blank name, email containing "@" — no server to validate against, nothing more to check. */
    fun canContinue(): Boolean =
        _name.value.isNotBlank() && _email.value.contains("@") && _email.value.trim().length > 2

    fun saveAndContinue(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setUserProfile(_name.value.trim(), _email.value.trim())
            onComplete()
        }
    }
}

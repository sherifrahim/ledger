package com.sherif.ledger.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class UserProfileUiState(
    val name: String = "",
    val email: String = "",
    val initials: String = "",
)

/**
 * Single source for the locally-collected user profile (name/email/derived
 * initials) — shared by every screen that shows it (Dashboard's avatar,
 * Profile's header), so there's exactly one place that reads
 * [UserPreferencesRepository]'s profile fields and one derivation of
 * initials, not a copy per screen.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<UserProfileUiState> = combine(
        userPreferencesRepository.userName,
        userPreferencesRepository.userEmail,
    ) { name, email ->
        UserProfileUiState(name = name, email = email, initials = initialsFrom(name))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfileUiState(),
    )

    private fun initialsFrom(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> ""
            words.size == 1 -> words[0].take(1).uppercase()
            else -> (words.first().take(1) + words.last().take(1)).uppercase()
        }
    }
}

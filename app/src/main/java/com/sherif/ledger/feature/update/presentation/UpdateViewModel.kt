package com.sherif.ledger.feature.update.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.feature.update.CheckForUpdateUseCase
import com.sherif.ledger.feature.update.UpdateDownloader
import com.sherif.ledger.feature.update.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val checking: Boolean = false,
    val hasChecked: Boolean = false,
    val available: UpdateInfo? = null,
    val downloading: Boolean = false,
    val downloadFailed: Boolean = false,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val updateDownloader: UpdateDownloader,
) : ViewModel() {

    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun checkNow() {
        if (_state.value.checking) return
        _state.value = _state.value.copy(checking = true)
        viewModelScope.launch {
            val result = checkForUpdateUseCase.execute()
            _state.value = _state.value.copy(checking = false, hasChecked = true, available = result)
        }
    }

    fun downloadAndInstall() {
        val update = _state.value.available ?: return
        _state.value = _state.value.copy(downloading = true, downloadFailed = false)
        viewModelScope.launch {
            val ok = updateDownloader.downloadAndInstall(update)
            _state.value = _state.value.copy(downloading = false, downloadFailed = !ok)
        }
    }

    fun dismiss() {
        _state.value = UpdateUiState()
    }
}

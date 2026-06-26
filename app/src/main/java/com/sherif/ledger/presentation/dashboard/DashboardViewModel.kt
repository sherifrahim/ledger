package com.sherif.ledger.presentation.dashboard

import androidx.lifecycle.ViewModel
import com.sherif.ledger.presentation.dashboard.preview.DashboardPreviewData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(

        DashboardPreviewData.state

    )

    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

}

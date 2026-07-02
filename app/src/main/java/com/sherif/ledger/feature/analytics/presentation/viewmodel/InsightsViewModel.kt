package com.sherif.ledger.feature.analytics.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.feature.analytics.presentation.InsightsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(EMPTY_STATE)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    companion object {
        private val EMPTY_STATE = InsightsUiState(
            totalSpent = "0.00",
            dateRange = "This Month",
            percentChange = "0% vs last month",
            categories = emptyList()
        )
    }
}

package com.sherif.ledger.feature.debug.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.feature.ai.cost.AiMetrics
import com.sherif.ledger.feature.ai.cost.AiMetricsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** RC6's "AI Metrics" — Developer Console only, all-time aggregates. See AiMetricsService for what's tracked and why percentages can be null. */
@HiltViewModel
class AiMetricsViewModel @Inject constructor(
    private val metricsService: AiMetricsService,
) : ViewModel() {

    private val _metrics = MutableStateFlow<AiMetrics?>(null)
    val metrics: StateFlow<AiMetrics?> = _metrics.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _metrics.value = metricsService.current() }
    }
}

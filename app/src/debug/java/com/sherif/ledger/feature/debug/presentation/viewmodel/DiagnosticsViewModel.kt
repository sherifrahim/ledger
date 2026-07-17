package com.sherif.ledger.feature.debug.presentation.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.common.logging.LedgerLogBuffer
import com.sherif.ledger.core.domain.service.diagnostic.DiagnosticBundleGenerator
import com.sherif.ledger.core.domain.service.diagnostic.DiagnosticBundleSharer
import com.sherif.ledger.core.domain.service.diagnostic.DiagnosticCollector
import com.sherif.ledger.core.domain.service.diagnostic.DiagnosticSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val availableCollectorIds: List<String> = emptyList(),
    val selectedCollectorId: String? = null,
    val selectedOutput: String? = null,
    val isRunningCollector: Boolean = false,
    val isGeneratingBundle: Boolean = false,
    val bundleShareIntent: Intent? = null,
    val liveLogCount: Int = 0,
)

/**
 * Deliberately generic rather than one bespoke screen per collector: a list
 * of collector IDs, "run" produces that collector's raw output, "Export
 * Bundle" runs every collector and packages the results. New collectors
 * (Split, Notes, Budgets, whatever's next) appear here automatically the
 * moment they're added to DiagnosticCollectorModule — this ViewModel and its
 * screen never need to change for that.
 *
 * Kept as its own ViewModel/screen rather than folded into the existing
 * DebugConsoleViewModel — that one is already working, already fixed once
 * this session (the PipelineTracker lazy-init bug), and has no need to carry
 * risk from a large, separate addition.
 */
@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val collectors: Set<@JvmSuppressWildcards DiagnosticCollector>,
    private val bundleGenerator: DiagnosticBundleGenerator,
    private val bundleSharer: DiagnosticBundleSharer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DiagnosticsUiState(availableCollectorIds = collectors.map { it.id }.sorted())
    )
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    fun refreshLiveLogCount() {
        _uiState.update { it.copy(liveLogCount = LedgerLogBuffer.recent().size) }
    }

    fun runCollector(id: String) {
        val collector = collectors.find { it.id == id } ?: return
        _uiState.update { it.copy(isRunningCollector = true, selectedCollectorId = id) }
        viewModelScope.launch {
            val output = try {
                when (val section = collector.collect()) {
                    is DiagnosticSection.Json -> section.json
                    is DiagnosticSection.LogText -> section.logText
                }
            } catch (e: Exception) {
                "Error running '$id': ${e.javaClass.simpleName}: ${e.message}"
            }
            _uiState.update { it.copy(isRunningCollector = false, selectedOutput = output) }
        }
    }

    fun exportBundle() {
        _uiState.update { it.copy(isGeneratingBundle = true) }
        viewModelScope.launch {
            val file = bundleGenerator.generateBundle()
            val intent = bundleSharer.buildShareIntent(file)
            _uiState.update { it.copy(isGeneratingBundle = false, bundleShareIntent = intent) }
        }
    }

    fun bundleShareIntentConsumed() {
        _uiState.update { it.copy(bundleShareIntent = null) }
    }

    fun clearLiveLogs() {
        LedgerLogBuffer.clear()
        refreshLiveLogCount()
    }
}




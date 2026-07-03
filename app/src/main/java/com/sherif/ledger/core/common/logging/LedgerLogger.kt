package com.sherif.ledger.core.common.logging

import android.util.Log

import com.sherif.ledger.core.common.diagnostics.PipelineEvent
import com.sherif.ledger.core.common.diagnostics.PipelineStage
import com.sherif.ledger.core.common.diagnostics.StageStatus

/**
 * Structured logger for Ledger.
 * Allows easy toggling of debug logs for production.
 */
object LedgerLogger {
    private const val TAG = "LedgerPipeline"
    var isEnabled = true
    
    // Bridge to structured diagnostics (Set by RealPipelineTracker in debug)
    var eventTracker: ((PipelineEvent) -> Unit)? = null

    private val _logs = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    val logs: kotlinx.coroutines.flow.StateFlow<List<String>> = _logs

    fun d(message: String) {
        if (isEnabled) {
            android.util.Log.d(TAG, message)
            _logs.value = (listOf(message) + _logs.value).take(100)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            android.util.Log.e(TAG, message, throwable)
            _logs.value = (listOf("ERROR: $message") + _logs.value).take(100)
        }
    }

    fun pipeline(stage: String, details: String) {
        d("[$stage] $details")
        
        // Map string-based stage to enum for diagnostics
        val enumStage = when (stage) {
            "Capture" -> PipelineStage.CAPTURE
            "Filter" -> PipelineStage.FILTER
            "Parser" -> PipelineStage.PARSER
            "Reconciliation" -> PipelineStage.RECONCILIATION
            "Persistence" -> PipelineStage.PERSISTENCE
            "Normalization" -> PipelineStage.NORMALIZATION
            else -> null
        }
        
        enumStage?.let {
            val status = when {
                "failed" in details.lowercase() || "error" in details.lowercase() -> 
                    StageStatus.Failed(details)
                "ignored" in details.lowercase() -> 
                    StageStatus.Ignored
                else -> 
                    StageStatus.SuccessWithDetails(details)
            }
            eventTracker?.invoke(PipelineEvent(it, status))
        }
    }
}

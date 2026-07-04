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
    
    // Diagnostic Trace Context (Transient)
    private val currentTraceId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun setTraceId(id: String?) { currentTraceId.value = id }
    fun getTraceId(): String? = currentTraceId.value

    // Bridge to structured diagnostics (Set by RealPipelineTracker in debug)
    var eventTracker: ((PipelineEvent) -> Unit)? = null

    private val _logs = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())
    val logs: kotlinx.coroutines.flow.StateFlow<List<String>> = _logs

    fun d(message: String) {
        if (isEnabled) {
            val traceId = getTraceId()
            val formattedMessage = if (traceId != null) "[$traceId] $message" else message
            try {
                android.util.Log.d(TAG, formattedMessage)
            } catch (e: Exception) {
                // Handle unit tests where android.util.Log is not available
                println("$TAG: $formattedMessage")
            }
            _logs.value = (listOf(formattedMessage) + _logs.value).take(100)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            val traceId = getTraceId()
            val prefix = if (traceId != null) "[$traceId] " else ""
            try {
                android.util.Log.e(TAG, "$prefix$message", throwable)
            } catch (e: Exception) {
                println("ERROR $TAG: $prefix$message")
            }
            _logs.value = (listOf("${prefix}ERROR: $message") + _logs.value).take(100)
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
            "UI" -> PipelineStage.UI_REFRESH
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
            eventTracker?.invoke(PipelineEvent(it, status, traceId = getTraceId()))
        }
    }
}

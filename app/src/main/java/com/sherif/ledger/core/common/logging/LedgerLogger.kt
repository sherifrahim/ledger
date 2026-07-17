package com.sherif.ledger.core.common.logging

import java.time.Instant

/**
 * Structured logger for Ledger.
 * Allows easy toggling of debug logs for production.
 *
 * RC4: every call now also records a [LogEntry] into [LedgerLogBuffer] — the
 * persistent-diagnostics half of the requirement, alongside the existing
 * Logcat write, which is unchanged. Both happen unconditionally together;
 * there is no path that logs to one and not the other.
 *
 * Removed the eventTracker bridge to the old core.common.diagnostics
 * PipelineEvent/PipelineTracker system: confirmed dead in RC3 (nothing has
 * injected PipelineTracker since DebugConsoleViewModel was moved onto
 * PipelineTraceSink), so it was silently doing nothing on every call. Not
 * deleting PipelineTracker.kt/RealPipelineTracker.kt/DiagnosticModule.kt
 * themselves — that would be the architectural clean-up RC4 explicitly asked
 * to avoid — only removing this call site now that building the real
 * persistent-logging path makes clear it has nowhere useful left to feed.
 */
object LedgerLogger {
    private const val TAG = "LedgerPipeline"
    var isEnabled = true

    // Diagnostic Trace Context (Transient)
    private val currentTraceId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun setTraceId(id: String?) { currentTraceId.value = id }
    fun getTraceId(): String? = currentTraceId.value

    // Retained purely so RealPipelineTracker.kt (app/src/debug/) still compiles
    // — its init{} block wires this. Confirmed dead as a functional path (RC3:
    // nothing has injected the old PipelineTracker interface since
    // DebugConsoleViewModel moved onto PipelineTraceSink), so pipeline() below
    // no longer invokes it. Not deleting RealPipelineTracker.kt itself, since
    // that would be the architectural clean-up RC4 asked to avoid.
    var eventTracker: ((com.sherif.ledger.core.common.diagnostics.PipelineEvent) -> Unit)? = null

    // Retained for the existing Developer Console "Logs" tab — unchanged shape,
    // still capped, so nothing there needs to change alongside this.
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
            LedgerLogBuffer.record(LogEntry(Instant.now(), LogLevel.DEBUG, TAG, formattedMessage))
        }
    }

    fun i(message: String) {
        if (isEnabled) {
            val traceId = getTraceId()
            val formattedMessage = if (traceId != null) "[$traceId] $message" else message
            try {
                android.util.Log.i(TAG, formattedMessage)
            } catch (e: Exception) {
                println("$TAG: $formattedMessage")
            }
            _logs.value = (listOf(formattedMessage) + _logs.value).take(100)
            LedgerLogBuffer.record(LogEntry(Instant.now(), LogLevel.INFO, TAG, formattedMessage))
        }
    }

    fun w(message: String) {
        if (isEnabled) {
            val traceId = getTraceId()
            val formattedMessage = if (traceId != null) "[$traceId] $message" else message
            try {
                android.util.Log.w(TAG, formattedMessage)
            } catch (e: Exception) {
                println("$TAG: $formattedMessage")
            }
            _logs.value = (listOf(formattedMessage) + _logs.value).take(100)
            LedgerLogBuffer.record(LogEntry(Instant.now(), LogLevel.WARN, TAG, formattedMessage))
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
            val fullMessage = if (throwable != null) "$prefix$message (${throwable.javaClass.simpleName}: ${throwable.message})" else "$prefix$message"
            LedgerLogBuffer.record(LogEntry(Instant.now(), LogLevel.ERROR, TAG, fullMessage))
        }
    }

    fun pipeline(stage: String, details: String) {
        d("[$stage] $details")
    }
}




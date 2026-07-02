package com.sherif.ledger.core.common.logging

import android.util.Log

/**
 * Structured logger for Ledger.
 * Allows easy toggling of debug logs for production.
 */
object LedgerLogger {
    private const val TAG = "LedgerPipeline"
    var isEnabled = true

    fun d(message: String) {
        if (isEnabled) Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (isEnabled) Log.e(TAG, message, throwable)
    }

    fun pipeline(stage: String, details: String) {
        d("[$stage] $details")
    }
}

package com.sherif.ledger.core.common.logging

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LogLevel(val marker: Char) {
    DEBUG('D'),
    INFO('I'),
    WARN('W'),
    ERROR('E'),
}

/**
 * One captured log line. Formatting mirrors `adb logcat`'s own default output
 * (`MM-dd HH:mm:ss.SSS LEVEL/TAG: message`) deliberately — the whole point of
 * this framework is that exported logs stay familiar to anyone who has ever
 * read real logcat output, not a bespoke format that needs learning.
 */
data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
    }

    fun toLogcatLine(): String {
        val time = FORMATTER.format(timestamp.atZone(ZoneId.systemDefault()))
        return "$time ${level.marker}/$tag: $message"
    }
}




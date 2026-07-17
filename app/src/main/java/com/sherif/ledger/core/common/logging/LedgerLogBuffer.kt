package com.sherif.ledger.core.common.logging

/**
 * Bounded, in-process ring buffer of every [LogEntry] LedgerLogger records —
 * the actual "persist locally" half of RC4's requirement, alongside the
 * existing Logcat write.
 *
 * Deliberately a plain object, not a Hilt-managed class. RC3 found a real bug
 * in exactly this shape: a lazily-created Hilt singleton (the old
 * PipelineTracker) whose bridge only wired the moment something first
 * injected it, silently missing everything captured before that first
 * injection. A plain object is live from the moment the process starts —
 * there is no "first injection" to wait for, so there is nothing to miss.
 *
 * Bounded to 10,000 entries per RC4's explicit requirement, to avoid
 * unlimited memory growth over a long-running process. Not written to disk
 * incrementally — this is the in-memory record for the current process
 * lifetime; [snapshotLogcatText] is what the diagnostic bundle exports.
 */
object LedgerLogBuffer {
    private const val MAX_ENTRIES = 10_000

    private val lock = Any()
    private val entries = ArrayDeque<LogEntry>(MAX_ENTRIES)

    fun record(entry: LogEntry) {
        synchronized(lock) {
            if (entries.size >= MAX_ENTRIES) entries.removeFirst()
            entries.addLast(entry)
        }
    }

    /** Snapshot, oldest first — the natural chronological reading order. */
    fun recent(limit: Int = MAX_ENTRIES): List<LogEntry> = synchronized(lock) {
        if (limit >= entries.size) entries.toList() else entries.toList().takeLast(limit)
    }

    fun clear() = synchronized(lock) { entries.clear() }

    /** The full buffer formatted as one logcat-style line per entry — this is
     *  exactly what becomes `ledger.log` in the exported diagnostic bundle. */
    fun snapshotLogcatText(): String = synchronized(lock) {
        entries.joinToString("\n") { it.toLogcatLine() }
    }
}




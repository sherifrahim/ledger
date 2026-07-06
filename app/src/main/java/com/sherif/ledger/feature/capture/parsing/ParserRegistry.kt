package com.sherif.ledger.feature.capture.parsing

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry responsible for managing parsers and delegating parsing requests.
 */
@Singleton
class ParserRegistry @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards BankParser>
) {
    /**
     * Finds a matching parser and attempts to extract financial facts.
     */
    fun parse(envelope: NotificationEnvelope): ParseResult {
        val candidates = parsers
            .filter { it.supports(envelope) }
            .sortedBy { it.priority }

        if (candidates.isEmpty()) {
            return ParseResult.Failed("No matching parser found for ${envelope.packageName}")
        }

        var lastResult: ParseResult = ParseResult.Failed("No parser produced a result.")
        for (parser in candidates) {
            when (val result = parser.parse(envelope)) {
                is ParseResult.Success -> return result
                is ParseResult.Ignore -> return result
                is ParseResult.Failed -> lastResult = result
            }
        }
        return lastResult
    }

    /**
     * Returns all registered parsers for inspection.
     */
    fun getParsers(): List<BankParser> = parsers.toList()
}

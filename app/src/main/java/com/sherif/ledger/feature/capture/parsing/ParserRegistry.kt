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
        val parser = parsers.find { it.supports(envelope) }
            ?: return ParseResult.Failed("No matching parser found for ${envelope.packageName}")
        
        return parser.parse(envelope)
    }

    /**
     * Returns all registered parsers for inspection.
     */
    fun getParsers(): List<BankParser> = parsers.toList()
}

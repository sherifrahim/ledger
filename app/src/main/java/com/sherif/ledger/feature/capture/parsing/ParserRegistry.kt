package com.sherif.ledger.feature.capture.parsing

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry responsible for selecting and delegating to the appropriate bank parser.
 */
@Singleton
class ParserRegistry @Inject constructor() {
    private val parsers = mutableListOf<BankParser>()

    /**
     * Registers a new bank parser into the registry.
     */
    fun registerParser(parser: BankParser) {
        parsers.add(parser)
    }

    /**
     * Returns all registered parsers.
     */
    fun getParsers(): List<BankParser> = parsers
}

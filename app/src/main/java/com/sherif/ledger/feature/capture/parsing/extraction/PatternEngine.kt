package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.NotificationPattern
import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.feature.capture.parsing.ParseResult
import javax.inject.Inject

/**
 * Engine responsible for executing sequential patterns against a notification envelope.
 */
class PatternEngine @Inject constructor(
    private val normalizer: TextNormalizer
) {
    /**
     * Executes the provided patterns until one matches.
     */
fun extract(envelope: NotificationEnvelope, patterns: List<NotificationPattern>): ParseResult {
    val normalizedText = normalizer.normalize("${envelope.title} ${envelope.text}")

    LedgerLogger.pipeline(
        "PatternEngine",
        "NORMALIZED='$normalizedText'"
    )

    for (pattern in patterns) {
        val matched = pattern.matches(normalizedText)

        LedgerLogger.pipeline(
            "PatternEngine",
            "${pattern.javaClass.simpleName} -> $matched"
        )

        if (matched) {
            LedgerLogger.pipeline(
                "PatternEngine",
                "SELECTED=${pattern.javaClass.simpleName}"
            )

            return pattern.extract(envelope, normalizedText)
        }
    }

    LedgerLogger.pipeline(
        "PatternEngine",
        "NO MATCH"
    )

    return ParseResult.Failed("No matching pattern found for notification.")
}
}

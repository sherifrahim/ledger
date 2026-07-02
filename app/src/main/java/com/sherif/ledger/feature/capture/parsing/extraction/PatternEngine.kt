package com.sherif.ledger.feature.capture.parsing.extraction

import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.NotificationPattern
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
        
        for (pattern in patterns) {
            if (pattern.matches(normalizedText)) {
                return pattern.extract(envelope, normalizedText)
            }
        }
        
        return ParseResult.Failed("No matching pattern found for notification.")
    }
}

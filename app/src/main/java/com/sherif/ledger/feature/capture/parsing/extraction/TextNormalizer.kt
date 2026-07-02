package com.sherif.ledger.feature.capture.parsing.extraction

import java.text.Normalizer
import javax.inject.Inject

/**
 * Preprocesses raw notification text for consistent extraction.
 */
class TextNormalizer @Inject constructor() {

    fun normalize(input: String): String {
        return input
            .replace("\n", " ") // Merge lines
            .let { Normalizer.normalize(it, Normalizer.Form.NFKC) } // Unicode normalization
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }
}

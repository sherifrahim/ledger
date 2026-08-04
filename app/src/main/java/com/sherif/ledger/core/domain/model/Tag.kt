package com.sherif.ledger.core.domain.model

/**
 * A user-authored label on a transaction.
 *
 * The only classification in Ledger that comes from the person rather than from
 * the data — see [com.sherif.ledger.core.database.entity.TagEntity] for why that
 * matters. Never inferred, never auto-assigned, never removed by any engine.
 */
data class Tag(
    val id: Long,
    val name: String,
) {
    companion object {
        /**
         * The form uniqueness is judged on: case- and whitespace-insensitive, so
         * "Dubai Trip", "dubai trip" and "Dubai  Trip" are one tag rather than
         * three that look identical in a list.
         */
        fun normalize(name: String): String =
            name.trim().lowercase().replace(Regex("\\s+"), " ")

        /** The longest a tag may be. Long enough for a real phrase, short enough to render as a chip. */
        const val MAX_LENGTH = 32

        /** Null when [raw] can never be a tag; otherwise the cleaned display form. */
        fun sanitize(raw: String): String? {
            val cleaned = raw.trim().replace(Regex("\\s+"), " ")
            if (cleaned.isEmpty()) return null
            return cleaned.take(MAX_LENGTH)
        }
    }
}

package com.sherif.ledger.core.model

/**
 * Immutable collection of transaction tags that guarantees uniqueness.
 */
class TransactionTags private constructor(
    val values: Set<TransactionTag>,
) {
    /** Returns true when the collection contains the given tag. */
    operator fun contains(tag: TransactionTag): Boolean = values.contains(tag)

    /** Adds a tag and returns a new immutable tag collection. */
    fun plus(tag: TransactionTag): TransactionTags = TransactionTags(values + tag)

    /** Removes a tag and returns a new immutable tag collection. */
    fun minus(tag: TransactionTag): TransactionTags = TransactionTags(values - tag)

    /** Returns true when no tags are present. */
    fun isEmpty(): Boolean = values.isEmpty()

    /** Compares tag collections by their contained values. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransactionTags) return false
        return values == other.values
    }

    /** Returns a hash code based on contained values. */
    override fun hashCode(): Int = values.hashCode()

    /** Returns a debug representation of contained tags. */
    override fun toString(): String = "TransactionTags(values=$values)"

    companion object {
        /** Empty immutable tag collection. */
        val Empty = TransactionTags(emptySet())

        /** Creates a tag collection and removes duplicate tags. */
        fun of(tags: Collection<TransactionTag>): TransactionTags = TransactionTags(tags.toSet())
    }
}

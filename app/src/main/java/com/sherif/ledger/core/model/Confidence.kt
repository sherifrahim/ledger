package com.sherif.ledger.core.model

/**
 * Strongly typed confidence score stored as basis points from 0 to 10,000.
 *
 * This avoids exposing floating point precision to transaction parsing or
 * review decisions.
 */
@JvmInline
value class Confidence private constructor(val basisPoints: Int) : Comparable<Confidence> {
    init {
        require(basisPoints in MIN_BASIS_POINTS..MAX_BASIS_POINTS) {
            "Confidence must be between 0 and 10,000 basis points."
        }
    }

    /** Returns this confidence as a whole percentage rounded down. */
    fun percent(): Int = basisPoints / BASIS_POINTS_PER_PERCENT

    /** Compares two confidence scores. */
    override fun compareTo(other: Confidence): Int = basisPoints.compareTo(other.basisPoints)

    companion object {
        private const val MIN_BASIS_POINTS = 0
        private const val MAX_BASIS_POINTS = 10_000
        private const val BASIS_POINTS_PER_PERCENT = 100

        /** Zero confidence. */
        val None = Confidence(MIN_BASIS_POINTS)

        /** Full confidence. */
        val Certain = Confidence(MAX_BASIS_POINTS)

        /** Creates a confidence score from basis points. */
        fun fromBasisPoints(basisPoints: Int): Confidence = Confidence(basisPoints)

        /** Creates a confidence score from an integer percentage from 0 to 100. */
        fun fromPercent(percent: Int): Confidence {
            require(percent in 0..100) { "Confidence percent must be between 0 and 100." }
            return Confidence(percent * BASIS_POINTS_PER_PERCENT)
        }
    }
}

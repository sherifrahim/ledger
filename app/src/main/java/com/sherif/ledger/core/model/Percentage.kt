package com.sherif.ledger.core.model

/**
 * A percentage stored as basis points (0 to 10000) to avoid floating point.
 *
 * Mirrors how [Confidence] stores its value. 100% = 10000 basis points.
 */
@JvmInline
value class Percentage(val basisPoints: Int) : Comparable<Percentage> {
    init { require(basisPoints in 0..10000) { "Percentage must be 0..10000 basis points." } }

    fun asFloat(): Float = basisPoints / 10000f
    fun display(): String = "${basisPoints / 100}%"

    override fun compareTo(other: Percentage): Int = basisPoints.compareTo(other.basisPoints)

    companion object {
        val Zero = Percentage(0)
        val Full = Percentage(10000)
        fun fromPercent(percent: Int): Percentage {
            require(percent in 0..100)
            return Percentage(percent * 100)
        }
    }
}

package com.sherif.ledger.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Provides time to domain code through an interface so tests can use fixed clocks.
 */
fun interface TimeProvider {
    /** Returns the current instant. */
    fun now(): Instant

    companion object {
        /** Creates a provider backed by the system clock. */
        fun system(): TimeProvider = TimeProvider { Instant.now() }

        /** Creates a provider that always returns the same instant. */
        fun fixed(instant: Instant): TimeProvider = TimeProvider { instant }
    }
}

/**
 * Returns the current local date for a provider and zone.
 */
fun TimeProvider.today(zoneId: ZoneId): LocalDate = now().atZone(zoneId).toLocalDate()

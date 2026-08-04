package com.sherif.ledger.presentation.dashboard

import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.service.intelligence.RecurrenceFrequency
import com.sherif.ledger.core.domain.service.intelligence.RecurringKind
import com.sherif.ledger.core.domain.service.intelligence.RecurringSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class UpcomingProjectionTest {

    private val now: Instant = Instant.parse("2026-08-04T09:00:00Z")

    private fun schedule(
        label: String,
        daysAhead: Long,
        confidence: Int = 90,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        amountMinor: Long = 5_000L,
    ) = RecurringSchedule(
        label = label,
        kind = RecurringKind.SUBSCRIPTION,
        frequency = frequency,
        averageAmountMinor = amountMinor,
        currencyCode = CurrencyCode.AED,
        lastOccurrence = now.minus(30, ChronoUnit.DAYS),
        nextExpectedDate = now.plus(daysAhead, ChronoUnit.DAYS),
        confidence = confidence,
        transactionIds = listOf(1L, 2L, 3L),
    )

    @Test
    fun `an irregular series is never projected`() {
        // The engine uses IRREGULAR to say it could not fit a cadence. Turning that
        // into a dated prediction would be inventing precision it explicitly denied.
        val result = UpcomingProjection.from(
            listOf(schedule("Odd one out", daysAhead = 3, frequency = RecurrenceFrequency.IRREGULAR)),
            now,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `the engine's own doubt is respected`() {
        val result = UpcomingProjection.from(
            listOf(schedule("Maybe Netflix", daysAhead = 3, confidence = UpcomingProjection.MIN_CONFIDENCE - 1)),
            now,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `a projection whose date has already passed is not upcoming`() {
        // Either the charge arrived — and is in the feed — or the series ended.
        val result = UpcomingProjection.from(listOf(schedule("Stale", daysAhead = -2)), now)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `the soonest charges come first and the list is capped`() {
        val result = UpcomingProjection.from(
            listOf(
                schedule("Far", daysAhead = 25),
                schedule("Soon", daysAhead = 1),
                schedule("Middle", daysAhead = 10),
                schedule("Furthest", daysAhead = 40),
            ),
            now,
        )

        assertEquals(UpcomingProjection.MAX_ITEMS, result.size)
        assertEquals(listOf("Soon", "Middle", "Far"), result.map { it.label })
    }

    @Test
    fun `due dates read as distances, not calendar dates`() {
        // A date implies the engine knows the merchant's billing calendar. It knows
        // the interval between past charges, which is a different thing.
        assertEquals("Tomorrow", UpcomingProjection.describeDue(now, now.plus(1, ChronoUnit.DAYS)))
        assertEquals("in 5 days", UpcomingProjection.describeDue(now, now.plus(5, ChronoUnit.DAYS)))
        assertEquals("next week", UpcomingProjection.describeDue(now, now.plus(9, ChronoUnit.DAYS)))
        assertEquals("in 3 weeks", UpcomingProjection.describeDue(now, now.plus(21, ChronoUnit.DAYS)))
    }

    @Test
    fun `cadence and confidence travel with the row`() {
        // Both are what stop this reading as a promise.
        val result = UpcomingProjection.from(listOf(schedule("Spotify", daysAhead = 2, confidence = 88)), now).single()

        assertEquals("Monthly", result.cadence)
        assertEquals(88, result.confidence)
    }

    @Test
    fun `no history means no section rather than an empty one`() {
        assertTrue(UpcomingProjection.from(emptyList(), now).isEmpty())
    }
}

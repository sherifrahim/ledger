package com.sherif.ledger.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * An inclusive date range used for filtering, analytics periods, and budgets.
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init { require(!end.isBefore(start)) { "DateRange end cannot be before start." } }

    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)

    companion object {
        fun singleDay(date: LocalDate): DateRange = DateRange(date, date)

        fun monthOf(date: LocalDate): DateRange = DateRange(
            start = date.withDayOfMonth(1),
            end = date.withDayOfMonth(date.lengthOfMonth()),
        )
    }
}

package com.sherif.ledger.feature.onboarding.presentation

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * First-launch historical-import window choices (Part 2). [CUSTOM] carries no
 * built-in range — the caller supplies explicit start/end dates via
 * [SmsOnboardingViewModel]'s custom-range state; [startInstant] here is only a
 * placeholder for the non-custom cases.
 */
enum class ImportRangeOption(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_12_MONTHS("Last 12 Months"),
    CUSTOM("Custom Date Range");

    fun startInstant(now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())): Instant = when (this) {
        THIS_WEEK -> now.minusWeeks(1).toInstant()
        THIS_MONTH -> now.minusMonths(1).toInstant()
        LAST_3_MONTHS -> now.minusMonths(3).toInstant()
        LAST_12_MONTHS -> now.minusMonths(12).toInstant()
        CUSTOM -> now.toInstant()
    }
}

/** The resolved, ready-to-import window — either a preset or a confirmed custom range. */
data class ResolvedImportRange(
    val label: String,
    val start: Instant,
    val end: Instant,
)

fun ImportRangeOption.resolve(
    customStart: LocalDate?,
    customEnd: LocalDate?,
    now: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault()),
): ResolvedImportRange {
    return if (this == ImportRangeOption.CUSTOM && customStart != null && customEnd != null) {
        ResolvedImportRange(
            label = label,
            start = customStart.atStartOfDay(now.zone).toInstant(),
            end = customEnd.plusDays(1).atStartOfDay(now.zone).toInstant(),
        )
    } else {
        ResolvedImportRange(label = label, start = startInstant(now), end = now.toInstant())
    }
}

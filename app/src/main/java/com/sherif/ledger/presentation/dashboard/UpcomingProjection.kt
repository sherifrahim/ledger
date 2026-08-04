package com.sherif.ledger.presentation.dashboard

import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.service.intelligence.RecurrenceFrequency
import com.sherif.ledger.core.domain.service.intelligence.RecurringSchedule
import com.sherif.ledger.core.domain.util.MoneyFormatter
import java.time.Duration
import java.time.Instant

/**
 * Turns the recurring engine's detected series into the handful of charges worth
 * showing on a dashboard.
 *
 * Separate from the ViewModel because the interesting part is judgement, not
 * plumbing: which projections are honest enough to show. A projection is an
 * assertion about the future, and this screen has spent its whole life refusing
 * to fabricate — so the bar is deliberately high and the three rejections below
 * are each about not over-claiming.
 */
object UpcomingProjection {

    /** Below this the engine is signalling doubt; the dashboard stays quiet. */
    const val MIN_CONFIDENCE = 60

    /** A glance surface, not a list screen. */
    const val MAX_ITEMS = 3

    fun from(schedules: List<RecurringSchedule>, now: Instant): List<UpcomingUiModel> =
        schedules
            // The engine labels a series it could not fit to a cadence as IRREGULAR.
            // Projecting a date from one is fiction dressed as a forecast.
            .filter { it.frequency != RecurrenceFrequency.IRREGULAR }
            // The engine's own confidence, respected rather than argued with.
            .filter { it.confidence >= MIN_CONFIDENCE }
            // A "next expected" date already in the past means the charge either
            // arrived — in which case it is in the feed, not upcoming — or the
            // series ended. Neither is something to predict.
            .filter { it.nextExpectedDate.isAfter(now) }
            .sortedBy { it.nextExpectedDate }
            .take(MAX_ITEMS)
            .map { schedule ->
                UpcomingUiModel(
                    id = schedule.transactionIds.joinToString("-").ifEmpty { schedule.label },
                    label = schedule.label,
                    amount = MoneyFormatter.format(
                        Money(schedule.averageAmountMinor, schedule.currencyCode), includeSymbol = true,
                    ),
                    dueLabel = describeDue(now, schedule.nextExpectedDate),
                    cadence = schedule.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    confidence = schedule.confidence,
                )
            }

    /**
     * "Tomorrow" / "in 5 days" rather than a calendar date.
     *
     * A date implies a precision the engine does not have: it knows the interval
     * between past occurrences, not the merchant's billing calendar. A distance
     * reads as the estimate it is.
     */
    internal fun describeDue(now: Instant, next: Instant): String {
        val days = Duration.between(now, next).toDays()
        return when {
            days <= 0L -> "Today"
            days == 1L -> "Tomorrow"
            days < 7L -> "in $days days"
            days < 14L -> "next week"
            else -> "in ${days / 7} weeks"
        }
    }
}

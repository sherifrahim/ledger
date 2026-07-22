package com.sherif.ledger.core.designsystem.component.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTimeTextStyle
import java.util.Locale

/**
 * A custom, LDL-styled month-grid range picker — no Material DatePicker/
 * DateRangePicker. Per AI_ENGINEERING_GUIDE.md, Material is a rendering
 * primitive only; Ledger's visual identity (tokens, motion, surfaces) must
 * come from LDL, not a borrowed platform picker.
 *
 * Tap sequence: first tap sets [rangeStart] (clears any prior end); a second
 * tap on or after that date sets [rangeEnd]; a tap before the current start
 * begins a new range instead of extending it. Dates after [maxDate] (default:
 * today — historical SMS import can never look into the future) are shown
 * but not selectable.
 */
@Composable
fun LedgerDateRangeCalendar(
    rangeStart: LocalDate?,
    rangeEnd: LocalDate?,
    onRangeChange: (start: LocalDate?, end: LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate = LocalDate.now(),
) {
    var visibleMonth by remember {
        mutableStateOf(YearMonth.from(rangeStart ?: rangeEnd ?: maxDate))
    }
    val colors = LedgerTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        // Month header + navigation.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerIconButton(
                icon = Icons.Filled.ChevronLeft,
                onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                contentDescription = "Previous month",
            )
            Text(
                text = "${visibleMonth.month.getDisplayName(JavaTimeTextStyle.FULL, Locale.getDefault())} ${visibleMonth.year}",
                style = LedgerTextStyles.Title,
                color = colors.textPrimary,
            )
            val nextDisabled = visibleMonth.plusMonths(1) > YearMonth.from(maxDate)
            LedgerIconButton(
                icon = Icons.Filled.ChevronRight,
                onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                enabled = !nextDisabled,
                contentDescription = "Next month",
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Small))

        // Weekday header, Monday-first.
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.values().sortedBy { (it.value + 6) % 7 }.forEach { dow ->
                Text(
                    text = dow.getDisplayName(JavaTimeTextStyle.NARROW, Locale.getDefault()),
                    style = LedgerTextStyles.Caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Tiny))

        val weeks = remember(visibleMonth) { monthGrid(visibleMonth) }
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        maxDate = maxDate,
                        onClick = { tapped ->
                            val (nextStart, nextEnd) = nextRange(rangeStart, rangeEnd, tapped)
                            onRangeChange(nextStart, nextEnd)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate?,
    rangeStart: LocalDate?,
    rangeEnd: LocalDate?,
    maxDate: LocalDate,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerTheme.colors
    if (day == null) {
        Box(modifier = modifier.aspectRatio(1f))
        return
    }

    val isFuture = day.isAfter(maxDate)
    val isEndpoint = day == rangeStart || day == rangeEnd
    val isInRange = rangeStart != null && rangeEnd != null && day.isAfter(rangeStart) && day.isBefore(rangeEnd)

    val background = when {
        isEndpoint -> colors.textPrimary
        isInRange -> colors.surfaceInset
        else -> Color.Transparent
    }
    val textColor = when {
        isFuture -> colors.textTertiary.copy(alpha = 0.4f)
        isEndpoint -> colors.surfaceBase
        else -> colors.textPrimary
    }
    val shape = if (isEndpoint) CircleShape else RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(background, shape)
            .then(if (!isFuture) Modifier.clickable { onClick(day) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = LedgerTextStyles.BodyMedium,
            color = textColor,
        )
    }
}

/** Monday-first 6-row grid for [month]; null cells pad days outside the month. */
private fun monthGrid(month: YearMonth): List<List<LocalDate?>> {
    val firstDay = month.atDay(1)
    val leadingBlanks = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val totalDays = month.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    repeat(leadingBlanks) { cells += null }
    for (d in 1..totalDays) cells += month.atDay(d)
    while (cells.size % 7 != 0) cells += null
    return cells.chunked(7)
}

/** Tap-sequence rule: new start if none, or if tapping before the current start; otherwise sets the end. */
private fun nextRange(start: LocalDate?, end: LocalDate?, tapped: LocalDate): Pair<LocalDate?, LocalDate?> {
    return when {
        start == null || end != null -> tapped to null
        tapped.isBefore(start) -> tapped to null
        else -> start to tapped
    }
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.haptics.LedgerHaptics
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * A slice of the interactive pie/donut. Money is pre-formatted by the caller into
 * [valueLabel] and the share into [percent] — the component never does financial math.
 */
data class LedgerPieSlice(
    val label: String,
    val value: Float,
    val valueLabel: String,
    val percent: Int,
    val color: Color,
)

/**
 * LDL interactive donut — a restrained part-to-whole composition you can *interrogate*.
 *
 * Tapping a slice selects it: that arc thickens and brightens, the others recede, and
 * the centre swaps from the resting total to the selected slice's name, value and share.
 * A soft selection haptic ticks on each change. Tapping the selected slice again (or the
 * hole) returns to the resting total. Everything shown comes from [slices] and
 * [restingCenterLabel] / [restingCenterValue] — no fabricated figures.
 *
 * The legend beside the ring is interactive too, so the same selection is reachable by
 * touch on either element (and remains usable without fine-grained arc targeting).
 */
@Composable
fun LedgerInteractivePieChart(
    slices: List<LedgerPieSlice>,
    restingCenterLabel: String,
    restingCenterValue: String,
    modifier: Modifier = Modifier,
    ringSize: Dp = 148.dp,
    strokeWidth: Dp = 18.dp,
    gapDegrees: Float = 3f,
) {
    if (slices.isEmpty()) return
    val colors = LedgerTheme.colors
    val density = LocalDensity.current
    val haptics = LedgerHaptics.current
    val track = colors.surfaceInset

    var selected by remember(slices) { mutableIntStateOf(-1) }

    fun select(index: Int) {
        val next = if (index == selected) -1 else index
        if (next != selected) {
            selected = next
            haptics.selection()
        }
    }

    val drawn = remember(slices) { slices.filter { it.value > 0f } }
    val total = remember(drawn) { drawn.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(drawn) {
                        // Tap-to-select an arc by angle; a near-still press counts as a tap.
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var moved = false
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.positionChange().getDistanceSquared() > 64f * 64f }) {
                                    moved = true
                                }
                                if (event.changes.all { !it.pressed }) break
                            }
                            if (!moved) {
                                val idx = hitSlice(
                                    down.position, size.width.toFloat(), size.height.toFloat(),
                                    drawn, total, gapDegrees,
                                )
                                select(idx)
                            }
                        }
                    },
            ) {
                val strokePx = with(density) { strokeWidth.toPx() }
                val arcSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
                val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

                drawArc(
                    color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(width = strokePx),
                )

                val gaps = if (drawn.size > 1) gapDegrees * drawn.size else 0f
                var start = -90f
                drawn.forEachIndexed { i, slice ->
                    val sweep = (slice.value / total) * (360f - gaps)
                    val isSel = i == selected
                    val dim = selected >= 0 && !isSel
                    val thisStroke = if (isSel) strokePx * 1.24f else strokePx
                    drawArc(
                        color = if (dim) slice.color.copy(alpha = 0.32f) else slice.color,
                        startAngle = start + gapDegrees / 2f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = thisStroke, cap = StrokeCap.Round),
                    )
                    start += sweep + gapDegrees
                }
            }

            // Centre: resting total, or the selected slice's facts.
            val sel = selected.takeIf { it in drawn.indices }?.let { drawn[it] }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (sel == null) {
                    Text(restingCenterLabel, style = LedgerTextStyles.Caption, color = colors.textTertiary)
                    Text(
                        restingCenterValue,
                        style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                } else {
                    Text(
                        sel.label,
                        style = LedgerTextStyles.Caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        sel.valueLabel,
                        style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("${sel.percent}%", style = LedgerTextStyles.Caption, color = sel.color)
                }
            }
        }

        Spacer(Modifier.width(LedgerSpacing.Large))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
        ) {
            drawn.forEachIndexed { i, slice ->
                LegendRow(
                    slice = slice,
                    selected = i == selected,
                    dimmed = selected >= 0 && i != selected,
                    onClick = { select(i) },
                )
            }
        }
    }
}

@Composable
private fun LegendRow(
    slice: LedgerPieSlice,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val colors = LedgerTheme.colors
    val alpha = if (dimmed) 0.45f else 1f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.surfaceInset else Color.Transparent)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(vertical = 3.dp, horizontal = 4.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(slice.color.copy(alpha = alpha)))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(
            slice.label,
            style = LedgerTextStyles.BodyMedium,
            color = colors.textPrimary.copy(alpha = alpha),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            "${slice.percent}%",
            style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
            color = colors.textSecondary.copy(alpha = alpha),
        )
    }
}

/**
 * Maps a touch point on the ring to a slice index, or -1 if it landed in the hole /
 * outside / in a gap. Angles are measured clockwise from 12 o'clock to match the
 * drawing convention (which starts at -90° and sweeps positive).
 */
private fun hitSlice(
    pos: Offset,
    width: Float,
    height: Float,
    drawn: List<LedgerPieSlice>,
    total: Float,
    gapDegrees: Float,
): Int {
    val cx = width / 2f
    val cy = height / 2f
    val dx = pos.x - cx
    val dy = pos.y - cy
    val r = hypot(dx, dy)
    val outer = minOf(width, height) / 2f
    val inner = outer * 0.5f
    if (r < inner || r > outer) return -1

    // Clockwise degrees from top (12 o'clock).
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
    if (angle < 0f) angle += 360f

    val gaps = if (drawn.size > 1) gapDegrees * drawn.size else 0f
    var start = 0f
    drawn.forEachIndexed { i, slice ->
        val sweep = (slice.value / total) * (360f - gaps)
        val segStart = start + gapDegrees / 2f
        val segEnd = segStart + sweep
        if (angle in segStart..segEnd) return i
        start += sweep + gapDegrees
    }
    return -1
}

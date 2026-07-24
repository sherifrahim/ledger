package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.haptics.LedgerHaptics
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import kotlin.math.roundToInt

/**
 * A single point on the interactive line chart. Domain-agnostic: the caller
 * formats money (currency, minor units) into [valueLabel] so this component never
 * does financial formatting itself — it only draws strings it is handed.
 *
 * @param axisLabel short x-axis label candidate, e.g. "5 Jul".
 * @param value the numeric value used purely for plotting the line height.
 * @param valueLabel the exact, pre-formatted figure revealed on scrub, e.g. "AED 1,234.00".
 */
data class LedgerLinePoint(
    val axisLabel: String,
    val value: Float,
    val valueLabel: String,
)

/**
 * LDL interactive line chart — calm and editorial, but now *legible* and *touchable*.
 *
 * Over the restrained single-line aesthetic of [LedgerLineChart] this adds the three
 * things the mock parity plan asks for:
 *  - **Labeled axes** — a left Y scale (a few quiet value ticks) and a bottom X scale
 *    (a few date labels), drawn faintly so the data still leads.
 *  - **Touch-scrub** — press or drag across the plot to reveal a vertical guide, a dot
 *    on the line, and a floating callout with that point's exact date + value. A soft
 *    selection haptic ticks each time the highlighted point changes.
 *  - **Real data** — every figure comes from [points]; nothing is fabricated.
 *
 * Scroll-friendliness: only *horizontal* drags are captured (vertical drags fall
 * through so a surrounding scrolling list still scrolls), and a plain tap reveals the
 * nearest point. The highlight persists after lift so the value stays readable.
 *
 * @param zeroBaseline anchor the Y axis at 0 (honest for spending/flow magnitudes)
 *   rather than the data's own min — set false to emphasise movement in a bounded range.
 */
@Composable
fun LedgerInteractiveLineChart(
    points: List<LedgerLinePoint>,
    yAxisFormatter: (Float) -> String,
    modifier: Modifier = Modifier,
    lineColor: Color = LedgerTheme.colors.accent,
    fill: Boolean = true,
    zeroBaseline: Boolean = true,
    height: Dp = 180.dp,
) {
    if (points.isEmpty()) return

    val colors = LedgerTheme.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val haptics = LedgerHaptics.current

    // -1 == resting (no point highlighted).
    var selected by remember(points) { mutableIntStateOf(-1) }

    val strokeWidth = with(density) { 2.dp.toPx() }
    val axisLabelStyle = remember(colors) {
        TextStyle(fontSize = 10.sp, color = colors.textTertiary)
    }
    val calloutLabelStyle = remember(colors) {
        TextStyle(fontSize = 11.sp, color = colors.textSecondary)
    }
    val calloutValueStyle = remember(colors) {
        TextStyle(fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
    }

    // Plot insets: room for the Y labels on the left and X labels along the bottom.
    val leftInset = with(density) { 44.dp.toPx() }
    val bottomInset = with(density) { 20.dp.toPx() }
    val topInset = with(density) { 8.dp.toPx() }
    val rightInset = with(density) { 8.dp.toPx() }

    val minV = if (zeroBaseline) 0f else (points.minOf { it.value })
    val maxVRaw = points.maxOf { it.value }
    val maxV = if (maxVRaw <= minV) minV + 1f else maxVRaw
    val range = maxV - minV

    fun updateSelection(newIndex: Int) {
        val clamped = newIndex.coerceIn(0, points.lastIndex)
        if (clamped != selected) {
            selected = clamped
            haptics.selection()
        }
    }

    Canvas(
        modifier = modifier
            .height(height)
            .pointerInput(points) {
                detectTapGestures { pos ->
                    updateSelection(xToIndex(pos.x, leftInset, size.width - rightInset, points.size))
                }
            }
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        updateSelection(xToIndex(pos.x, leftInset, size.width - rightInset, points.size))
                    },
                    onHorizontalDrag = { change, _ ->
                        updateSelection(xToIndex(change.position.x, leftInset, size.width - rightInset, points.size))
                        change.consume()
                    },
                )
            },
    ) {
        val plotLeft = leftInset
        val plotRight = size.width - rightInset
        val plotTop = topInset
        val plotBottom = size.height - bottomInset
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - plotTop
        val stepX = if (points.size > 1) plotW / (points.size - 1) else 0f

        fun xAt(i: Int): Float = if (points.size == 1) plotLeft + plotW / 2f else plotLeft + i * stepX
        fun yAt(v: Float): Float = plotBottom - ((v - minV) / range) * plotH

        // --- Y gridlines + labels (min / mid / max) ---
        val ticks = listOf(minV, minV + range / 2f, maxV)
        val gridColor = colors.border.copy(alpha = 0.5f)
        ticks.forEach { tick ->
            val y = yAt(tick)
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, y),
                end = Offset(plotRight, y),
                strokeWidth = with(density) { 1.dp.toPx() },
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
            )
            val label = measurer.measure(yAxisFormatter(tick), axisLabelStyle)
            drawText(
                textLayoutResult = label,
                topLeft = Offset(
                    x = (plotLeft - 6f - label.size.width).coerceAtLeast(0f),
                    y = y - label.size.height / 2f,
                ),
            )
        }

        // --- X labels (first / quartiles / last, deduped) ---
        val xLabelIndices = niceXIndices(points.size)
        xLabelIndices.forEach { i ->
            val text = measurer.measure(points[i].axisLabel, axisLabelStyle)
            val cx = xAt(i)
            val tx = when (i) {
                0 -> cx
                points.lastIndex -> cx - text.size.width
                else -> cx - text.size.width / 2f
            }.coerceIn(0f, size.width - text.size.width)
            drawText(
                textLayoutResult = text,
                topLeft = Offset(tx, plotBottom + 4f),
            )
        }

        // --- Fill + line ---
        val line = Path()
        val area = Path()
        points.indices.forEach { i ->
            val x = xAt(i)
            val y = yAt(points[i].value)
            if (i == 0) {
                line.moveTo(x, y); area.moveTo(x, plotBottom); area.lineTo(x, y)
            } else {
                line.lineTo(x, y); area.lineTo(x, y)
            }
            if (i == points.lastIndex) { area.lineTo(x, plotBottom); area.close() }
        }
        if (fill && points.size > 1) {
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.16f), Color.Transparent),
                    startY = plotTop,
                    endY = plotBottom,
                ),
            )
        }
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // --- Resting marker: the most recent point, quietly ---
        if (selected < 0) {
            val lx = xAt(points.lastIndex)
            val ly = yAt(points.last().value)
            drawCircle(color = lineColor, radius = strokeWidth * 1.8f, center = Offset(lx, ly))
            drawCircle(color = colors.surfaceCard, radius = strokeWidth * 0.8f, center = Offset(lx, ly))
        }

        // --- Scrub highlight + callout ---
        if (selected in points.indices) {
            val p = points[selected]
            val sx = xAt(selected)
            val sy = yAt(p.value)
            drawLine(
                color = colors.textTertiary.copy(alpha = 0.4f),
                start = Offset(sx, plotTop),
                end = Offset(sx, plotBottom),
                strokeWidth = with(density) { 1.dp.toPx() },
            )
            drawCircle(color = colors.surfaceCard, radius = strokeWidth * 3f, center = Offset(sx, sy))
            drawCircle(color = lineColor, radius = strokeWidth * 2.2f, center = Offset(sx, sy))
            drawCircle(color = colors.surfaceCard, radius = strokeWidth * 0.9f, center = Offset(sx, sy))

            drawCallout(
                measurer = measurer,
                density = density,
                anchorX = sx,
                plotLeft = plotLeft,
                plotRight = plotRight,
                plotTop = plotTop,
                dateLabel = p.axisLabel,
                valueLabel = p.valueLabel,
                dateStyle = calloutLabelStyle,
                valueStyle = calloutValueStyle,
                bg = colors.surfaceOverlay,
                borderColor = colors.cardBorder,
            )
        }
    }
}

private fun xToIndex(x: Float, plotLeft: Float, plotRight: Float, count: Int): Int {
    if (count <= 1) return 0
    val clampedX = x.coerceIn(plotLeft, plotRight)
    val frac = (clampedX - plotLeft) / (plotRight - plotLeft)
    return (frac * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

/** First, ~⅓, ~⅔, last — deduplicated for short series so labels never overlap. */
private fun niceXIndices(count: Int): List<Int> = when {
    count <= 1 -> listOf(0)
    count <= 3 -> (0 until count).toList()
    else -> listOf(0, count / 3, (2 * count) / 3, count - 1).distinct()
}

private fun DrawScope.drawCallout(
    measurer: TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    anchorX: Float,
    plotLeft: Float,
    plotRight: Float,
    plotTop: Float,
    dateLabel: String,
    valueLabel: String,
    dateStyle: TextStyle,
    valueStyle: TextStyle,
    bg: Color,
    borderColor: Color,
) {
    val date = measurer.measure(dateLabel, dateStyle)
    val value = measurer.measure(valueLabel, valueStyle)
    val padH = with(density) { 8.dp.toPx() }
    val padV = with(density) { 6.dp.toPx() }
    val gap = with(density) { 2.dp.toPx() }
    val contentW = maxOf(date.size.width, value.size.width).toFloat()
    val contentH = date.size.height + gap + value.size.height
    val boxW = contentW + padH * 2
    val boxH = contentH + padV * 2

    var boxLeft = anchorX - boxW / 2f
    boxLeft = boxLeft.coerceIn(plotLeft, (plotRight - boxW).coerceAtLeast(plotLeft))
    val boxTop = plotTop

    val corner = CornerRadius(with(density) { 10.dp.toPx() })
    val rr = RoundRect(Rect(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH), corner)
    val path = Path().apply { addRoundRect(rr) }
    drawPath(path, color = bg)
    drawPath(path, color = borderColor, style = Stroke(width = with(density) { 1.dp.toPx() }))

    drawText(
        textLayoutResult = date,
        topLeft = Offset(boxLeft + padH, boxTop + padV),
    )
    drawText(
        textLayoutResult = value,
        topLeft = Offset(boxLeft + padH, boxTop + padV + date.size.height + gap),
    )
}

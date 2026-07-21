package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

data class LedgerDonutSlice(val value: Float, val color: Color)

/**
 * LDL donut — a restrained part-to-whole composition (Apple Health, not a
 * reporting widget). Soft slices with rounded caps and small gaps over a quiet
 * track; no 3D, no gradients, no in-chart legend (the legend is a plain list next
 * to it). The [center] slot holds a single calm figure, nothing more.
 */
@Composable
fun LedgerDonutChart(
    slices: List<LedgerDonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    gapDegrees: Float = 4f,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    val track = LedgerTheme.colors.surfaceInset
    val density = LocalDensity.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = with(density) { strokeWidth.toPx() }
            val inset = strokePx / 2f
            val arcSize = Size(size.minDimension - strokePx, size.minDimension - strokePx)
            val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)

            // Quiet full-circle track.
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(width = strokePx),
            )

            val drawn = slices.filter { it.value > 0f }
            val gaps = if (drawn.size > 1) gapDegrees * drawn.size else 0f
            var start = -90f
            drawn.forEach { slice ->
                val sweep = (slice.value / total) * (360f - gaps)
                drawArc(
                    color = slice.color, startAngle = start + gapDegrees / 2f, sweepAngle = sweep,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(width = strokePx - inset * 0.15f, cap = StrokeCap.Round),
                )
                start += sweep + gapDegrees
            }
        }
        center()
    }
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL line chart — calm, editorial, information-first.
 *
 * Deliberately restrained (Apple Health / Linear / Things, not a finance
 * dashboard): a single thin line, no gridlines, no axis chrome, no per-point
 * markers, and no gradient by default. The vertical range is scaled to the data's
 * own min…max so real movement — including declines — is visible rather than
 * squashed against the top. Only the most recent point is marked, quietly.
 */
@Composable
fun LedgerLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = LedgerTheme.colors.textSecondary,
    fill: Boolean = false,
    fillColor: Color = lineColor.copy(alpha = 0.06f),
    markLast: Boolean = true,
) {
    if (data.isEmpty()) return
    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.toPx() }
    val surface = LedgerTheme.colors.surfaceCard

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val padY = h * 0.14f
        val usableH = h - padY * 2
        val minV = data.minOrNull() ?: 0f
        val maxV = data.maxOrNull() ?: 1f
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = if (data.size > 1) w / (data.size - 1) else 0f

        fun pointAt(i: Int): Offset {
            val x = if (data.size == 1) w / 2f else i * stepX
            val norm = (data[i] - minV) / range          // 0 (min) … 1 (max)
            val y = padY + (1f - norm) * usableH
            return Offset(x, y)
        }

        val line = Path()
        val area = Path()
        data.indices.forEach { i ->
            val p = pointAt(i)
            if (i == 0) {
                line.moveTo(p.x, p.y); area.moveTo(p.x, h); area.lineTo(p.x, p.y)
            } else {
                line.lineTo(p.x, p.y); area.lineTo(p.x, p.y)
            }
            if (i == data.lastIndex) { area.lineTo(p.x, h); area.close() }
        }

        if (fill && data.size > 1) {
            drawPath(
                path = area,
                brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)),
            )
        }

        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        if (markLast) {
            val last = pointAt(data.lastIndex)
            drawCircle(color = lineColor, radius = strokeWidth * 1.8f, center = last)
            drawCircle(color = surface, radius = strokeWidth * 0.8f, center = last)
        }
    }
}

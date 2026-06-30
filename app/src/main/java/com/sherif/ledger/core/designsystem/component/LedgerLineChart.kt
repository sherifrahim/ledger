package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Handcrafted LDL Line Chart.
 *
 * Designed with financial precision and atmospheric depth.
 */
@Composable
fun LedgerLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = LedgerTheme.colors.expense,
    fillColor: Color = LedgerTheme.colors.expense.copy(alpha = 0.1f),
) {
    if (data.isEmpty()) return
    val surfaceColor = LedgerTheme.colors.surfaceLevel0
    val density = LocalDensity.current
    val strokeWidth = with(density) { 2.dp.toPx() }
    val dotRadius = with(density) { 3.dp.toPx() }
    val dotInnerRadius = with(density) { 1.5.dp.toPx() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val stepX = w / (data.size - 1)
        val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
        
        val path = Path()
        val fillPath = Path()
        
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / maxVal * h * 0.7f) - (h * 0.15f)
            
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            
            if (i == data.lastIndex) {
                fillPath.lineTo(x, h)
                fillPath.close()
            }
        }

        // 1. Draw atmospheric area fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = h * 0.1f,
                endY = h
            )
        )

        // 2. Draw precise data line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
        
        // 3. Optional markers (dots) for data points
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / maxVal * h * 0.7f) - (h * 0.15f)
            drawCircle(
                color = lineColor,
                radius = dotRadius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
            drawCircle(
                color = surfaceColor,
                radius = dotInnerRadius,
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}

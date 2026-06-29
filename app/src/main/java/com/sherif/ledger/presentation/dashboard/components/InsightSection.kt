package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

/**
 * Insight section for Dashboard.
 *
 * Faithfully recreates the 'Food spending' card seen in the mockup.
 */
@Composable
fun InsightSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    if (state.insights.isEmpty()) return
    val insight = state.insights.first()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .ledgerSurface(level = LedgerSurfaceLevel.Level1)
                .padding(LedgerSpacing.Medium),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(insight.title, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            null,
                            tint = LedgerTheme.colors.income,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = insight.indicator,
                            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                            color = LedgerTheme.colors.income,
                        )
                    }
                }

                Text(
                    text = insight.subtitle,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.income,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Box(
                        modifier = Modifier
                            .ledgerSurface(
                                backgroundColor = Color.White.copy(alpha = 0.05f),
                                borderColor = Color.White.copy(alpha = 0.1f),
                                shape = LedgerTheme.radius.Full,
                            )
                            .ledgerClickable { /* TODO */ }
                            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.XxSmall),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "View insight",
                                style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Spacer(Modifier.width(LedgerSpacing.XxSmall))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(10.dp),
                            )
                        }
                    }

                    MiniSparkline(modifier = Modifier.padding(bottom = LedgerSpacing.XxSmall))
                }
            }
        }
    }
}

@Composable
private fun MiniSparkline(modifier: Modifier = Modifier) {
    val color = LedgerTheme.colors.income
    val density = LocalDensity.current
    val strokeWidth = with(density) { LedgerTheme.border.Hairline.toPx() * 2f }
    
    Canvas(modifier = modifier.width(44.dp).height(18.dp)) {
        val points = listOf(0.7f, 0.85f, 0.6f, 0.75f, 0.45f, 0.5f, 0.3f)
        val stepX = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height * (1f - v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
            ),
        )
    }
}

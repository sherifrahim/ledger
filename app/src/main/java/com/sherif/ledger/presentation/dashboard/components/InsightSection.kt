package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

/**
 * Insight section.
 *
 * Designed with a "Boundary-less" architecture. Individual insights are
 * separated by rhythm and hairlines rather than boxed cards.
 */
@Composable
fun InsightSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    if (state.insights.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        LedgerSectionHeader(title = "Insights")

        Column(modifier = Modifier.fillMaxWidth()) {
            state.insights.forEachIndexed { index, insight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ledgerClickable { /* TODO */ }
                        .padding(vertical = LedgerSpacing.Group),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall)) {
                        Text(insight.title, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                        Text(insight.subtitle, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                    }
                    Spacer(Modifier.width(LedgerSpacing.Small))
                    if (insight.indicator.isNotEmpty()) {
                        MiniSparkline()
                        Spacer(Modifier.width(LedgerSpacing.Small))
                        Text(insight.indicator, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.income)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = LedgerTheme.colors.tertiaryLabel,
                            modifier = Modifier.size(LedgerTheme.iconSize.Small),
                        )
                    }
                }
                if (index != state.insights.lastIndex) {
                    LedgerHairline()
                }
            }
        }
    }
}

@Composable
private fun MiniSparkline(modifier: Modifier = Modifier) {
    val color = LedgerTheme.colors.income
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
                width = LedgerTheme.border.Hairline.toPx() * 2f,
                cap = StrokeCap.Round,
            ),
        )
    }
}

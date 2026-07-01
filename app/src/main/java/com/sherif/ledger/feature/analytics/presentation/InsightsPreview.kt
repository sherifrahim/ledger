package com.sherif.ledger.feature.analytics.presentation

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

/**
 * Reusable Insight Preview Card.
 *
 * Provides a high-density summary of spending context with a call to action
 * to the full analytics view. Used in Home and Accounts.
 */
@Composable
fun InsightsPreview(
    title: String,
    subtitle: String,
    indicator: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            .padding(vertical = LedgerSpacing.Medium, horizontal = LedgerSpacing.Medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                    color = LedgerTheme.colors.label
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = LedgerTheme.colors.income,
                        modifier = Modifier.size(10.dp),
                    )
                    Text(
                        text = indicator,
                        style = LedgerTextStyles.Caption.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = LedgerTheme.colors.income,
                    )
                }
            }

            Text(
                text = subtitle,
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.success.copy(alpha = 0.8f),
            )
            
            Spacer(Modifier.height(LedgerSpacing.XxSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .ledgerSurface(
                            backgroundColor = Color.White.copy(alpha = 0.05f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            shape = LedgerTheme.radius.Full,
                        )
                        .ledgerClickable(onClick = onClick)
                        .padding(horizontal = LedgerSpacing.Medium, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "View insight",
                            style = LedgerTextStyles.Caption.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.width(LedgerSpacing.XxSmall))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }

                MiniSparkline()
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

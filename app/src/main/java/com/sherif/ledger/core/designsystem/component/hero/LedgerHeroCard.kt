package com.sherif.ledger.core.designsystem.component.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LedgerHeroCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F766E),
            Color(0xFF115E59),
        ),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(235.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(gradient),
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                Text(
                    text = title,
                    color = Color.White.copy(alpha = .82f),
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(
                    text = value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp,
                )

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = .80f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = .18f),
                )

                Text(
                    text = "${(progress * 100).toInt()}% completed",
                    color = Color.White.copy(alpha = .75f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

        }

    }
}

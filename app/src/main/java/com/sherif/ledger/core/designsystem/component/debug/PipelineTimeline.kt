package com.sherif.ledger.core.designsystem.component.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Forensic timeline component for the developer console.
 */
@Composable
fun PipelineTimeline(
    items: List<PipelineEvent>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        items.forEachIndexed { index, event ->
            TimelineNode(
                event = event,
                isFirst = index == 0,
                isLast = index == items.lastIndex
            )
        }
    }
}

data class PipelineEvent(
    val title: String,
    val timestamp: String,
    val color: Color,
    val detail: String? = null
)

@Composable
private fun TimelineNode(
    event: PipelineEvent,
    isFirst: Boolean,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(8.dp)
                        .background(LedgerTheme.colors.separator.copy(alpha = 0.1f))
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(event.color, CircleShape)
            )
            
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(LedgerTheme.colors.separator.copy(alpha = 0.1f))
                )
            }
        }
        
        Column(
            modifier = Modifier
                .padding(bottom = if (isLast) 0.dp else LedgerSpacing.Large)
                .weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                    color = LedgerTheme.colors.label
                )
                Text(
                    text = event.timestamp,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel
                )
            }
            if (event.detail != null) {
                Text(
                    text = event.detail,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.secondaryLabel
                )
            }
        }
    }
}

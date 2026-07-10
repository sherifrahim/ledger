package com.sherif.ledger.core.designsystem.component.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Container for individual ingestion stage status in the developer console.
 */
@Composable
fun PipelineStageCard(
    stage: String,
    status: String,
    duration: String? = null,
    confidence: String? = null,
    reason: String? = null,
    modifier: Modifier = Modifier,
    statusColor: Color = LedgerTheme.colors.tint,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stage,
                    style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
                    color = LedgerTheme.colors.label
                )
                if (duration != null) {
                    Text(
                        text = duration,
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.tertiaryLabel
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                if (confidence != null) {
                    ConfidenceBadge(text = confidence, color = statusColor)
                }
                ConfidenceBadge(text = status, color = statusColor)
            }
        }
        
        if (reason != null) {
            Spacer(Modifier.height(LedgerSpacing.Small))
            Text(
                text = reason,
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.secondaryLabel
            )
        }
        
        if (content != null) {
            Spacer(Modifier.height(LedgerSpacing.Medium))
            content()
        }
    }
}

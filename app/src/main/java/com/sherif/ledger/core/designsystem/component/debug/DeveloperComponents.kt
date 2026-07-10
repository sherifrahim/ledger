package com.sherif.ledger.core.designsystem.component.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.LedgerTag
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Reusable key/value rows for technical diagnostics.
 */
@Composable
fun DiagnosticKeyValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LedgerTheme.colors.label
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.XxSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.secondaryLabel
        )
        Text(
            text = value,
            style = LedgerTextStyles.Caption.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = valueColor
        )
    }
}

/**
 * Small semantic chip for extraction confidence or status.
 */
@Composable
fun ConfidenceBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LedgerTheme.colors.tint
) {
    LedgerTag(
        text = text,
        modifier = modifier,
        containerColor = color.copy(alpha = 0.1f),
        contentColor = color
    )
}

/**
 * Standardized header for Developer Console sections.
 */
@Composable
fun DeveloperSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.Small),
        style = LedgerTextStyles.Caption.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        ),
        color = LedgerTheme.colors.tertiaryLabel
    )
}

/**
 * Visual representation of an entity relationship.
 */
@Composable
fun RelationshipCard(
    source: String,
    target: String,
    type: String,
    modifier: Modifier = Modifier
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level2,
        contentPadding = PaddingValues(LedgerSpacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(source, style = LedgerTextStyles.Caption, fontWeight = FontWeight.Bold)
            Text("\u2192", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text(target, style = LedgerTextStyles.Caption, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            ConfidenceBadge(text = type, color = LedgerTheme.colors.secondaryLabel)
        }
    }
}

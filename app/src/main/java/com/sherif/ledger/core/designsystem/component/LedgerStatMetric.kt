package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL atomic stat metric.
 *
 * Used in Dashboard QuickStats and other summary sections.
 */
@Composable
fun LedgerStatMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LedgerTheme.colors.label,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Spacer(Modifier.height(LedgerSpacing.XxSmall))
        Text(
            text = value,
            style = LedgerTextStyles.Section,
            color = valueColor,
        )
    }
}

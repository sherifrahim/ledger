package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Compact LDL surface for one dashboard metric.
 *
 * Built on [LedgerSurface] at Level1, so a metric tile reads as a grouped
 * section on the Level0 page rather than a floating Material card. Pass
 * preformatted values from the screen layer; this carries no business logic.
 */
@Composable
fun LedgerStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = LedgerTheme.colors.label,
    supportingText: String? = null,
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Medium),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XSmall),
        ) {
            Text(
                text = label,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.secondaryLabel,
            )
            LedgerAmount(
                amount = value,
                style = LedgerAmountStyle.Large,
                color = valueColor,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.secondaryLabel,
                )
            }
        }
    }
}

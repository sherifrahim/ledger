package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Ledger V3 Header (Architectural Anchor)
 * 
 * Provides a clean, typographic start to every major screen zone.
 */
@Composable
fun LedgerHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LedgerSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Atomic)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = LedgerTextStyles.Headline,
                color = LedgerTheme.colors.textPrimary,
            )
            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = LedgerTextStyles.BodyMedium,
                color = LedgerTheme.colors.textSecondary,
            )
        }
    }
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Standard LDL section heading.
 *
 * Sits above an inset grouped surface to label it, in the Apple grouped-list
 * grammar where the header is outside the rounded group rather than inside it.
 */
@Composable
fun LedgerSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = LedgerTextStyles.Section,
        color = LedgerTheme.colors.label,
    )
}

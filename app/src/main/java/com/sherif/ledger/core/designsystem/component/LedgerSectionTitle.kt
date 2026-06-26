package com.sherif.ledger.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles

/**
 * Standard LDS section heading.
 *
 * Use this to label logical screen groups such as "Recent transactions",
 * "Budgets", or "Analytics".
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
        color = MaterialTheme.colorScheme.onBackground,
    )
}

package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * TimelineDateHeader
 *
 * Separates the user's financial timeline into human-readable chapters.
 *
 * Examples:
 *
 * Today
 * Yesterday
 * 23 Jun
 *
 * This is intentionally lighter than a page title.
 * The transaction rows remain the visual focus.
 */
@Composable
fun TimelineDateHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = LedgerSpacing.Screen,
                vertical = LedgerSpacing.Content,
            ),
    ) {
        Text(
            text = title.uppercase(),
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.secondaryLabel,
        )
    }
}

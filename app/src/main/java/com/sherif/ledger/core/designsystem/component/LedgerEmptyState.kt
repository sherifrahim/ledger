package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles

/**
 * Standard LDS empty state for screens and sections with no content yet.
 *
 * The optional illustration slot lets features provide an icon or artwork
 * without coupling LDS to a specific feature.
 */
@Composable
fun LedgerEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
    ) {
        if (illustration != null) {
            illustration()
        }
        Text(
            text = title,
            style = LedgerTextStyles.Section,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (message != null) {
            Text(
                text = message,
                style = LedgerTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (action != null) {
            action()
        }
    }
}

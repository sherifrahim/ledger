package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing

enum class LedgerButtonStyle {
    Primary,
    Secondary,
    Text,
}

/**
 * Standard LDS action button with primary, secondary, and text styles.
 *
 * Prefer this over raw Material buttons so spacing, shape, and color remain
 * consistent across future screens.
 */
@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: LedgerButtonStyle = LedgerButtonStyle.Primary,
) {
    val contentPadding = PaddingValues(
        horizontal = LedgerSpacing.XLarge,
        vertical = LedgerSpacing.Small,
    )

    when (style) {
        LedgerButtonStyle.Primary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = contentPadding,
        ) {
            Text(text = text)
        }

        LedgerButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = contentPadding,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(text = text)
        }

        LedgerButtonStyle.Text -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = contentPadding,
        ) {
            Text(text = text)
        }
    }
}

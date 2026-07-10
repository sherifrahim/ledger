package com.sherif.ledger.core.designsystem.component.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Monospace technical card for raw forensic inspection.
 */
@Composable
fun RawMessageCard(
    text: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level3,
        contentPadding = PaddingValues(LedgerSpacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = LedgerTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = LedgerTheme.colors.label
                    ),
                    modifier = Modifier.padding(LedgerSpacing.Small)
                )
            }
            LedgerIconButton(
                icon = Icons.Default.ContentCopy,
                onClick = onCopyClick,
                tint = LedgerTheme.colors.tertiaryLabel
            )
        }
    }
}

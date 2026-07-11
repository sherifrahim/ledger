package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Ledger V3 Atomic Tag (Semantic Pill)
 */
@Composable
fun LedgerTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LedgerTheme.colors.surfaceInset,
    contentColor: Color = LedgerTheme.colors.textSecondary,
) {
    Box(
        modifier = modifier
            .ledgerSurface(
                shape = LedgerRadius.Small,
                backgroundColor = containerColor,
                borderColor = Color.Transparent,
            )
            .padding(
                horizontal = LedgerSpacing.Small,
                vertical = LedgerSpacing.Atomic,
            ),
    ) {
        Text(
            text = text,
            style = LedgerTextStyles.Caption,
            color = contentColor,
        )
    }
}

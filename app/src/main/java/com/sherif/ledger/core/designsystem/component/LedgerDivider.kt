package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Ledger V3 Divider (Architectural Line)
 */
@Composable
fun LedgerDivider(
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LedgerTheme.border.Hairline)
            .background(LedgerTheme.colors.border.copy(alpha = alpha))
    )
}

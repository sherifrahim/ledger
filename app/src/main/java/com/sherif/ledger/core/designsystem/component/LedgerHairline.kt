package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL hairline separator.
 *
 * A 0.5dp line at separator color. Used inside grouped surfaces to
 * divide rows without creating visual weight. Thinner than Material
 * Divider (1dp) for a more refined appearance.
 */
@Composable
fun LedgerHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(LedgerTheme.colors.separator),
    )
}

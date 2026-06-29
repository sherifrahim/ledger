package com.sherif.ledger.core.designsystem.component.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing

/**
 * LDL container for multiple metadata rows.
 *
 * Automatically applies spacing and hairlines between items.
 */
@Composable
fun LedgerInfoGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        content()
    }
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL top navigation bar.
 *
 * Purged of Material 3 CenterAlignedTopAppBar. Follows the "Carved"
 * aesthetic where the top bar is a flat extension of the background.
 */
@Composable
fun LedgerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LedgerTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Transparent)
            .padding(horizontal = LedgerSpacing.Medium),
        contentAlignment = Alignment.Center
    ) {
        if (navigationIcon != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart),
            ) {
                navigationIcon()
            }
        }

        Text(
            text = title,
            style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold),
            color = colors.label,
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions()
        }
    }
}

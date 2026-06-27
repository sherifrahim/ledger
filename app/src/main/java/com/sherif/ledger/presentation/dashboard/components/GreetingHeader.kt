package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Dashboard greeting shown at the top of the Home screen.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    userName: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$greeting,",
            style = LedgerTextStyles.Body,
            color = LedgerTheme.colors.secondaryLabel,
        )
        Text(
            text = userName,
            style = LedgerTextStyles.Headline,
            color = LedgerTheme.colors.label,
        )
    }
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    illustration: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        if (illustration != null) illustration()
        Text(title, style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)
        if (message != null) Text(message, style = LedgerTextStyles.Body, color = LedgerTheme.colors.secondaryLabel)
        if (action != null) action()
    }
}

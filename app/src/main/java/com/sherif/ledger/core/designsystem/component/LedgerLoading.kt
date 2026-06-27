package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerLoading(modifier: Modifier = Modifier, message: String? = null) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)) {
        CircularProgressIndicator(color = LedgerTheme.colors.tint)
        if (message != null) Text(message, style = LedgerTextStyles.Body, color = LedgerTheme.colors.secondaryLabel)
    }
}

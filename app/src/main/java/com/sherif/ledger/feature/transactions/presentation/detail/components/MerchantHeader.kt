package com.sherif.ledger.feature.transactions.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Merchant identity header for transaction detail and review screens.
 *
 * Centered avatar above merchant name and category. Future consumers:
 * Review Inbox (confirming a parsed merchant) and Merchant Profile.
 */
@Composable
fun MerchantHeader(
    name: String,
    category: String,
    accentHue: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
    ) {
        LedgerAvatar(
            name = name,
            color = Color(accentHue),
            modifier = Modifier.size(56.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = LedgerTextStyles.Title, color = LedgerTheme.colors.label)
            Spacer(Modifier.height(LedgerSpacing.XxSmall))
            Text(category, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
    }
}

package com.sherif.ledger.core.designsystem.component

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Merchant identity display: centered avatar, name, and category.
 *
 * Consumers: Transaction Details, Review Inbox. Future: Merchant Profile.
 */
@Composable
fun MerchantHeader(
    name: String,
    category: String,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 64.dp,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
    ) {
        LedgerBrandIcon(name = name, size = avatarSize)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = LedgerTextStyles.Title, color = LedgerTheme.colors.label)
            Spacer(Modifier.height(LedgerSpacing.XxSmall))
            Text(category, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
    }
}

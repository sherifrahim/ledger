package com.sherif.ledger.feature.transactions.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Large centered amount display for transaction detail.
 *
 * The amount is the emotional anchor: the user instantly knows
 * how much. Status and time sit below as quiet context.
 * Future consumers: Review Inbox and Capture confirmation.
 */
@Composable
fun AmountHero(
    amount: String,
    sign: String,
    isIncome: Boolean,
    status: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    val amountColor = if (isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Inline),
    ) {
        Text(
            "${sign}AED $amount",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
            color = amountColor,
        )
        Text(
            "$status \u00B7 $time",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
    }
}

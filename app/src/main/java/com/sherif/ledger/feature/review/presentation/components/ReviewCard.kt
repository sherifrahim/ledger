package com.sherif.ledger.feature.review.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.review.presentation.ReviewItemUi

/**
 * Review card for one transaction requiring confirmation.
 *
 * Stateless. Actions are callbacks, no internal state.
 * The card tells a story: who, how much, what the parser thinks,
 * why it needs review, and what the user can do about it.
 */
@Composable
fun ReviewCard(
    item: ReviewItemUi,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onIgnore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amountColor = if (item.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (item.isIncome) "+" else "-"
    val confidenceColor = when {
        item.confidence >= 80 -> LedgerTheme.colors.income
        item.confidence >= 50 -> LedgerTheme.colors.pending
        else -> LedgerTheme.colors.expense
    }

    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Group),
    ) {
        // Merchant + amount
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerAvatar(
                name = item.merchant,
                color = Color(item.merchantAccentHue),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(item.merchant, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(item.timestamp, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            Text(
                "${sign}AED ${item.amount}",
                style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                color = amountColor,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerHairline()
        Spacer(Modifier.height(LedgerSpacing.Small))

        // Parsed fields
        DetailRow("Category", item.suggestedCategory)
        DetailRow("Account", item.suggestedAccount)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Confidence", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text("${item.confidence}%", style = LedgerTextStyles.Label, color = confidenceColor)
        }

        Spacer(Modifier.height(LedgerSpacing.Content))

        // Reason
        Text(
            "\u26A0 ${item.reason}",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.pending,
        )

        Spacer(Modifier.height(LedgerSpacing.Group))

        // Actions
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
            LedgerButton("Ignore", onClick = onIgnore, style = LedgerButtonStyle.Text, modifier = Modifier.weight(1f))
            LedgerButton("Edit", onClick = onEdit, style = LedgerButtonStyle.Secondary, modifier = Modifier.weight(1f))
            LedgerButton("Confirm", onClick = onConfirm, style = LedgerButtonStyle.Primary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = LedgerSpacing.Inline),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Text(value, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
    }
}

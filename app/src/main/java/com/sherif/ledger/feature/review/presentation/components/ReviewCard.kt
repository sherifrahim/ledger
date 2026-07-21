package com.sherif.ledger.feature.review.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.merchant.MerchantCategory
import com.sherif.ledger.feature.review.presentation.ReviewItemUi

/**
 * [onCategorySelected] non-null switches the bottom row from the original
 * Confirm/Edit/Ignore actions to a direct category-chip picker — there is no
 * AI suggestion to "confirm" for a genuinely unresolved merchant, so the
 * user picks the real category once, and it's remembered from then on (see
 * LearnedMerchantCategoryStore).
 */
@Composable
fun ReviewCard(
    item: ReviewItemUi,
    onConfirm: () -> Unit = {},
    onEdit: () -> Unit = {},
    onIgnore: () -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onCategorySelected: ((MerchantCategory) -> Unit)? = null,
) {
    val amountColor = if (item.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (item.isIncome) "+" else "-"
    val confidenceColor = when {
        item.confidence >= 80 -> LedgerTheme.colors.income
        item.confidence >= 50 -> LedgerTheme.colors.pending
        else -> LedgerTheme.colors.expense
    }

    LedgerSurface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Group),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerBrandIcon(name = item.merchant, size = 40.dp)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(item.merchant, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(item.timestamp, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            LedgerAmount(
                amount = "${sign}AED ${item.amount}",
                style = LedgerAmountStyle.Regular,
                color = amountColor
            )
        }
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerHairline()
        Spacer(Modifier.height(LedgerSpacing.Small))
        DetailRow("Category", item.suggestedCategory)
        DetailRow("Account", item.suggestedAccount)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Confidence", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text("${item.confidence}%", style = LedgerTextStyles.Label, color = confidenceColor)
        }
        Spacer(Modifier.height(LedgerSpacing.Content))
        Text("\u26A0 ${item.reason}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.pending)
        Spacer(Modifier.height(LedgerSpacing.Group))
        if (onCategorySelected != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ignore", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel, modifier = Modifier.clickable(onClick = onIgnore))
            }
            Spacer(Modifier.height(LedgerSpacing.Inline))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Inline)) {
                items(MerchantCategory.entries.filter { it != MerchantCategory.UNKNOWN }) { category ->
                    CategoryChip(category, onClick = { onCategorySelected(category) })
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
                LedgerButton("Ignore", onClick = onIgnore, style = LedgerButtonStyle.Text, modifier = Modifier.weight(1f))
                LedgerButton("Edit", onClick = onEdit, style = LedgerButtonStyle.Secondary, modifier = Modifier.weight(1f))
                LedgerButton("Confirm", onClick = onConfirm, style = LedgerButtonStyle.Primary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryChip(category: MerchantCategory, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LedgerTheme.colors.surfaceInset)
            .clickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Inline),
    ) {
        Text(
            text = category.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' '),
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = LedgerSpacing.Inline), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Text(value, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
    }
}

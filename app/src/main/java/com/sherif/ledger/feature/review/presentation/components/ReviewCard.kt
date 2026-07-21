package com.sherif.ledger.feature.review.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerCardDefaults
import com.sherif.ledger.core.designsystem.component.LedgerConfidenceBadge
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.merchant.MerchantCategory
import com.sherif.ledger.feature.review.presentation.ReviewItemUi

/**
 * Review Queue card (P3) — the confidence/evidence card language.
 *
 * Leads with the question ("Should this be Dining?" when a suggestion exists,
 * else "Which category?"), pairs it with the real confidence band, shows the
 * merchant / amount / time, then the evidence behind the guess (explainability
 * over decoration). The action is the *real* mechanism: for a genuinely
 * unresolved merchant there is no AI answer to "confirm", so the user picks the
 * true category once and it is remembered (LearnedMerchantCategoryStore). No
 * button performs a no-op — the category chips are the decision.
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
    val colors = LedgerTheme.colors
    val amountColor = if (item.isIncome) colors.income else colors.textPrimary
    val sign = if (item.isIncome) "+" else "-"
    val hasSuggestion = item.suggestedCategory.isNotBlank() &&
        !item.suggestedCategory.equals("unknown", ignoreCase = true) &&
        !item.suggestedCategory.equals("uncategorized", ignoreCase = true)

    LedgerCard(
        modifier = modifier,
        elevation = LedgerCardDefaults.ElevationLow,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasSuggestion) "Should this be" else "Which category?",
                    style = LedgerTextStyles.BodyMedium,
                    color = colors.textSecondary,
                )
                if (hasSuggestion) {
                    Text(
                        "${prettyCategory(item.suggestedCategory)}?",
                        style = LedgerTextStyles.Headline,
                        color = colors.textPrimary,
                    )
                }
            }
            LedgerConfidenceBadge(score = item.confidence)
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LedgerBrandIcon(name = item.merchant, size = 40.dp)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(item.merchant, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                Text(item.timestamp, style = LedgerTextStyles.Caption, color = colors.textTertiary)
            }
            LedgerAmount(amount = "${sign}AED ${item.amount}", style = LedgerAmountStyle.Regular, color = amountColor)
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        LedgerDivider(alpha = 0.06f)
        Spacer(Modifier.height(LedgerSpacing.Small))

        // Evidence — real fields only.
        if (item.suggestedAccount.isNotBlank()) EvidenceRow(Icons.Filled.Store, "Account", item.suggestedAccount)
        if (item.reason.isNotBlank()) EvidenceRow(Icons.Outlined.HelpOutline, "Why review", item.reason)

        Spacer(Modifier.height(LedgerSpacing.Medium))

        if (onCategorySelected != null) {
            Text("Choose a category", style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold), color = colors.textTertiary)
            Spacer(Modifier.height(LedgerSpacing.Small))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                items(MerchantCategory.entries.filter { it != MerchantCategory.UNKNOWN }) { category ->
                    CategoryChip(
                        category = category,
                        suggested = hasSuggestion && category.name.equals(item.suggestedCategory, ignoreCase = true),
                        onClick = { onCategorySelected(category) },
                    )
                }
            }
            Spacer(Modifier.height(LedgerSpacing.Small))
            Text("Ignore", style = LedgerTextStyles.Label, color = colors.textTertiary, modifier = Modifier.clickable(onClick = onIgnore))
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                LedgerButton("Approve", onClick = onConfirm, style = LedgerButtonStyle.Accent, modifier = Modifier.weight(1f))
                LedgerButton("Change", onClick = onEdit, style = LedgerButtonStyle.Tonal, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EvidenceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, note: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = LedgerTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.width(80.dp))
        Text(note, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategoryChip(category: MerchantCategory, suggested: Boolean, onClick: () -> Unit) {
    val bg = if (suggested) LedgerTheme.colors.accent.copy(alpha = 0.14f) else LedgerTheme.colors.surfaceInset
    val fg = if (suggested) LedgerTheme.colors.accent else LedgerTheme.colors.textPrimary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Tiny),
    ) {
        Text(prettyCategory(category.name), style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

private fun prettyCategory(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

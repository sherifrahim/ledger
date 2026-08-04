package com.sherif.ledger.feature.review.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.signedAmount

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
@OptIn(ExperimentalLayoutApi::class)
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
            LedgerBrandIcon(name = item.merchant, size = 34.dp)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(
                    item.merchant,
                    style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(item.timestamp, style = LedgerTextStyles.Caption, color = colors.textTertiary)
            }
            Spacer(Modifier.width(LedgerSpacing.Small))
            LedgerAmount(
                amount = signedAmount(item.amount, isExpense = !item.isIncome),
                currency = "AED",
                style = LedgerAmountStyle.Regular,
                color = amountColor,
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        LedgerDivider(alpha = 0.06f)
        Spacer(Modifier.height(LedgerSpacing.Small))

        // Evidence — real fields only.
        if (item.suggestedAccount.isNotBlank()) EvidenceRow(Icons.Filled.Store, "Account", item.suggestedAccount)
        if (item.reason.isNotBlank()) EvidenceRow(Icons.AutoMirrored.Outlined.HelpOutline, "Why review", item.reason)

        Spacer(Modifier.height(LedgerSpacing.Medium))

        if (onCategorySelected != null) {
            Text("Choose a category", style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold), color = colors.textTertiary)
            Spacer(Modifier.height(LedgerSpacing.Small))
            // Wraps rather than scrolls. This was a LazyRow, which hid most of the
            // categories off the right edge of the card with nothing to suggest they
            // were there, and clipped the first and last chip mid-word ("ainment",
            // "F") because a lazy row nested in a lazy column can restore a sibling
            // card's scroll offset. The set is small and fixed — showing all of it is
            // both simpler and the better answer to "which category?".
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
            ) {
                MerchantCategory.entries.filter { it != MerchantCategory.UNKNOWN }.forEach { category ->
                    CategoryChip(
                        category = category,
                        suggested = hasSuggestion && category.name.equals(item.suggestedCategory, ignoreCase = true),
                        onClick = { onCategorySelected(category) },
                    )
                }
            }
            Spacer(Modifier.height(LedgerSpacing.Medium))
            // Was a bare Text with a clickable on it: no affordance that it could be
            // tapped, and a hit target the height of one line of 13sp type.
            LedgerButton(
                text = "Ignore",
                onClick = onIgnore,
                style = LedgerButtonStyle.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
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
        // 80.dp was narrower than the longest label this row is given ("Why
        // review"), so it wrapped to two lines against a one-line value.
        Text(
            label,
            style = LedgerTextStyles.BodyMedium,
            color = LedgerTheme.colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.width(104.dp),
        )
        Text(note, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CategoryChip(category: MerchantCategory, suggested: Boolean, onClick: () -> Unit) {
    val colors = LedgerTheme.colors
    val bg = if (suggested) colors.accent.copy(alpha = 0.14f) else colors.surfaceInset
    val fg = if (suggested) colors.accent else colors.textPrimary
    // Inside a card, surfaceInset is within a couple of percent of the card's own
    // fill, so the chips rendered as bare floating words with a mysterious 16dp
    // indent (their own padding, with nothing drawn around it). The hairline is
    // what makes them read as tappable objects rather than a list of labels.
    val outline = if (suggested) colors.accent.copy(alpha = 0.5f) else colors.cardBorder
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(LedgerTheme.border.Hairline, outline, RoundedCornerShape(50))
            .ledgerClickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Tiny),
    ) {
        Text(prettyCategory(category.name), style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

private fun prettyCategory(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

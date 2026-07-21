package com.sherif.ledger.feature.review.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerCardDefaults
import com.sherif.ledger.core.designsystem.component.LedgerConfidenceBadge
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.review.presentation.components.ReviewCard
import com.sherif.ledger.feature.review.presentation.viewmodel.ReviewInboxViewModel

/**
 * Review Queue (Milestone 1.5).
 *
 * The queue is where Ledger asks for a human decision on anything it could not
 * settle deterministically. Each item is a calm card that leads with the
 * *question* ("Should this be Dining?"), pairs it with a confidence band, shows
 * the evidence behind the guess (explainability over decoration), and offers one
 * affirmative and one corrective action.
 *
 * Real items come from [ReviewInboxViewModel]. On a fresh debug device there are
 * none, so the design is shown with placeholder items; production shows the
 * honest "All clear" state instead.
 */
@Composable
fun ReviewInboxScreen(
    onReviewItemClick: ((String) -> Unit)? = null,
    viewModel: ReviewInboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(state.selectedFilter) }
    val filtered = when (selectedFilter) {
        ReviewFilter.All -> state.items
        ReviewFilter.LowConfidence -> state.items.filter { it.confidence < 50 }
        ReviewFilter.MediumConfidence -> state.items.filter { it.confidence in 50..79 }
        ReviewFilter.HighConfidence -> state.items.filter { it.confidence >= 80 }
    }
    val showcase = com.sherif.ledger.BuildConfig.DEBUG && state.items.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceBase),
        contentPadding = PaddingValues(
            start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
            top = LedgerSpacing.Small, bottom = LedgerSpacing.ScreenBottom + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
    ) {
        item("header") {
            Column(modifier = Modifier.statusBarsPadding().padding(top = LedgerSpacing.Small)) {
                Text(
                    "Review Queue",
                    style = LedgerTextStyles.Headline,
                    color = LedgerTheme.colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                val count = if (showcase) ReviewShowcase.items.size else filtered.size
                Text(
                    if (count == 0) "You're all caught up" else "$count items need a quick decision",
                    style = LedgerTextStyles.BodyMedium,
                    color = LedgerTheme.colors.textSecondary,
                )
            }
        }

        item("filters") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                ReviewFilter.entries.forEach { filter ->
                    FilterChip(label = filter.label, selected = selectedFilter == filter, onClick = { selectedFilter = filter })
                }
            }
        }

        when {
            showcase -> items(ReviewShowcase.items, key = { it.id }) { ShowcaseReviewCard(it) }
            filtered.isEmpty() -> item("empty") {
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                LedgerEmptyState(title = "All clear", subtitle = "No transactions need review right now.")
            }
            else -> items(filtered, key = { it.id }) { item ->
                ReviewCard(
                    item = item,
                    onIgnore = { viewModel.ignore(item.id) },
                    onClick = { onReviewItemClick?.invoke(item.id) },
                    onCategorySelected = { category -> viewModel.categorize(item.id, item.rawMerchantText, category) },
                )
            }
        }
    }
}

@Composable
private fun ShowcaseReviewCard(item: ShowcaseReviewItem) {
    LedgerCard(elevation = LedgerCardDefaults.Elevation) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Should this be", style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textSecondary)
                Text(
                    "${item.category}?",
                    style = LedgerTextStyles.Headline,
                    color = LedgerTheme.colors.textPrimary,
                )
            }
            LedgerConfidenceBadge(score = item.confidence)
        }
        Spacer(Modifier.height(LedgerSpacing.Medium))
        Text(item.merchant, style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.textPrimary)
        Text(item.amount, style = LedgerTextStyles.BodyLarge, color = LedgerTheme.colors.textPrimary)
        Text(item.meta, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textTertiary)

        Spacer(Modifier.height(LedgerSpacing.Medium))
        LedgerDivider(alpha = 0.06f)
        Spacer(Modifier.height(LedgerSpacing.Medium))

        Text(
            "EVIDENCE",
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = LedgerTheme.colors.textTertiary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        item.evidence.forEach { EvidenceRow(it) }

        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
            LedgerButton(text = "Approve", onClick = {}, style = LedgerButtonStyle.Accent, modifier = Modifier.weight(1f))
            LedgerButton(text = "Change", onClick = {}, style = LedgerButtonStyle.Tonal, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EvidenceRow(e: ShowcaseEvidence) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(e.icon, null, tint = LedgerTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(e.label, style = LedgerTextStyles.BodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        Text(
            e.note,
            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.SemiBold),
            color = if (e.strong) LedgerTheme.colors.positive else LedgerTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) LedgerTheme.colors.textPrimary else LedgerTheme.colors.surfaceInset
    val fg = if (selected) LedgerTheme.colors.surfaceBase else LedgerTheme.colors.textSecondary
    Box(
        modifier = Modifier
            .clip(LedgerRadius.Full)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Tiny),
    ) {
        Text(label, style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.SemiBold), color = fg)
    }
}

// ---- Design-sprint placeholder review items (DEBUG only) ----
data class ShowcaseEvidence(val icon: ImageVector, val label: String, val note: String, val strong: Boolean = false)
data class ShowcaseReviewItem(
    val id: String,
    val category: String,
    val confidence: Int,
    val merchant: String,
    val amount: String,
    val meta: String,
    val evidence: List<ShowcaseEvidence>,
)

private object ReviewShowcase {
    val items = listOf(
        ShowcaseReviewItem(
            id = "s1", category = "Dining", confidence = 92, merchant = "Burger King",
            amount = "AED 42.50", meta = "Today · Emaar Boulevard",
            evidence = listOf(
                ShowcaseEvidence(Icons.Filled.Store, "Merchant", "Verified", strong = true),
                ShowcaseEvidence(Icons.Filled.Schedule, "Time", "Lunch time"),
                ShowcaseEvidence(Icons.Filled.History, "History", "14 similar"),
            ),
        ),
        ShowcaseReviewItem(
            id = "s2", category = "Transport", confidence = 78, merchant = "Careem",
            amount = "AED 28.00", meta = "Yesterday · Business Bay",
            evidence = listOf(
                ShowcaseEvidence(Icons.Filled.Store, "Merchant", "Recognised"),
                ShowcaseEvidence(Icons.Filled.History, "History", "9 similar"),
            ),
        ),
    )
}

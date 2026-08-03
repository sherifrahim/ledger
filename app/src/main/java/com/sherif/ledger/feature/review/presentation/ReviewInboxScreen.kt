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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.review.presentation.components.ReviewCard
import com.sherif.ledger.feature.review.presentation.viewmodel.ReviewInboxViewModel
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations

/**
 * Review Queue (P3) — wired to live data only.
 *
 * The queue is where Ledger asks for a human decision on anything it could not
 * settle deterministically. Each item (real, from [ReviewInboxViewModel]) is a
 * calm [ReviewCard] that leads with the question, shows the confidence band and
 * evidence, and lets the user set the true category once (remembered thereafter).
 * When nothing needs review, an honest "All clear" state is shown — no placeholder
 * items.
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceBase),
        contentPadding = PaddingValues(
            start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
            top = LedgerSpacing.Small, bottom = ledgerScreenBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
    ) {
        item("header") {
            Column(modifier = Modifier.statusBarsPadding().padding(top = LedgerSpacing.Small)) {
                Text("Review Queue", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (filtered.isEmpty()) "You're all caught up" else "${filtered.size} items need a quick decision",
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

        if (filtered.isEmpty()) {
            item("empty") {
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                LedgerEmptyState(title = "All clear", subtitle = "No transactions need review right now.")
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                ReviewCard(
                    // Acting on a card removes it. Without this the queue jumps to
                    // its new arrangement between two frames, so the decision the
                    // user just made looks like a rendering fault.
                    modifier = Modifier.animateItem(
                        fadeInSpec = LedgerAnimations.itemAppear(),
                        placementSpec = LedgerAnimations.itemPlacement(),
                        fadeOutSpec = LedgerAnimations.itemDisappear(),
                    ),
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

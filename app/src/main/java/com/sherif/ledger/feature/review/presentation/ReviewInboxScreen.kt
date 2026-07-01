package com.sherif.ledger.feature.review.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.review.presentation.components.ReviewCard
import com.sherif.ledger.feature.review.presentation.preview.ReviewInboxPreviewData

@Composable
fun ReviewInboxScreen(
    state: ReviewInboxUiState = ReviewInboxPreviewData.state,
    onReviewItemClick: ((String) -> Unit)? = null,
) {
    var selectedFilter by remember { mutableStateOf(state.selectedFilter) }
    val filtered = when (selectedFilter) {
        ReviewFilter.All -> state.items
        ReviewFilter.LowConfidence -> state.items.filter { it.confidence < 50 }
        ReviewFilter.MediumConfidence -> state.items.filter { it.confidence in 50..79 }
        ReviewFilter.HighConfidence -> state.items.filter { it.confidence >= 80 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.XLarge, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Group),
    ) {
        item("header") {
            Column(modifier = Modifier.statusBarsPadding()) {
                LedgerHeader(title = "Review")
                Spacer(Modifier.height(LedgerSpacing.Content))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge)) {
                    SummaryCount("Pending", state.pendingCount, LedgerTheme.colors.pending)
                    SummaryCount("Confirmed", state.confirmedTodayCount, LedgerTheme.colors.income)
                    SummaryCount("Ignored", state.ignoredTodayCount, LedgerTheme.colors.tertiaryLabel)
                }
            }
        }

        item("filters") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
                ReviewFilter.entries.forEach { filter ->
                    FilterChip(label = filter.label, selected = selectedFilter == filter, onClick = { selectedFilter = filter })
                }
            }
        }

        if (filtered.isEmpty()) {
            item("empty") {
                Spacer(Modifier.height(LedgerSpacing.XxLarge))
                LedgerEmptyState(title = "All clear", message = "No transactions need review right now.")
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                ReviewCard(
                    item = item,
                    onConfirm = {},
                    onEdit = {},
                    onIgnore = {},
                    onClick = { onReviewItemClick?.invoke(item.id) },
                )
            }
        }
    }
}

@Composable
private fun SummaryCount(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text("$count", style = LedgerTextStyles.Title, color = color)
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val fg = if (selected) LedgerTheme.colors.onTint else LedgerTheme.colors.secondaryLabel
    LedgerSurface(
        modifier = Modifier.clip(LedgerShapes.small).clickable(onClick = onClick),
        level = if (selected) LedgerSurfaceLevel.Level2 else LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Inline),
    ) {
        Text(label, style = LedgerTextStyles.Caption, color = fg)
    }
}

package com.sherif.ledger.feature.story.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerTopBar
import com.sherif.ledger.core.designsystem.component.LedgerTransactionRow
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import androidx.compose.material.icons.filled.Hub
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

/**
 * Financial Story — a primary destination (spec Chapter 34/36/80).
 *
 * A chronological narrative of the user's real captured activity: each event
 * carries the relationship-derived explanation the intelligence engine already
 * produces. When there is no activity yet, an honest empty state educates rather
 * than showing sample stories (spec Chapter 29/94).
 */
@Composable
fun FinancialStoryScreen(
    state: StoryUiState,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {},
    onOpenGraph: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        // The graph is the same story told as connections rather than as a list, so
        // it belongs here beside the feed — not buried in Search's quick-access,
        // where nobody looking for their story would think to find it.
        LedgerScreenHeader(
            title = "Story",
            modifier = Modifier.padding(horizontal = LedgerSpacing.ScreenPadding),
            actions = {
                LedgerIconButton(
                    icon = Icons.Filled.Hub,
                    onClick = onOpenGraph,
                    contentDescription = "Story Graph",
                    tint = LedgerTheme.colors.textPrimary,
                )
            },
        )

        if (state.groups.isEmpty()) {
            Spacer(Modifier.height(LedgerSpacing.XxLarge))
            LedgerEmptyState(
                title = "Your Financial Story",
                subtitle = "Ledger turns your captured activity into a narrative of your " +
                    "financial life — what happened, why it matters, and what comes next. " +
                    "Capture or import activity to begin building your story.",
                icon = Icons.AutoMirrored.Filled.MenuBook,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = LedgerSpacing.ScreenPadding,
                    end = LedgerSpacing.ScreenPadding,
                    bottom = ledgerScreenBottomPadding,
                ),
            ) {
                state.groups.forEach { group ->
                    item(key = "hdr_${group.title}") {
                        androidx.compose.material3.Text(
                            text = group.title,
                            style = LedgerTextStyles.Caption,
                            color = LedgerTheme.colors.textTertiary,
                            modifier = Modifier.padding(vertical = LedgerSpacing.Medium),
                        )
                    }
                    items(group.items, key = { it.id }) { item ->
                        LedgerTransactionRow(
                            modifier = Modifier.animateItem(
                            fadeInSpec = LedgerAnimations.itemAppear(),
                            placementSpec = LedgerAnimations.itemPlacement(),
                            fadeOutSpec = LedgerAnimations.itemDisappear(),
                        ),
                            title = item.merchant,
                            amount = item.amount,
                            explanation = item.explanation,
                            currency = "AED",
                            isExpense = item.isExpense,
                            onClick = { onItemClick(item.id) },
                        )
                        LedgerDivider(alpha = 0.05f)
                    }
                }
            }
        }
    }
}

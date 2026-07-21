package com.sherif.ledger.feature.story.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerTopBar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Financial Story — a primary destination (spec Chapter 34/36/80).
 *
 * Milestone 1 (Foundation Sprint) establishes this destination in the navigation
 * with an honest empty state (empty states educate, never apologize — spec Chapter
 * 29/94). The Story timeline itself — turning Financial Events into a narrative of
 * Past/Present/Future — is deliberately NOT implemented here; it is a later
 * milestone that depends on the Financial Event model and a Story-generation Technical
 * Design Document. This file is a navigation scaffold, not a feature.
 */
@Composable
fun FinancialStoryScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceBase),
    ) {
        LedgerTopBar(title = "Story", modifier = Modifier.statusBarsPadding())
        Spacer(Modifier.height(LedgerSpacing.XxLarge))
        LedgerEmptyState(
            title = "Your Financial Story",
            subtitle = "Ledger will turn your captured activity into a narrative of your " +
                "financial life — what happened, why it matters, and what comes next. " +
                "Capture or import activity to begin building your story.",
            icon = Icons.AutoMirrored.Filled.MenuBook,
        )
    }
}

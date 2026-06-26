package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.hero.LedgerHeroCard

/**
 * Dashboard-specific adapter around the reusable LedgerHeroCard.
 */
@Composable
fun MonthlySummaryCard(
    month: String,
    totalSpent: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {

    LedgerHeroCard(
        modifier = modifier,
        title = month,
        value = totalSpent,
        subtitle = "Monthly spending",
        progress = progress,
    )

}

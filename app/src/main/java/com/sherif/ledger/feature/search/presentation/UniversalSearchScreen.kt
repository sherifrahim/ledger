package com.sherif.ledger.feature.search.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerTopBar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Universal Search — a primary destination (spec Chapter 34/41/85).
 *
 * Milestone 1 (Foundation Sprint) establishes this destination in the navigation
 * with an honest empty state. The universal, Spotlight-like search across merchants,
 * transactions, accounts, institutions, insights, and forecasts is a later milestone
 * — it is intentionally NOT implemented here.
 *
 * Note: the existing transaction filter (`SearchFilterScreen`, reachable from the
 * Transactions screen) is a separate, narrower feature and is left in place; this is
 * the destination that universal search will be built into.
 */
@Composable
fun UniversalSearchScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceBase),
    ) {
        LedgerTopBar(title = "Search", modifier = Modifier.statusBarsPadding())
        Spacer(Modifier.height(LedgerSpacing.XxLarge))
        LedgerEmptyState(
            title = "Search everything",
            subtitle = "Find merchants, transactions, accounts, institutions, insights, and " +
                "forecasts in one place. Universal search will let you search concepts, " +
                "not locations.",
            icon = Icons.Filled.Search,
        )
    }
}

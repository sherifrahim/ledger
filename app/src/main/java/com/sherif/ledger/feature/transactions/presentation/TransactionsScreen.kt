package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerSearchBar
import com.sherif.ledger.core.designsystem.component.LedgerTag
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.transactions.presentation.components.TimelineSection
import com.sherif.ledger.feature.transactions.presentation.preview.TransactionsPreviewData

@Composable
fun TransactionsScreen(
    state: TransactionsUiState = TransactionsPreviewData.state,
    onTransactionClick: ((String) -> Unit)? = null,
    onSearchClick: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            top = LedgerSpacing.Large,
            bottom = LedgerSpacing.ScreenBottom,
        ),
    ) {
        item(key = "title") {
            Text(
                text = "Transactions",
                style = LedgerTextStyles.Headline,
                color = LedgerTheme.colors.label,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = LedgerSpacing.Screen, vertical = LedgerSpacing.Medium),
            )
        }

        item(key = "search") {
            LedgerSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search transactions",
                modifier = Modifier
                    .padding(horizontal = LedgerSpacing.Screen)
                    .ledgerClickable { onSearchClick() },
                enabled = false, // Force navigation to SearchFilterScreen
            )
            Spacer(Modifier.height(LedgerSpacing.Group))
            FilterPills(modifier = Modifier.padding(horizontal = LedgerSpacing.Screen))
            Spacer(Modifier.height(LedgerSpacing.Section))
        }

        items(state.groups, key = { it.id }) { group ->
            TimelineSection(
                group = group,
                onTransactionClick = onTransactionClick,
                modifier = Modifier.padding(horizontal = LedgerSpacing.Screen),
            )
            Spacer(Modifier.height(LedgerSpacing.Section))
        }
    }
}

@Composable
private fun FilterPills(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        FilterPill("All", selected = true)
        FilterPill("Income", selected = false)
        FilterPill("Expense", selected = false)
        FilterPill("Transfer", selected = false)
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
) {
    val colors = LedgerTheme.colors
    LedgerTag(
        text = label,
        containerColor = if (selected) colors.success else colors.surfaceLevel1,
        contentColor = if (selected) colors.onTint else colors.secondaryLabel,
        modifier = Modifier.ledgerClickable { /* TODO */ },
    )
}

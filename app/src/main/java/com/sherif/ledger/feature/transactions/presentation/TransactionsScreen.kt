package com.sherif.ledger.feature.transactions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.components.TimelineSection
import com.sherif.ledger.feature.transactions.presentation.preview.TransactionsPreviewData

@Composable
fun TransactionsScreen(
    state: TransactionsUiState = TransactionsPreviewData.state,
) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            top = LedgerSpacing.XLarge,
            bottom = LedgerSpacing.ScreenBottom,
        ),
    ) {
        item(key = "title") {
            Text(
                "Transactions",
                style = LedgerTextStyles.Headline,
                color = LedgerTheme.colors.label,
                modifier = Modifier.padding(horizontal = LedgerSpacing.Screen),
            )
            Spacer(Modifier.height(LedgerSpacing.XxLarge))
        }

        item(key = "search") {
            LedgerSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search transactions",
                modifier = Modifier.padding(horizontal = LedgerSpacing.Screen),
            )
            Spacer(Modifier.height(LedgerSpacing.Section))
        }

        items(state.groups, key = { it.id }) { group ->
            TimelineSection(group = group)
            Spacer(Modifier.height(LedgerSpacing.Section))
        }
    }
}

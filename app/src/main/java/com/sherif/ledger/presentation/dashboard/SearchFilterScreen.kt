package com.sherif.ledger.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerSearchBar
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Search and Filter screen (Screen 8).
 */
@Composable
fun SearchFilterScreen(
    onBackClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(horizontal = LedgerSpacing.Screen)
            .padding(top = LedgerSpacing.XxLarge, bottom = LedgerSpacing.Medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LedgerSearchBar(
                query = "",
                onQueryChange = {},
                modifier = Modifier.weight(1f),
            )
            Text(
                "Cancel",
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label,
                modifier = Modifier.padding(start = LedgerSpacing.Medium).ledgerClickable { onBackClick() },
            )
        }

        Spacer(Modifier.height(LedgerSpacing.XLarge))

        LedgerSectionHeader(title = "Recent searches", trailing = "Clear all", onTrailingClick = {})
        Spacer(Modifier.height(LedgerSpacing.Medium))
        Row(horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
            RecentSearchPill("Amazon")
            RecentSearchPill("Carrefour")
            RecentSearchPill("Salary")
        }

        Spacer(Modifier.height(LedgerSpacing.XLarge))

        Text("Filters", style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
        Spacer(Modifier.height(LedgerSpacing.Medium))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ledgerSurface(level = LedgerSurfaceLevel.Level1)
                .padding(vertical = LedgerSpacing.Small),
        ) {
            FilterRow("Date range", "Jun 1 - Jun 25, 2025")
            FilterRow("Type", "All")
            FilterRow("Category", "All")
            FilterRow("Account", "All")
            FilterRow("Amount range", "Min - Max")
        }

        Spacer(Modifier.weight(1f))

        LedgerButton(
            text = "Apply filters",
            onClick = { onBackClick() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RecentSearchPill(label: String) {
    Text(
        text = label,
        style = LedgerTextStyles.Caption,
        color = LedgerTheme.colors.secondaryLabel,
        modifier = Modifier
            .ledgerSurface(level = LedgerSurfaceLevel.Level2, shape = LedgerRadius.Full)
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.XxSmall),
    )
}

@Composable
private fun FilterRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerClickable { /* TODO */ }
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = LedgerTextStyles.Label, color = LedgerTheme.colors.secondaryLabel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Spacer(Modifier.padding(horizontal = LedgerSpacing.XxSmall))
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = LedgerTheme.colors.tertiaryLabel,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

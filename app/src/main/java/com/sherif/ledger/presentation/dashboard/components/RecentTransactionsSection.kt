package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

private val CategoryColors = mapOf(
    "shopping" to Color(0xFFA855F7),
    "groceries" to Color(0xFF22C55E),
    "coffee" to Color(0xFF92400E),
    "food" to Color(0xFFF97316),
    "transport" to Color(0xFF3B82F6),
    "fuel" to Color(0xFFEF4444),
    "entertainment" to Color(0xFFEC4899),
    "income" to Color(0xFF047857),
)

@Composable
fun RecentTransactionsSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Recent activity", style = LedgerTextStyles.Section, color = LedgerTheme.colors.label)
            Text("See all", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tint)
        }

        LedgerSurface(
            level = LedgerSurfaceLevel.Level1,
            contentPadding = PaddingValues(0.dp),
        ) {
            state.recentTransactions.forEachIndexed { index, txn ->
                TransactionRow(txn)
                if (index != state.recentTransactions.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = 60.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(txn: com.sherif.ledger.presentation.dashboard.TransactionUiModel) {
    val catColor = CategoryColors[txn.category.lowercase()] ?: LedgerTheme.colors.tint
    val amountColor = if (txn.isExpense) LedgerTheme.colors.expense else LedgerTheme.colors.income
    val sign = if (txn.isExpense) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(catColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                txn.merchant.take(1).uppercase(),
                style = LedgerTextStyles.Label,
                color = catColor,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(txn.merchant, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
            Text(
                "${txn.category} \u00B7 ${txn.date}",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$sign${txn.amount}", style = LedgerTextStyles.Label, color = amountColor)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LedgerTheme.colors.tertiaryLabel,
            modifier = Modifier.size(18.dp),
        )
    }
}

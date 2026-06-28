package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.feature.transactions.presentation.TransactionGroupUi

/**
 * One complete day in the financial timeline.
 *
 * Composes [TimelineDateHeader], [DaySummary], and a grouped
 * [LedgerSurface] of [TransactionRow] entries separated by
 * [LedgerHairline]. This is the repeating unit that
 * TransactionsScreen renders per date group.
 */
@Composable
fun TimelineSection(
    group: TransactionGroupUi,
    modifier: Modifier = Modifier,
    onTransactionClick: ((String) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
    ) {
        TimelineDateHeader(title = group.title)
        DaySummary(summary = group.summary)

        LedgerSurface(
            level = LedgerSurfaceLevel.Level1,
            contentPadding = PaddingValues(0.dp),
        ) {
            group.transactions.forEachIndexed { index, txn ->
                TransactionRow(
                    transaction = txn,
                    onClick = onTransactionClick?.let { { it(txn.id) } },
                )
                if (index != group.transactions.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = 68.dp))
                }
            }
        }
    }
}

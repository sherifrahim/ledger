package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.feature.transactions.presentation.TransactionGroupUi

/**
 * One complete day in the financial timeline.
 *
 * Designed with a 'Boundary-less' architecture. Cards are removed in
 * favor of vertical rhythm and clear editorial hierarchy.
 */
@Composable
fun TimelineSection(
    group: TransactionGroupUi,
    modifier: Modifier = Modifier,
    onTransactionClick: ((String) -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        LedgerSectionHeader(
            title = group.title,
            trailing = "-AED ${group.summary.spent}",
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            group.transactions.forEachIndexed { index, txn ->
                TransactionRow(
                    transaction = txn,
                    onClick = onTransactionClick?.let { { it(txn.id) } },
                )
                if (index != group.transactions.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = LedgerSpacing.AvatarIndent))
                }
            }
        }
    }
}

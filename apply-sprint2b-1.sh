#!/bin/bash
set -e

B=app/src/main/java/com/sherif/ledger

echo "Applying Sprint 2B.1 — Timeline Header & Day Summary"

mkdir -p "$B/feature/transactions/presentation/components"

###############################################################################
# TimelineDateHeader.kt
###############################################################################

cat > "$B/feature/transactions/presentation/components/TimelineDateHeader.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * TimelineDateHeader
 *
 * Separates the user's financial timeline into human-readable chapters.
 *
 * Examples:
 *
 * Today
 * Yesterday
 * 23 Jun
 *
 * This is intentionally lighter than a page title.
 * The transaction rows remain the visual focus.
 */
@Composable
fun TimelineDateHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = LedgerSpacing.Screen,
                vertical = LedgerSpacing.Content,
            ),
    ) {
        Text(
            text = title.uppercase(),
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.secondaryLabel,
        )
    }
}
EOF

###############################################################################
# DaySummary.kt
###############################################################################

cat > "$B/feature/transactions/presentation/components/DaySummary.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.DaySummaryUi

/**
 * DaySummary
 *
 * Gives immediate context before the user reads
 * individual transactions.
 *
 * The user should understand the day in
 * under one second.
 *
 * No charts.
 * No decoration.
 * Just information hierarchy.
 */
@Composable
fun DaySummary(
    summary: DaySummaryUi,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = LedgerSpacing.Screen,
                vertical = LedgerSpacing.Content,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(
                LedgerSpacing.XxSmall
            ),
        ) {

            Text(
                text = "Spent",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )

            Text(
                text = "AED ${summary.spent}",
                style = LedgerTextStyles.Title,
                color = LedgerTheme.colors.expense,
            )

        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(
                LedgerSpacing.XxSmall
            ),
        ) {
            Text(
                text = "Income",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )

            Text(
                text = "AED ${summary.income}",
                style = LedgerTextStyles.Title,
                color = LedgerTheme.colors.income,
            )
        }

    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = LedgerSpacing.Screen,
                end = LedgerSpacing.Screen,
                bottom = LedgerSpacing.Section,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Column {

            Text(
                text = "Transactions",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )

            Text(
                text = "${summary.transactionCount}",
                style = LedgerTextStyles.Body,
                color = LedgerTheme.colors.label,
            )

        }

        Column(
            horizontalAlignment = Alignment.End,
        ) {

            Text(
                text = "Top Category",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )

            Text(
                text = summary.dominantCategory.name,
                style = LedgerTextStyles.Body,
                color = LedgerTheme.colors.secondaryLabel,
            )

        }

    }

}
EOF

###############################################################################
# Patch complete
#
# Sprint 2B.2 will introduce:
#
# - TransactionRow
# - TimelineSection
#
###############################################################################

echo ""
echo "----------------------------------------"
echo "Sprint 2B.1 Applied"
echo "----------------------------------------"
echo ""
echo "Files created:"
echo "  ✓ TimelineDateHeader.kt"
echo "  ✓ DaySummary.kt"
echo ""
echo "Next:"
echo "  ./gradlew assembleDebug"
echo ""
echo "If build is green,"
echo "continue with Sprint 2B.2."

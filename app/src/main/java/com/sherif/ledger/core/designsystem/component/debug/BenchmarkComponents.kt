package com.sherif.ledger.core.designsystem.component.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerBrandIcon
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Reusable components for Benchmarking and Merchant Intelligence.
 */

@Composable
fun BenchmarkSummaryCard(
    accuracy: String,
    totalTests: Int,
    failedTests: Int,
    modifier: Modifier = Modifier
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Overall Accuracy", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
                Text(accuracy, style = LedgerTextStyles.Headline, color = LedgerTheme.colors.tint)
            }
            Column(horizontalAlignment = Alignment.End) {
                DiagnosticKeyValue(label = "Tests", value = totalTests.toString())
                DiagnosticKeyValue(label = "Failed", value = failedTests.toString(), valueColor = LedgerTheme.colors.expense)
            }
        }
    }
}

@Composable
fun MerchantCard(
    name: String,
    confidence: String,
    matchCount: Int,
    modifier: Modifier = Modifier
) {
    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium)
        ) {
            LedgerBrandIcon(name = name, size = 32.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold), color = LedgerTheme.colors.label)
                Text("$matchCount matches", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            ConfidenceBadge(text = confidence)
        }
    }
}

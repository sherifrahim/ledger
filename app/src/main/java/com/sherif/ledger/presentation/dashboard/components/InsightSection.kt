package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionTitle
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun InsightSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small),
    ) {
        LedgerSectionTitle(text = "Insights")

        LedgerSurface(level = LedgerSurfaceLevel.Level1) {
            state.insights.forEachIndexed { index, insight ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LedgerSpacing.Medium),
                    verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
                ) {
                    Text(
                        text = insight.title,
                        style = LedgerTextStyles.Label,
                        color = LedgerTheme.colors.label,
                    )
                    Text(
                        text = insight.subtitle,
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.secondaryLabel,
                    )
                }
                if (index != state.insights.lastIndex) {
                    LedgerHairline(modifier = Modifier.padding(start = LedgerSpacing.Medium))
                }
            }
        }
    }
}

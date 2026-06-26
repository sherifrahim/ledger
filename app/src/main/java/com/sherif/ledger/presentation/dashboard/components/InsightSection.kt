package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerSectionTitle
import com.sherif.ledger.presentation.dashboard.DashboardUiState

@Composable
fun InsightSection(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
) {

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        LedgerSectionTitle(
            text = "Insights",
        )

        state.insights.forEach {

            LedgerCard {

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {

                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Text(
                        text = it.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                    )

                }

            }

        }

    }

}

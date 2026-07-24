package com.sherif.ledger.feature.analytics.presentation

import com.sherif.ledger.core.designsystem.component.LedgerLinePoint
import com.sherif.ledger.core.designsystem.component.LedgerPieSlice

data class InsightsUiState(
    val spentTotal: String = "0.00",
    val incomeTotal: String = "0.00",
    val dateRange: String = "",
    val currency: String = "AED",
    /** Currency symbol (e.g. "AED", "$") used to label the chart's Y axis compactly. */
    val currencySymbol: String = "AED",
    /** Real daily-spend series for the interactive line chart (labeled + exact-valued). */
    val trend: List<LedgerLinePoint> = emptyList(),
    /** Real category composition for the interactive donut (top 5 + folded "Other"). */
    val pieSlices: List<LedgerPieSlice> = emptyList(),
)

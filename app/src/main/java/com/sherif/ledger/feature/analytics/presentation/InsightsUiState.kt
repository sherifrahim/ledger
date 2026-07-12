package com.sherif.ledger.feature.analytics.presentation

import androidx.compose.ui.graphics.Color

data class InsightsUiState(
    val spentTotal: String = "0.00",
    val incomeTotal: String = "0.00",
    val dateRange: String = "",
    val categories: List<CategoryInsightUi> = emptyList(),
    val currency: String = "AED",
    val chartPoints: List<Float> = emptyList()
)

data class CategoryInsightUi(
    val name: String,
    val amount: String,
    val percentage: String = "",
    val percentageValue: Int = 0,
    val color: Color,
    val currency: String = "AED"
)

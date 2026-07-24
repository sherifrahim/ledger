package com.sherif.ledger.feature.analytics.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.designsystem.component.LedgerLinePoint
import com.sherif.ledger.core.designsystem.component.LedgerPieSlice
import com.sherif.ledger.core.domain.model.CategoryTotal
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.CurrencyRegistry
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.analytics.presentation.InsightsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
) : ViewModel() {

    private val currentMonthRange = run {
        val now = ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999).toInstant()
        start to end
    }

    val uiState: StateFlow<InsightsUiState> = transactionReadSource
        .observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second)
        .map { result ->
            val transactions = (result as? LedgerResult.Success)?.data ?: emptyList()

            if (transactions.isEmpty()) {
                EMPTY_STATE
            } else {
                val analytics = getFinancialAnalyticsUseCase.compute(
                    transactions, currentMonthRange.first, currentMonthRange.second,
                )
                val currency = analytics.currency
                val currencyInfo = CurrencyRegistry.get(currency)
                val divisor = powerOfTen(currencyInfo.decimalDigits)

                InsightsUiState(
                    spentTotal = MoneyFormatter.format(Money(analytics.netSpendMinor, currency), includeSymbol = true),
                    incomeTotal = MoneyFormatter.format(Money(analytics.incomeMinor, currency), includeSymbol = true),
                    dateRange = "This Month",
                    currency = currency.name,
                    currencySymbol = currencyInfo.symbol,
                    trend = analytics.trendPoints.map { point ->
                        LedgerLinePoint(
                            axisLabel = point.label,
                            value = point.amountMinor / divisor,
                            valueLabel = MoneyFormatter.format(Money(point.amountMinor, currency), includeSymbol = true),
                        )
                    },
                    pieSlices = buildPieSlices(analytics.categoryTotals, analytics.netSpendMinor, currency),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EMPTY_STATE
        )

    /**
     * Top five categories individually, everything else folded into a single "Other"
     * slice so the ring and legend always account for the full 100% of spend — never a
     * legend that silently sums to less. Percentages are of net spend, from real totals.
     */
    private fun buildPieSlices(
        categoryTotals: List<CategoryTotal>,
        netSpendMinor: Long,
        currency: CurrencyCode,
    ): List<LedgerPieSlice> {
        if (categoryTotals.isEmpty()) return emptyList()
        fun percentOf(minor: Long): Int = if (netSpendMinor > 0) (minor * 100.0 / netSpendMinor).toInt() else 0

        val shown = categoryTotals.take(5).mapIndexed { index, total ->
            LedgerPieSlice(
                label = total.category.lowercase().replaceFirstChar { it.uppercase() },
                value = total.amountMinor.toFloat().coerceAtLeast(0.5f),
                valueLabel = MoneyFormatter.format(Money(total.amountMinor, currency), includeSymbol = true),
                percent = percentOf(total.amountMinor),
                color = CATEGORY_COLORS[index % CATEGORY_COLORS.size],
            )
        }
        val rest = categoryTotals.drop(5)
        if (rest.isEmpty()) return shown
        val restMinor = rest.sumOf { it.amountMinor }
        return shown + LedgerPieSlice(
            label = "Other",
            value = restMinor.toFloat().coerceAtLeast(0.5f),
            valueLabel = MoneyFormatter.format(Money(restMinor, currency), includeSymbol = true),
            percent = percentOf(restMinor),
            color = OTHER_COLOR,
        )
    }

    private fun powerOfTen(exponent: Int): Float {
        var value = 1f
        repeat(exponent) { value *= 10f }
        return value
    }

    companion object {
        // Soft, desaturated palette — the design language asks for restrained,
        // calm colour, not bright category chips (DESIGN_REFERENCE / P-Analytics).
        private val CATEGORY_COLORS = listOf(
            Color(0xFF7FA183), Color(0xFF7E9CB8), Color(0xFFC9AE7C),
            Color(0xFFB08FA0), Color(0xFF8A98A6), Color(0xFFBE977F),
            Color(0xFF7EA3A1), Color(0xFF9A93B8),
        )
        private val OTHER_COLOR = Color(0xFF9AA0A6)

        private val EMPTY_STATE = InsightsUiState()
    }
}

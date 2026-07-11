package com.sherif.ledger.feature.analytics.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CategoryTotal
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.analytics.presentation.CategoryInsightUi
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
    private val transactionRepository: TransactionRepository,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
) : ViewModel() {

    private val currentMonthRange = run {
        val now = ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999).toInstant()
        start to end
    }

    val uiState: StateFlow<InsightsUiState> = transactionRepository
        .observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second)
        .map { result ->
            val transactions = (result as? LedgerResult.Success)?.data ?: emptyList()
            if (transactions.isEmpty()) {
                EMPTY_STATE
            } else {
                val analytics = getFinancialAnalyticsUseCase.compute(
                    transactions, currentMonthRange.first, currentMonthRange.second,
                )

                InsightsUiState(
                    spentTotal = MoneyFormatter.format(Money(analytics.netSpendMinor, analytics.currency), includeSymbol = true),
                    incomeTotal = MoneyFormatter.format(Money(analytics.incomeMinor, analytics.currency), includeSymbol = true),
                    dateRange = "This Month",
                    categories = analytics.categoryTotals.take(8).mapIndexed { index, total ->
                        toCategoryUi(total, analytics.netSpendMinor, analytics.currency, index)
                    },
                    currency = analytics.currency.name,
                    chartPoints = analytics.trendPoints.map { it.amountMinor / 100f },
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EMPTY_STATE
        )

    private fun toCategoryUi(total: CategoryTotal, netSpendMinor: Long, currency: CurrencyCode, index: Int): CategoryInsightUi {
        val pct = if (netSpendMinor > 0) (total.amountMinor * 100.0 / netSpendMinor) else 0.0
        return CategoryInsightUi(
            name = total.category.lowercase().replaceFirstChar { it.uppercase() },
            amount = MoneyFormatter.format(Money(total.amountMinor, currency), includeSymbol = true),
            percentageValue = pct.toInt(),
            color = CATEGORY_COLORS[index % CATEGORY_COLORS.size],
            currency = currency.name
        )
    }

    companion object {
        private val CATEGORY_COLORS = listOf(
            Color(0xFFFACC15), Color(0xFF10B981), Color(0xFF3B82F6),
            Color(0xFFEF4444), Color(0xFF8B5CF6), Color(0xFFF97316),
            Color(0xFF06B6D4), Color(0xFFEC4899),
        )

        private val EMPTY_STATE = InsightsUiState()
    }
}

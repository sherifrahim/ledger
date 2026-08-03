package com.sherif.ledger.feature.merchant.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.merchant.presentation.CategorySlice
import com.sherif.ledger.feature.merchant.presentation.MerchantUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.absoluteValue
import com.sherif.ledger.core.domain.model.merchantOrRawText

/**
 * Merchant relationship intelligence (P2), computed from the user's real
 * transactions with a merchant. The merchant is identified by the extracted
 * merchant text (`Transaction.rawText`) passed as the `merchantKey` nav argument.
 * Read-only over persisted data — no writes, no Financial-Truth impact.
 */
@HiltViewModel
class MerchantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    transactionReadSource: TransactionReadSource,
    private val analytics: GetFinancialAnalyticsUseCase,
) : ViewModel() {

    private val merchantKey: String = savedStateHandle.get<String>("merchantKey").orEmpty()

    val uiState: StateFlow<MerchantUiState> = transactionReadSource.observeAllTransactions()
        .map { result -> build(result) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MerchantUiState(name = merchantKey))

    private fun build(result: LedgerResult<List<Transaction>>): MerchantUiState {
        val all = (result as? LedgerResult.Success)?.data ?: emptyList()
        val mine = all.filter { it.merchantOrRawText?.trim().equals(merchantKey.trim(), ignoreCase = true) }
        if (mine.isEmpty()) return MerchantUiState(name = prettify(merchantKey), loaded = true)

        val currency: CurrencyCode = mine.first().amount.currencyCode
        fun money(minor: Long) = MoneyFormatter.format(Money(minor, currency), includeSymbol = false)

        val spendMinor = mine.filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits.absoluteValue }
        val largestMinor = mine.maxOf { it.amount.minorUnits.absoluteValue }
        val firstSeen = mine.minByOrNull { it.timestamp }!!.timestamp
        val monthsSpanned = (ChronoUnit.MONTHS.between(
            firstSeen.atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1),
            java.time.LocalDate.now().withDayOfMonth(1),
        ) + 1).coerceAtLeast(1)
        val avgMonthlyMinor = spendMinor / monthsSpanned

        // Real category mix, via the analytics story layer.
        val stories = analytics.transactionStories(all)
        val byCatMinor = mine.groupBy { stories[it.id]?.category ?: "Uncategorized" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount.minorUnits.absoluteValue } }
        val catTotal = byCatMinor.values.sum().coerceAtLeast(1)
        val categories = byCatMinor.entries.sortedByDescending { it.value }.take(3)
            .map { CategorySlice(prettify(it.key), it.value.toFloat() / catTotal) }

        val topCatRaw = byCatMinor.maxByOrNull { it.value }?.key
        val related = all.asSequence()
            .filter { it.merchantOrRawText?.trim()?.equals(merchantKey.trim(), ignoreCase = true) == false }
            .filter { topCatRaw == null || stories[it.id]?.category == topCatRaw }
            .mapNotNull { it.merchantOrRawText?.trim() }
            .filter { it.isNotBlank() }
            .distinct().take(4).map { prettify(it) }.toList()

        return MerchantUiState(
            name = prettify(merchantKey),
            since = "Since " + firstSeen.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM yyyy")),
            totalSpent = money(spendMinor),
            txCount = mine.size,
            avgMonthly = money(avgMonthlyMinor),
            largest = money(largestMinor),
            currency = currency.name,
            insights = buildInsights(mine.size, money(largestMinor), currency.name, categories.firstOrNull()?.label),
            categories = categories,
            related = related,
            loaded = true,
        )
    }

    private fun buildInsights(count: Int, largest: String, currency: String, topCategory: String?): List<String> =
        buildList {
            add(if (count == 1) "1 transaction so far" else "$count transactions so far")
            topCategory?.let { add("Mostly $it") }
            add("Largest was $currency $largest")
        }

    /** Turn a raw merchant/category token into a display label. */
    private fun prettify(raw: String): String = raw.trim()
        .lowercase()
        .split(Regex("\\s+"))
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        .ifBlank { "Merchant" }
}

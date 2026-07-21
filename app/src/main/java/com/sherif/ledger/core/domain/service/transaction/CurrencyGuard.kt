package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.model.CurrencyCode

/**
 * RC7 Phase C. A single, reusable "never sum across currencies" primitive —
 * replacing three independently-written copies of the same unguarded pattern
 * found during RC7's audit ([AccountBalanceService.netWorth],
 * [com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase.computeNetWorth],
 * [com.sherif.ledger.core.domain.service.diagnostic.FinancialTraceCollector.buildReport]):
 * each summed raw minor units across every account regardless of currency,
 * then stamped the result with whichever currency the FIRST account in the
 * list happened to have. If an AED and an INR account ever coexisted, this
 * silently produced a nonsense total with no warning — the same class of bug
 * [BalanceCalculator.effect] already guards per-transaction (RC6), just never
 * closed at the aggregation layer.
 *
 * No exchange-rate conversion happens here or anywhere in this codebase (an
 * explicit RC7 constraint) — a non-primary-currency balance is reported
 * separately, in its own currency, never converted or added into the primary
 * figure. "Primary currency" is chosen deterministically (the currency shared
 * by the most items — ties broken by whichever currency appears first), never
 * hardcoded to AED, so this works correctly for a user whose accounts are
 * predominantly a different currency too.
 */
object CurrencyGuard {

    data class GroupedTotals(
        val primaryCurrency: CurrencyCode,
        val primaryTotalMinor: Long,
        /** Non-primary-currency totals, kept separate — never summed into [primaryTotalMinor]. */
        val otherCurrencyTotals: Map<CurrencyCode, Long>,
        /** How many items contributed to each non-primary currency's bucket — for a human-readable "why is this excluded" explanation. */
        val otherCurrencyItemCounts: Map<CurrencyCode, Int>,
        val fallbackCurrency: CurrencyCode,
    )

    fun <T> groupAndSum(
        items: List<T>,
        currencyOf: (T) -> CurrencyCode,
        amountOf: (T) -> Long,
        fallbackCurrency: CurrencyCode = CurrencyCode.AED,
    ): GroupedTotals {
        if (items.isEmpty()) {
            return GroupedTotals(fallbackCurrency, 0L, emptyMap(), emptyMap(), fallbackCurrency)
        }
        val byCurrency = items.groupBy(currencyOf)
        val primary = byCurrency.entries
            .sortedByDescending { it.value.size }
            .first().key
        val primaryTotal = byCurrency[primary].orEmpty().sumOf(amountOf)
        val others = byCurrency.filterKeys { it != primary }
        return GroupedTotals(
            primaryCurrency = primary,
            primaryTotalMinor = primaryTotal,
            otherCurrencyTotals = others.mapValues { (_, list) -> list.sumOf(amountOf) },
            otherCurrencyItemCounts = others.mapValues { (_, list) -> list.size },
            fallbackCurrency = fallbackCurrency,
        )
    }
}

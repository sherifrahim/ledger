package com.sherif.ledger.feature.accounts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.accounts.presentation.AccountSectionUi
import com.sherif.ledger.feature.accounts.presentation.AccountUi
import com.sherif.ledger.feature.accounts.presentation.AccountsUiState
import com.sherif.ledger.presentation.dashboard.InsightUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Phase 9: every figure here — net worth, assets, liabilities, each account's
 * balance — is [GetFinancialAnalyticsUseCase.computeNetWorth], which replays
 * persisted transactions via AccountBalanceService. Nothing here reads a stored
 * balance field or classifies assets/liabilities by balance sign; classification
 * is [com.sherif.ledger.core.domain.model.AccountType.isLiability].
 */
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
) : ViewModel() {

    init {
        com.sherif.ledger.core.common.logging.LedgerLogger.d("EXECUTING: AccountsViewModel")
    }

    private val currentMonthRange = run {
        val now = java.time.ZonedDateTime.now()
        val start = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant()
        val end = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999).toInstant()
        start to end
    }

    // The month-transactions flow is only a TRIGGER for "when should this
    // recompute" (reusing the existing repository stream, no new query) — the
    // actual balance/net-worth figures always come from computeNetWorth(), a full
    // replay, never from anything cached in this flow's emitted list.
    val uiState: StateFlow<AccountsUiState> = transactionReadSource
        .observeTransactionsBetween(currentMonthRange.first, currentMonthRange.second)
        .map { monthResult -> buildUiState(monthResult) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EMPTY_STATE
        )

    private suspend fun buildUiState(monthResult: LedgerResult<List<com.sherif.ledger.core.domain.model.Transaction>>): AccountsUiState {
        val netWorth = getFinancialAnalyticsUseCase.computeNetWorth()

        val accounts = netWorth.accountBalances.map { summary ->
            AccountUi(
                id = summary.accountId.toString(),
                name = summary.accountName,
                subtitle = summary.accountType.name,
                balance = MoneyFormatter.format(Money(summary.balanceMinor, summary.currencyCode), includeSymbol = false),
                currency = summary.currencyCode.name,
                isNegative = if (summary.isLiability) summary.balanceMinor > 0 else summary.balanceMinor < 0,
            )
        }
        // Read the currency-scoped figures the use case already computes; do NOT
        // re-sum accountBalances here. That list carries every account regardless of
        // currency and AccountBalanceSummary does not record which currency each one
        // is in, so summing it added a USD balance into an AED total and labelled
        // the result AED — the owner's Assets read AED 1,521.53, which is
        // 1,568.52 AED with -46.99 USD subtracted from it as though the two were the
        // same unit. This is the same cross-currency corruption RC7 Phase C fixed
        // inside computeNetWorth; it survived one layer up because this screen
        // derived its own totals instead of using that one's.
        val assetsUnits = netWorth.cashBalanceMinor
        val liabilitiesUnits = netWorth.cardDebtMinor

        val monthTransactions = (monthResult as? LedgerResult.Success)?.data ?: emptyList()
        val analytics = getFinancialAnalyticsUseCase.compute(monthTransactions, currentMonthRange.first, currentMonthRange.second)
        val insight = if (monthTransactions.size >= 5) {
            val spendFormatted = MoneyFormatter.format(
                Money(analytics.netSpendMinor, analytics.currency), includeSymbol = true,
            )
            InsightUiModel(
                title = "This month's spending",
                subtitle = "$spendFormatted across ${analytics.categoryTotals.size} categories this month.",
                indicator = ""
            )
        } else null

        return AccountsUiState(
            netWorth = MoneyFormatter.format(Money(netWorth.netWorthMinor, netWorth.currency), includeSymbol = false),
            netWorthIsNegative = netWorth.netWorthMinor < 0,
            netWorthCurrency = netWorth.currency.name,
            assetsTotal = MoneyFormatter.format(Money(assetsUnits, netWorth.currency), includeSymbol = false),
            liabilitiesTotal = MoneyFormatter.format(Money(liabilitiesUnits, netWorth.currency), includeSymbol = false),
            sections = if (accounts.isNotEmpty()) listOf(
                AccountSectionUi(
                    "My Accounts",
                    MoneyFormatter.format(Money(netWorth.netWorthMinor, netWorth.currency), includeSymbol = false),
                    accounts,
                )
            ) else emptyList(),
            insight = insight,
        )
    }

    companion object {
        private val EMPTY_STATE = AccountsUiState(
            netWorth = "0",
            netWorthCurrency = "AED",
            assetsTotal = "0",
            liabilitiesTotal = "0",
            sections = emptyList(),
            insight = null
        )
    }
}


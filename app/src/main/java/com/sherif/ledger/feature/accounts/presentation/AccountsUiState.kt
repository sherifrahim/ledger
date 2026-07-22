package com.sherif.ledger.feature.accounts.presentation

import com.sherif.ledger.presentation.dashboard.InsightUiModel

data class AccountsUiState(
    val netWorth: String,
    // MoneyFormatter renders a magnitude only; net worth can be negative (liabilities
    // exceed assets), so the sign is carried here — same pattern as the Dashboard hero.
    val netWorthIsNegative: Boolean = false,
    val netWorthCurrency: String,
    val assetsTotal: String,
    val liabilitiesTotal: String,
    val sections: List<AccountSectionUi>,
    val insight: com.sherif.ledger.presentation.dashboard.InsightUiModel? = null,
)

data class AccountSectionUi(
    val title: String,
    val total: String,
    val accounts: List<AccountUi>,
)

data class AccountUi(
    val id: String,
    val name: String,
    val subtitle: String,
    val balance: String,
    val isNegative: Boolean = false,
    val accentHue: Long = 0xFF6E6E6E,
    val accountNumber: String = "",
    val lastActivity: String = "",
)

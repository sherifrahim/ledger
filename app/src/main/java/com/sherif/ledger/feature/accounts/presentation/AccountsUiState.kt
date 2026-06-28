package com.sherif.ledger.feature.accounts.presentation

data class AccountsUiState(
    val netWorth: String,
    val netWorthCurrency: String,
    val assetsTotal: String,
    val liabilitiesTotal: String,
    val sections: List<AccountSectionUi>,
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

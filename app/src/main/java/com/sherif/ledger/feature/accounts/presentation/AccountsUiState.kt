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
    /**
     * The currency THIS account is denominated in — not the screen's primary one.
     * The row used to be labelled with the net-worth currency, which printed the
     * owner's USD account as "AED -46.99".
     */
    val currency: String = "AED",
    val isNegative: Boolean = false,
    val accentHue: Long = 0xFF6E6E6E,
    val accountNumber: String = "",
    val lastActivity: String = "",
    /**
     * Last digits of the account or card, when the bank quoted them. Without this
     * the owner's two ADCB accounts render as two identical rows called "ADCB
     * Account", which is indistinguishable from a duplicate. Placed last because
     * preview data constructs this positionally.
     */
    val accountTail: String? = null,
)

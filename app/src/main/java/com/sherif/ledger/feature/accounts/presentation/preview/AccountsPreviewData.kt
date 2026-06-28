package com.sherif.ledger.feature.accounts.presentation.preview

import com.sherif.ledger.feature.accounts.presentation.AccountSectionUi
import com.sherif.ledger.feature.accounts.presentation.AccountUi
import com.sherif.ledger.feature.accounts.presentation.AccountsUiState

object AccountsPreviewData {

    val state = AccountsUiState(
        netWorth = "47,250.75",
        netWorthCurrency = "AED",
        assetsTotal = "AED 47,250.75",
        liabilitiesTotal = "AED 0.00",
        sections = listOf(
            AccountSectionUi(
                title = "Assets",
                total = "AED 47,250.75",
                accounts = listOf(
                    AccountUi("1", "ADCB", "Current Account", "AED 32,450.50", accentHue = 0xFF2563EB, accountNumber = "\u2022\u20224521", lastActivity = "Today"),
                    AccountUi("2", "FAB", "Savings Account", "AED 8,200.25", accentHue = 0xFF7C3AED, accountNumber = "\u2022\u20227803", lastActivity = "Yesterday"),
                    AccountUi("3", "Wio", "Digital Account", "AED 5,100.00", accentHue = 0xFF0D9488, accountNumber = "\u2022\u20221190", lastActivity = "Jun 24"),
                    AccountUi("4", "Cash Wallet", "Cash", "AED 1,500.00", accentHue = 0xFF78716C, accountNumber = "", lastActivity = ""),
                ),
            ),
            AccountSectionUi(
                title = "Liabilities",
                total = "AED 0.00",
                accounts = listOf(
                    AccountUi("5", "ADCB Touchpoints", "Credit Card", "AED 0.00", accentHue = 0xFFDC2626, accountNumber = "\u2022\u20228847", lastActivity = "Jun 20"),
                ),
            ),
        ),
    )
}

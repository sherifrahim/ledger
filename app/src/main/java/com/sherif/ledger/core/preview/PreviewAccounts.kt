package com.sherif.ledger.core.preview

data class PreviewAccount(
    val id: String,
    val name: String,
    val subtitle: String,
    val balance: String,
    val accountNumber: String,
    val accentHue: Long,
)

object PreviewAccounts {
    val adcb = PreviewAccount("a_adcb", "ADCB", "Current Account", "AED 32,450.50", "\u2022\u20224521", 0xFF2563EB)
    val fab = PreviewAccount("a_fab", "FAB", "Savings Account", "AED 8,200.25", "\u2022\u20227803", 0xFF7C3AED)
    val wio = PreviewAccount("a_wio", "Wio", "Digital Account", "AED 5,100.00", "\u2022\u20221190", 0xFF0D9488)
    val cash = PreviewAccount("a_cash", "Cash Wallet", "Cash", "AED 1,500.00", "", 0xFF78716C)
    val creditCard = PreviewAccount("a_cc", "ADCB Touchpoints", "Credit Card", "AED 0.00", "\u2022\u20228847", 0xFFDC2626)
}

package com.sherif.ledger.core.preview

/**
 * Canonical transaction samples for preview data.
 *
 * Each transaction references a [PreviewMerchant] and [PreviewAccount]
 * so data stays consistent across Dashboard, Transactions, Transaction
 * Details, and future screens.
 */
data class PreviewTransaction(
    val id: String,
    val merchant: PreviewMerchant,
    val account: PreviewAccount,
    val amount: String,
    val isIncome: Boolean = false,
    val date: String,
    val time: String,
    val status: String = "Posted",
    val paymentMethod: String = "Debit Card",
    val reference: String = "",
    val isRecurring: Boolean = false,
)

object PreviewTransactions {
    val amazonToday = PreviewTransaction("t1", PreviewMerchants.amazon, PreviewAccounts.adcb, "52", date = "27 Jun 2026", time = "2:14 PM", reference = "TXN-2026-4851")
    val carrefourToday = PreviewTransaction("t2", PreviewMerchants.carrefour, PreviewAccounts.adcb, "126", date = "27 Jun 2026", time = "11:02 AM", reference = "TXN-2026-4850")
    val costaToday = PreviewTransaction("t3", PreviewMerchants.costaCoffee, PreviewAccounts.wio, "19", date = "27 Jun 2026", time = "9:30 AM", paymentMethod = "Apple Pay", reference = "TXN-2026-4849")
    val adnocYesterday = PreviewTransaction("t4", PreviewMerchants.adnoc, PreviewAccounts.adcb, "120", date = "26 Jun 2026", time = "3:15 PM", reference = "TXN-2026-4842")
    val noonPending = PreviewTransaction("t5", PreviewMerchants.noon, PreviewAccounts.adcb, "245", date = "26 Jun 2026", time = "1:20 PM", status = "Pending", reference = "TXN-2026-4841")
    val uberYesterday = PreviewTransaction("t6", PreviewMerchants.uber, PreviewAccounts.wio, "32", date = "26 Jun 2026", time = "8:45 AM", reference = "TXN-2026-4838")
    val salaryMonday = PreviewTransaction("t7", PreviewMerchants.salary, PreviewAccounts.fab, "9,500", isIncome = true, date = "23 Jun 2026", time = "Bank Transfer", paymentMethod = "Bank Transfer", reference = "SAL-2026-06")
    val netflixMonday = PreviewTransaction("t8", PreviewMerchants.netflix, PreviewAccounts.adcb, "55", date = "23 Jun 2026", time = "12:00 AM", isRecurring = true, reference = "TXN-2026-4820")
    val duMonday = PreviewTransaction("t9", PreviewMerchants.du, PreviewAccounts.adcb, "399", date = "23 Jun 2026", time = "12:00 AM", isRecurring = true, reference = "TXN-2026-4819")

    val all = listOf(amazonToday, carrefourToday, costaToday, adnocYesterday, noonPending, uberYesterday, salaryMonday, netflixMonday, duMonday)
}

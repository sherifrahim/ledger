package com.sherif.ledger.feature.transactions.presentation.preview

import com.sherif.ledger.feature.transactions.presentation.*

object TransactionsPreviewData {

    val state = TransactionsUiState(
        listOf(

            TransactionGroupUi(
                "today",
                "Today",
                DaySummaryUi(
                    "AED 197.00",
                    "AED 0.00",
                    3,
                    MerchantCategory.Grocery
                ),
                listOf(
                    TransactionUi("amazon", "Amazon", MerchantCategory.Shopping, "52.00", "10:45 AM"),
                    TransactionUi("careem", "Careem", MerchantCategory.Transport, "25.00", "9:15 AM"),
                    TransactionUi("salary", "Salary", MerchantCategory.Salary, "5,200.00", "9:00 AM"),
                ),
            ),

            TransactionGroupUi(
                "yesterday",
                "Yesterday",
                DaySummaryUi("AED 210.00", "AED 0.00", 3, MerchantCategory.Grocery),
                listOf(
                    TransactionUi("carrefour", "Carrefour", MerchantCategory.Grocery, "126.00", "9:32 AM"),
                    TransactionUi("costa", "Costa Coffee", MerchantCategory.Coffee, "19.00", "6:21 PM"),
                    TransactionUi("enoc", "Enoc", MerchantCategory.Fuel, "65.00", "12:11 PM"),
                ),
            ),

            TransactionGroupUi(
                "2026-06-24",
                "Jun 24, Tue",
                DaySummaryUi("AED 42.00", "AED 850.00", 2, MerchantCategory.Salary),
                listOf(
                    TransactionUi("netflix", "Netflix", MerchantCategory.Entertainment, "42.00", "9:45 PM"),
                    TransactionUi("freelance", "Freelance Work", MerchantCategory.Salary, "850.00", "2:30 PM"),
                ),
            ),
        ),
    )
}

package com.sherif.ledger.feature.transactions.presentation.preview

import com.sherif.ledger.feature.transactions.presentation.*
import java.math.BigDecimal

object TransactionsPreviewData {

    val state = TransactionsUiState(
        listOf(

            TransactionGroupUi(
                "today",
                "Today",
                DaySummaryUi(
                    BigDecimal("197"),
                    BigDecimal.ZERO,
                    3,
                    MerchantCategory.Grocery
                ),
                listOf(
                    TransactionUi("amazon", "Amazon", MerchantCategory.Shopping, BigDecimal("52"), "10:45 AM"),
                    TransactionUi("careem", "Careem", MerchantCategory.Transport, BigDecimal("25"), "9:15 AM"),
                    TransactionUi("salary", "Salary", MerchantCategory.Salary, BigDecimal("5200"), "9:00 AM"),
                ),
            ),

            TransactionGroupUi(
                "yesterday",
                "Yesterday",
                DaySummaryUi(BigDecimal("210"), BigDecimal.ZERO, 3, MerchantCategory.Grocery),
                listOf(
                    TransactionUi("carrefour", "Carrefour", MerchantCategory.Grocery, BigDecimal("126"), "9:32 AM"),
                    TransactionUi("costa", "Costa Coffee", MerchantCategory.Coffee, BigDecimal("19"), "6:21 PM"),
                    TransactionUi("enoc", "Enoc", MerchantCategory.Fuel, BigDecimal("65"), "12:11 PM"),
                ),
            ),

            TransactionGroupUi(
                "2026-06-24",
                "Jun 24, Tue",
                DaySummaryUi(BigDecimal("42"), BigDecimal("850"), 2, MerchantCategory.Salary),
                listOf(
                    TransactionUi("netflix", "Netflix", MerchantCategory.Entertainment, BigDecimal("42"), "9:45 PM"),
                    TransactionUi("freelance", "Freelance Work", MerchantCategory.Salary, BigDecimal("850"), "2:30 PM"),
                ),
            ),
        ),
    )
}

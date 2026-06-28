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
                    TransactionUi(
                        "amazon",
                        "Amazon",
                        MerchantCategory.Shopping,
                        BigDecimal("52"),
                        "2:14 PM"
                    ),
                    TransactionUi(
                        "carrefour",
                        "Carrefour",
                        MerchantCategory.Grocery,
                        BigDecimal("126"),
                        "11:02 AM"
                    ),
                    TransactionUi(
                        "costa",
                        "Costa Coffee",
                        MerchantCategory.Coffee,
                        BigDecimal("19"),
                        "9:30 AM"
                    ),
                ),
            ),

            TransactionGroupUi(
                "yesterday",
                "Yesterday",
                DaySummaryUi(
                    BigDecimal("397"),
                    BigDecimal.ZERO,
                    3,
                    MerchantCategory.Fuel
                ),
                listOf(
                    TransactionUi(
                        "adnoc",
                        "ADNOC",
                        MerchantCategory.Fuel,
                        BigDecimal("120"),
                        "3:15 PM"
                    ),
                    TransactionUi(
                        "noon",
                        "Noon",
                        MerchantCategory.Shopping,
                        BigDecimal("245"),
                        "1:20 PM",
                        TransactionState.Pending
                    ),
                    TransactionUi(
                        "uber",
                        "Uber",
                        MerchantCategory.Transport,
                        BigDecimal("32"),
                        "8:45 AM"
                    ),
                ),
            ),

            TransactionGroupUi(
                "2026-06-23",
                "23 Jun",
                DaySummaryUi(
                    BigDecimal("454"),
                    BigDecimal("9500"),
                    3,
                    MerchantCategory.Salary
                ),
                listOf(
                    TransactionUi(
                        "salary",
                        "Salary",
                        MerchantCategory.Salary,
                        BigDecimal("9500"),
                        "Bank transfer"
                    ),
                    TransactionUi(
                        "netflix",
                        "Netflix",
                        MerchantCategory.Entertainment,
                        BigDecimal("55"),
                        "Monthly",
                        TransactionState.Recurring
                    ),
                    TransactionUi(
                        "du",
                        "du",
                        MerchantCategory.Bills,
                        BigDecimal("399"),
                        "Monthly",
                        TransactionState.Recurring
                    ),
                ),
            ),
        ),
    )
}

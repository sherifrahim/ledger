package com.sherif.ledger.feature.transactions.presentation.detail

data class TransactionDetailsUiState(
    val merchant: String,
    val merchantCategory: String,
    val merchantAccentHue: Long,
    val amount: String,
    val sign: String,
    val isIncome: Boolean,
    val date: String,
    val time: String,
    val status: String,
    val paymentMethod: String,
    val accountName: String,
    val accountNumber: String,
    val reference: String,
    val history: List<MerchantHistoryItem>,
    val notes: String? = null,
)

data class MerchantHistoryItem(
    val amount: String,
    val date: String,
    val isIncome: Boolean = false,
)

package com.sherif.ledger.feature.capture.parsing.validation

import kotlinx.serialization.Serializable

@Serializable
data class ParserFixture(
    val id: String,
    val bank: String,
    val source: String,
    val type: String,
    val shouldParse: Boolean,
    val raw: String,
    val expected: ExpectedResult? = null
)

@Serializable
data class ExpectedResult(
    val amount: Long? = null,
    val currency: String? = null,
    val merchant: String? = null,
    val canonicalMerchant: String? = null,
    val transactionType: String? = null,
    val accountHint: String? = null,
    val timestamp: String? = null,
    val category: String? = null
)

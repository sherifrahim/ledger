package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a merchant.
 */
@JvmInline
value class MerchantId(val value: String) {
    init {
        require(value.isNotBlank()) { "Merchant id cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * Strongly typed display name for a merchant.
 */
@JvmInline
value class MerchantName(val value: String) {
    init {
        require(value.isNotBlank()) { "Merchant name cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * Domain merchant recognized or entered for a transaction.
 */
data class Merchant(
    val id: MerchantId,
    val name: MerchantName,
)

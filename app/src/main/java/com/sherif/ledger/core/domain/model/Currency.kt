package com.sherif.ledger.core.domain.model

/**
 * Supported ISO currency codes for Ledger.
 */
enum class CurrencyCode {
    AED,
    INR
}

/**
 * Domain representation of currency metadata.
 */
data class Currency(
    val code: CurrencyCode,
    val symbol: String,
    val decimalDigits: Int,
)

/**
 * Authority for currency properties. 
 * Prevents arbitrary currency creation and ensures formatting metadata is consistent.
 */
object CurrencyRegistry {
    private val currencies = mapOf(
        CurrencyCode.AED to Currency(CurrencyCode.AED, "AED", 2),
        CurrencyCode.INR to Currency(CurrencyCode.INR, "₹", 2)
    )

    fun get(code: CurrencyCode): Currency = currencies[code] 
        ?: throw IllegalArgumentException("Currency code $code not registered.")

    fun get(codeString: String): Currency = try {
        get(CurrencyCode.valueOf(codeString.uppercase()))
    } catch (e: Exception) {
        throw IllegalArgumentException("Unsupported currency code: $codeString")
    }
}

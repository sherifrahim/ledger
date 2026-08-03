package com.sherif.ledger.core.domain.model

/**
 * Supported ISO currency codes for Ledger.
 */
enum class CurrencyCode {
    AED,
    INR,

    // Foreign currencies observed in real captured messages (card used abroad or
    // with an overseas merchant). Representing them honestly matters: previously an
    // unrepresentable currency caused extraction to skip the transacted amount and
    // record the message's AED *balance* instead — e.g. a KZT purchase booked as an
    // "AED 8,225.16" charge. A transaction in a currency other than its account's is
    // still recorded, but BalanceCalculator's currency guard contributes zero to the
    // balance (the converted home-currency figure isn't stated in the message).
    USD,
    EUR,
    GBP,
    SAR,
    KZT,
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
        CurrencyCode.INR to Currency(CurrencyCode.INR, "₹", 2),
        CurrencyCode.USD to Currency(CurrencyCode.USD, "$", 2),
        CurrencyCode.EUR to Currency(CurrencyCode.EUR, "€", 2),
        CurrencyCode.GBP to Currency(CurrencyCode.GBP, "£", 2),
        CurrencyCode.SAR to Currency(CurrencyCode.SAR, "SAR", 2),
        CurrencyCode.KZT to Currency(CurrencyCode.KZT, "₸", 2),
    )

    fun get(code: CurrencyCode): Currency = currencies[code] 
        ?: throw IllegalArgumentException("Currency code $code not registered.")

    fun get(codeString: String): Currency = try {
        get(CurrencyCode.valueOf(codeString.uppercase()))
    } catch (e: Exception) {
        throw IllegalArgumentException("Unsupported currency code: $codeString")
    }
}

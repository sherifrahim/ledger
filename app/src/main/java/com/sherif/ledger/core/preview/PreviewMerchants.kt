package com.sherif.ledger.core.preview

/**
 * Canonical merchant catalog for preview data across all features.
 *
 * Every feature that displays a merchant should reference these objects
 * so the same merchant always carries the same name, category, and color.
 */
data class PreviewMerchant(
    val id: String,
    val name: String,
    val category: String,
    val accentHue: Long,
)

object PreviewMerchants {
    val amazon = PreviewMerchant("m_amazon", "Amazon", "Shopping", 0xFFA855F7)
    val carrefour = PreviewMerchant("m_carrefour", "Carrefour", "Groceries", 0xFF22C55E)
    val lulu = PreviewMerchant("m_lulu", "Lulu Hypermarket", "Groceries", 0xFF22C55E)
    val costaCoffee = PreviewMerchant("m_costa", "Costa Coffee", "Coffee", 0xFF92400E)
    val adnoc = PreviewMerchant("m_adnoc", "ADNOC", "Fuel", 0xFFEF4444)
    val noon = PreviewMerchant("m_noon", "Noon", "Shopping", 0xFFA855F7)
    val uber = PreviewMerchant("m_uber", "Uber", "Transport", 0xFF3B82F6)
    val careem = PreviewMerchant("m_careem", "Careem", "Transport", 0xFF3B82F6)
    val netflix = PreviewMerchant("m_netflix", "Netflix", "Entertainment", 0xFFEC4899)
    val spotify = PreviewMerchant("m_spotify", "Spotify", "Entertainment", 0xFFEC4899)
    val appleOne = PreviewMerchant("m_apple", "Apple One", "Technology", 0xFF6B7280)
    val du = PreviewMerchant("m_du", "du", "Bills", 0xFFEAB308)
    val etisalat = PreviewMerchant("m_etisalat", "Etisalat", "Bills", 0xFFEAB308)
    val zomato = PreviewMerchant("m_zomato", "Zomato", "Food", 0xFFF97316)
    val salary = PreviewMerchant("m_salary", "Salary", "Income", 0xFF047857)

    val all = listOf(amazon, carrefour, lulu, costaCoffee, adnoc, noon, uber, careem,
        netflix, spotify, appleOne, du, etisalat, zomato, salary)
}

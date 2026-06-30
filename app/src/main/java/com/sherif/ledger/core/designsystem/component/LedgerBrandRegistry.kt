package com.sherif.ledger.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ledger Identity System Types.
 */
enum class LedgerIdentityType {
    Merchant, Bank, Card, Category, Action
}

/**
 * Authentic Brand Identity model for Ledger.
 */
data class BrandIdentity(
    val icon: ImageVector? = null,
    val color: Color? = null,
    val monogram: String? = null,
    val backgroundColor: Color? = null,
)

/**
 * Global registry for Ledger brand identities.
 */
object LedgerBrandRegistry {
    private val registry = mutableMapOf<String, BrandIdentity>()

    init {
        // --- 1. MERCHANTS ---
        register("amazon", BrandIdentity(icon = Icons.Filled.ShoppingBag, color = Color(0xFFFF9900), monogram = "a", backgroundColor = Color.Black))
        register("apple", BrandIdentity(icon = Icons.Filled.Smartphone, color = Color.White, backgroundColor = Color.Black))
        register("google", BrandIdentity(icon = Icons.Filled.Smartphone, color = Color(0xFF4285F4)))
        register("netflix", BrandIdentity(monogram = "N", color = Color(0xFFE50914), backgroundColor = Color.Black))
        register("spotify", BrandIdentity(icon = Icons.Filled.Smartphone, color = Color(0xFF1DB954), backgroundColor = Color.Black))
        register("carrefour", BrandIdentity(icon = Icons.Filled.LocalGroceryStore, color = Color(0xFF0066BE), backgroundColor = Color.White))
        register("costa", BrandIdentity(icon = Icons.Filled.Coffee, color = Color.White, backgroundColor = Color(0xFF630821)))
        register("starbucks", BrandIdentity(icon = Icons.Filled.Coffee, color = Color(0xFF00704A), backgroundColor = Color.White))
        register("uber", BrandIdentity(monogram = "U", color = Color.White, backgroundColor = Color.Black))
        register("careem", BrandIdentity(monogram = "C", color = Color(0xFF47D366), backgroundColor = Color.White))

        // --- 2. BANKS ---
        register("adcb", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFE21E26), backgroundColor = Color.White))
        register("fab", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF003865), backgroundColor = Color.White))
        register("wio", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFCCFF00), backgroundColor = Color.Black))
        register("hsbc", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFDB0011), backgroundColor = Color.White))

        // --- 3. CARDS ---
        register("visa", BrandIdentity(icon = Icons.Filled.CreditCard, color = Color(0xFF1A1F71), backgroundColor = Color.White))
        register("mastercard", BrandIdentity(icon = Icons.Filled.CreditCard, color = Color(0xFFEB001B), backgroundColor = Color.White))
        register("amex", BrandIdentity(icon = Icons.Filled.CreditCard, color = Color(0xFF016FD0), backgroundColor = Color.White))

        // --- 4. CATEGORIES ---
        register("salary", BrandIdentity(icon = Icons.Filled.Payments, color = Color(0xFF10B981)))
        register("groceries", BrandIdentity(icon = Icons.Filled.LocalGroceryStore))
        register("dining", BrandIdentity(icon = Icons.Filled.Restaurant))
        register("shopping", BrandIdentity(icon = Icons.Filled.ShoppingBag))
        register("transfer", BrandIdentity(icon = Icons.AutoMirrored.Filled.Send))
        register("cash", BrandIdentity(icon = Icons.Filled.Wallet))
        register("cash", BrandIdentity(icon = Icons.Filled.Wallet, color = Color(0xFF10B981), backgroundColor = Color.White))
    }

    fun register(key: String, identity: BrandIdentity) {
        registry[key.lowercase().trim()] = identity
    }

    fun resolve(name: String, type: LedgerIdentityType): BrandIdentity {
        val normalized = name.lowercase().trim()
        
        // Match specific registry entry
        registry[normalized]?.let { return it }
        
        // Partial match
        registry.entries.find { normalized.contains(it.key) }?.value?.let { return it }

        // Type-based defaults
        return when (type) {
            LedgerIdentityType.Bank -> BrandIdentity(icon = Icons.Filled.AccountBalance)
            LedgerIdentityType.Card -> BrandIdentity(icon = Icons.Filled.CreditCard)
            LedgerIdentityType.Category -> resolveCategory(normalized)
            LedgerIdentityType.Action -> if (normalized.contains("add")) BrandIdentity(icon = Icons.Filled.Add) else BrandIdentity()
            else -> BrandIdentity()
        }
    }

    private fun resolveCategory(name: String): BrandIdentity {
        if (name.contains("food") || name.contains("dining")) return BrandIdentity(icon = Icons.Filled.Restaurant)
        if (name.contains("grocer")) return BrandIdentity(icon = Icons.Filled.LocalGroceryStore)
        if (name.contains("shop")) return BrandIdentity(icon = Icons.Filled.ShoppingBag)
        if (name.contains("salary")) return BrandIdentity(icon = Icons.Filled.Payments, color = Color(0xFF10B981))
        return BrandIdentity()
    }
}

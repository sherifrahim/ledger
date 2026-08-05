package com.sherif.ledger.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
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
    val painter: Painter? = null,
    val color: Color? = null,
    val monogram: String? = null,
    val backgroundColor: Color? = null,
)

/**
 * Global registry for Ledger brand identities.
 *
 * A bank/merchant with no entry here — real trademarked logos aren't
 * something this app can ship without licensing them — still gets an honest
 * fallback: a bank gets the generic institution glyph rather than a bare
 * initial, and an unrecognised merchant is matched against its OWN NAME for
 * a category-shaped icon (a taxi company gets a car, a "mart" gets a grocery
 * cart) rather than falling straight to a random letter in a circle, which
 * is what every unregistered row looked like before this. See [resolve].
 */
object LedgerBrandRegistry {
    private val registry = mutableMapOf<String, BrandIdentity>()

    init {
        // --- 1. MERCHANTS ---
        register("amazon", BrandIdentity(icon = Icons.Filled.ShoppingBag, color = Color(0xFFFF9900), monogram = "a"))
        register("apple", BrandIdentity(icon = Icons.Filled.Smartphone))
        register("google", BrandIdentity(icon = Icons.Filled.Smartphone, color = Color(0xFF4285F4)))
        register("netflix", BrandIdentity(monogram = "N", color = Color(0xFFE50914)))
        register("spotify", BrandIdentity(icon = Icons.Filled.Smartphone, color = Color(0xFF1DB954)))
        register("carrefour", BrandIdentity(icon = Icons.Filled.LocalGroceryStore, color = Color(0xFF0066BE)))
        // Design review finding F3 (2026-08-06): both fell through to the generic
        // pale-blue letter fallback (resolveMerchantByKeyword only matches
        // "mart"/"grocer"/"food"/"restaurant"-shaped substrings, and neither name
        // contains one) despite being two of the most common UAE merchants this
        // app actually captures — visibly inconsistent sitting beside Netflix/
        // Amazon/Carrefour, which do have real icons.
        register("spinneys", BrandIdentity(icon = Icons.Filled.LocalGroceryStore, color = Color(0xFF00573F)))
        register("talabat", BrandIdentity(icon = Icons.Filled.Restaurant, color = Color(0xFFFF5A00)))
        register("noon", BrandIdentity(icon = Icons.Filled.ShoppingBag, color = Color(0xFFFEEE00), backgroundColor = Color(0xFF1A1A1A)))
        register("deliveroo", BrandIdentity(icon = Icons.Filled.Restaurant, color = Color(0xFF00CCBC)))
        register("zomato", BrandIdentity(icon = Icons.Filled.Restaurant, color = Color(0xFFE23744)))
        register("swiggy", BrandIdentity(icon = Icons.Filled.Restaurant, color = Color(0xFFFC8019)))
        register("costa", BrandIdentity(icon = Icons.Filled.Coffee, backgroundColor = Color(0xFF630821)))
        register("starbucks", BrandIdentity(icon = Icons.Filled.Coffee, color = Color(0xFF00704A)))
        register("uber", BrandIdentity(monogram = "U"))
        register("careem", BrandIdentity(monogram = "C", color = Color(0xFF47D366)))
        register("makemytrip", BrandIdentity(icon = Icons.Filled.Flight, color = Color(0xFFE74C3C)))
        register("oracle", BrandIdentity(icon = Icons.Filled.Smartphone, color = Color(0xFFF80000)))

        // --- 2. BANKS — real, verifiable brand colours per institution. An
        // unlisted bank still isn't a letter (see the Bank fallback below),
        // but a recognised one gets its own colour instead of the shared
        // generic azure every unlisted institution defaults to.
        register("adcb", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFE21E26)))
        register("fab", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF003865)))
        register("wio", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFCCFF00)))
        register("hsbc", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFDB0011)))
        register("mashreq", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFEE3831)))
        register("emirates nbd", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF9E1B34)))
        register("emirates islamic", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF00A651)))
        register("rakbank", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFF7941D)))
        register("cbd", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF0072BC)))
        register("dib", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF00549F)))
        register("axis bank", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF97144D)))
        register("hdfc", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF004C8F)))
        register("icici", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFA4272C)))
        register("sbi", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFF2D5FA8)))
        register("kotak", BrandIdentity(icon = Icons.Filled.AccountBalance, color = Color(0xFFED1C24)))
        register("primary account", BrandIdentity(icon = Icons.Filled.Wallet))

        // --- 3. CARDS ---
        register("visa", BrandIdentity(icon = Icons.Filled.CreditCard, color = Color(0xFF1A1F71)))
        register("mastercard", BrandIdentity(icon = Icons.Filled.CreditCard, color = Color(0xFFEB001B)))
        register("amex", BrandIdentity(icon = Icons.Filled.CreditCard, color = Color(0xFF016FD0)))

        // --- 4. CATEGORIES ---
        register("salary", BrandIdentity(icon = Icons.Filled.Payments))
        register("groceries", BrandIdentity(icon = Icons.Filled.LocalGroceryStore))
        register("dining", BrandIdentity(icon = Icons.Filled.Restaurant))
        register("shopping", BrandIdentity(icon = Icons.Filled.ShoppingBag))
        register("transfer", BrandIdentity(icon = Icons.AutoMirrored.Filled.Send))
        register("cash", BrandIdentity(icon = Icons.Filled.Wallet))
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
            LedgerIdentityType.Merchant -> resolveMerchantByKeyword(normalized)
        }
    }

    private fun resolveCategory(name: String): BrandIdentity {
        if (name.contains("food") || name.contains("dining")) return BrandIdentity(icon = Icons.Filled.Restaurant)
        if (name.contains("grocer")) return BrandIdentity(icon = Icons.Filled.LocalGroceryStore)
        if (name.contains("shop")) return BrandIdentity(icon = Icons.Filled.ShoppingBag)
        if (name.contains("salary")) return BrandIdentity(icon = Icons.Filled.Payments)
        return BrandIdentity()
    }

    /**
     * A merchant nobody registered a real brand mark for is still not a
     * stranger — its own name almost always says what kind of business it is
     * ("... Taxi", "... Fresh Mart", "... Restaurant"). Matching on that
     * gives a real, meaningful icon instead of a random letter for the
     * overwhelming majority of small/local merchants an app like this
     * captures, which registering by exact name could never keep up with.
     */
    private fun resolveMerchantByKeyword(name: String): BrandIdentity {
        val rules: List<Pair<List<String>, ImageVector>> = listOf(
            listOf("taxi", "cab", "careem", "uber") to Icons.Filled.DirectionsCar,
            listOf("transport", "travel", "trip", "tourism", "cargo", "shipping") to Icons.Filled.DirectionsCar,
            listOf("flight", "airline", "airways") to Icons.Filled.Flight,
            listOf("hotel", "resort", "inn ") to Icons.Filled.Hotel,
            listOf("fitness", "gym", " fit") to Icons.Filled.FitnessCenter,
            listOf("mart", "grocer", "hypermarket", "supermarket", "fresh") to Icons.Filled.LocalGroceryStore,
            listOf("restaurant", "cafe", "cafteria", "cafeteria", "kitchen", "food", "diner", "bakery", "grill") to Icons.Filled.Restaurant,
            listOf("coffee", "tea trust", " tea ") to Icons.Filled.Coffee,
            listOf("pharmacy", "medical", "clinic") to Icons.Filled.LocalPharmacy,
            listOf("hospital") to Icons.Filled.LocalHospital,
            listOf("fuel", "petrol", "enoc", "adnoc", "gas station") to Icons.Filled.LocalGasStation,
            listOf("cinema", "movie", "theatre", "theater") to Icons.Filled.Movie,
            listOf("school", "university", "academy", "institute") to Icons.Filled.School,
            listOf("mobile", "telecom", "etisalat", "du ") to Icons.Filled.Smartphone,
        )
        val padded = " $name "
        for ((keywords, icon) in rules) {
            if (keywords.any { padded.contains(it) }) return BrandIdentity(icon = icon)
        }
        return BrandIdentity()
    }
}

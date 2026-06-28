#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 0.6 (revised) — domain vocabulary only..."

# ==== DOMAIN MODELS ====

cat > "$B/core/model/Account.kt" << 'EOF'
package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a financial account.
 */
@JvmInline
value class AccountId(val value: String) {
    init { require(value.isNotBlank()) { "Account id cannot be blank." } }
    override fun toString(): String = value
}

/**
 * A financial account the user holds (bank account, wallet, card).
 *
 * Each account has a single currency. Cross-currency views are built
 * at the presentation layer by aggregating accounts, never by storing
 * mixed currencies in one account.
 *
 * Relationships:
 * - owns zero or more [LedgerTransaction] instances
 * - carries a [Money] balance derived from those transactions
 * - belongs to exactly one [Currency]
 */
data class Account(
    val id: AccountId,
    val name: String,
    val currency: Currency,
    val balance: Money,
    val isDefault: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Account name cannot be blank." }
        require(balance.currency == currency) { "Account balance currency must match account currency." }
    }
}
EOF

cat > "$B/core/model/Budget.kt" << 'EOF'
package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a budget.
 */
@JvmInline
value class BudgetId(val value: String) {
    init { require(value.isNotBlank()) { "Budget id cannot be blank." } }
    override fun toString(): String = value
}

/**
 * The recurrence period a budget covers.
 */
enum class BudgetPeriod {
    Weekly,
    Monthly,
    Yearly,
}

/**
 * A spending limit applied overall or to a specific category.
 *
 * [limit] is the maximum the user intends to spend. [spent] is the
 * running total consumed so far within the current [period]. Both
 * carry the same [Currency] enforced at construction.
 *
 * Relationships:
 * - optionally scoped to one [Category] via [categoryId]
 * - [spent] is derived from [LedgerTransaction] amounts in the period
 */
data class Budget(
    val id: BudgetId,
    val name: String,
    val limit: Money,
    val spent: Money,
    val period: BudgetPeriod,
    val categoryId: CategoryId? = null,
) {
    init {
        require(name.isNotBlank()) { "Budget name cannot be blank." }
        require(limit.currency == spent.currency) { "Budget limit and spent must share the same currency." }
        require(limit.minorUnits > 0) { "Budget limit must be positive." }
    }

    val remaining: Money get() = limit - spent

    val progress: Float get() = if (limit.minorUnits == 0L) 0f
        else spent.minorUnits.toFloat() / limit.minorUnits.toFloat()
}
EOF

cat > "$B/core/model/DateRange.kt" << 'EOF'
package com.sherif.ledger.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * An inclusive date range used for filtering, analytics periods, and budgets.
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    init { require(!end.isBefore(start)) { "DateRange end cannot be before start." } }

    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)

    companion object {
        fun singleDay(date: LocalDate): DateRange = DateRange(date, date)

        fun monthOf(date: LocalDate): DateRange = DateRange(
            start = date.withDayOfMonth(1),
            end = date.withDayOfMonth(date.lengthOfMonth()),
        )
    }
}
EOF

cat > "$B/core/model/Percentage.kt" << 'EOF'
package com.sherif.ledger.core.model

/**
 * A percentage stored as basis points (0 to 10000) to avoid floating point.
 *
 * Mirrors how [Confidence] stores its value. 100% = 10000 basis points.
 */
@JvmInline
value class Percentage(val basisPoints: Int) : Comparable<Percentage> {
    init { require(basisPoints in 0..10000) { "Percentage must be 0..10000 basis points." } }

    fun asFloat(): Float = basisPoints / 10000f
    fun display(): String = "${basisPoints / 100}%"

    override fun compareTo(other: Percentage): Int = basisPoints.compareTo(other.basisPoints)

    companion object {
        val Zero = Percentage(0)
        val Full = Percentage(10000)
        fun fromPercent(percent: Int): Percentage {
            require(percent in 0..100)
            return Percentage(percent * 100)
        }
    }
}
EOF

cat > "$B/core/model/Currency.kt" << 'EOF'
package com.sherif.ledger.core.model

import java.util.Locale

@JvmInline
value class CurrencyCode(val value: String) {
    init { require(value.matches(CURRENCY_CODE_PATTERN)) { "Currency code must contain exactly three uppercase letters." } }
    override fun toString(): String = value
    private companion object { val CURRENCY_CODE_PATTERN = Regex("[A-Z]{3}") }
}

/**
 * Currency metadata used for formatting and currency-safe money operations.
 *
 * Ledger supports dual currency (AED and INR). Additional currencies can be
 * added by extending this companion.
 */
data class Currency(
    val code: CurrencyCode,
    val symbol: String,
    val fractionDigits: Int,
) {
    init {
        require(symbol.isNotBlank()) { "Currency symbol cannot be blank." }
        require(fractionDigits >= 0) { "Currency fraction digits cannot be negative." }
    }

    companion object {
        val AED = Currency(CurrencyCode("AED"), "AED", 2)
        val INR = Currency(CurrencyCode("INR"), "\u20B9", 2)
        val USD = Currency(CurrencyCode("USD"), "$", 2)

        fun of(code: String, symbol: String = code.uppercase(Locale.US), fractionDigits: Int = 2): Currency =
            Currency(CurrencyCode(code.uppercase(Locale.US)), symbol, fractionDigits)
    }
}
EOF

# ==== FEATURE DIRECTORY STRUCTURE (empty, no placeholder code) ====

for feat in accounts transactions budgets analytics capture review settings; do
    mkdir -p "$B/feature/$feat/data"
    mkdir -p "$B/feature/$feat/domain"
    mkdir -p "$B/feature/$feat/presentation/components"
    mkdir -p "$B/feature/$feat/presentation/preview"
    touch "$B/feature/$feat/data/.gitkeep"
    touch "$B/feature/$feat/domain/.gitkeep"
    touch "$B/feature/$feat/presentation/.gitkeep"
    touch "$B/feature/$feat/presentation/components/.gitkeep"
    touch "$B/feature/$feat/presentation/preview/.gitkeep"
done

echo "Done."
echo "5 domain files written. 7 feature directories created (empty)."
echo ""
echo "Run: git add -A && git commit -m 'feat(domain): sprint 0.6 canonical domain vocabulary and feature structure'"
echo "Then: ./gradlew assembleDebug"

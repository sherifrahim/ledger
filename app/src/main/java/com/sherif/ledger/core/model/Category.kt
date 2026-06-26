package com.sherif.ledger.core.model

/**
 * Strongly typed identifier for a transaction category.
 */
@JvmInline
value class CategoryId(val value: String) {
    init {
        require(value.isNotBlank()) { "Category id cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * Strongly typed display name for a transaction category.
 */
@JvmInline
value class CategoryName(val value: String) {
    init {
        require(value.isNotBlank()) { "Category name cannot be blank." }
    }

    override fun toString(): String = value
}

/**
 * Domain category used to group transactions without relying on raw strings.
 */
data class Category(
    val id: CategoryId,
    val name: CategoryName,
)

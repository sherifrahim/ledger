package com.sherif.ledger.core.domain.model

/**
 * Domain model for a canonical brand identity.
 */
data class Brand(
    val id: Long,
    val name: String,
    val brandKey: String,
    val defaultCategoryId: Long?
)

package com.sherif.ledger.core.domain.model

/**
 * Hierarchical domain model for transaction categorization.
 */
data class Category(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val iconKey: String?,
    val colorHue: Long?
)

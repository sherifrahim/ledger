package com.sherif.ledger.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * RC8 Phase B: generic deterministic memory — "the user already confirmed
 * this once, prefer that before asking AI." Deliberately generic
 * (decisionType/subjectKey/learnedValue as plain strings) rather than one new
 * table per decision kind, so a future decision type (transfer, relationship,
 * recurring) is a new [decisionType] string constant, never a new migration.
 * [com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore]'s existing
 * `merchant_category_overrides` table is intentionally NOT migrated into this
 * — it already works and is wired into a real UI flow (Review Inbox); this
 * table is for decision types that don't have one yet.
 */
@Entity(tableName = "learned_decisions", primaryKeys = ["decision_type", "subject_key"])
data class LearnedDecisionEntity(
    @ColumnInfo(name = "decision_type") val decisionType: String,
    @ColumnInfo(name = "subject_key") val subjectKey: String,
    @ColumnInfo(name = "learned_value") val learnedValue: String,
    @ColumnInfo(name = "confidence") val confidence: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

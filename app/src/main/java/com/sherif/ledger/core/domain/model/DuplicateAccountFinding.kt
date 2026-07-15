package com.sherif.ledger.core.domain.model

/**
 * A detected duplicate account identity: the same real-world (package, card
 * tail) signature has transactions split across more than one non-default
 * account. This is exactly the shape the RC1-era account-creation race
 * condition produces — two accounts created for one real card, transactions
 * for it splitting between them depending on timing.
 *
 * Detection only. Nothing merges, deletes, or reassigns automatically —
 * this is a diagnostic finding for safe manual review, not a repair.
 */
data class DuplicateAccountFinding(
    val packageName: String,
    val cardTail: String,
    val accountIds: List<Long>,
    val accountNames: List<String>,
    val transactionCounts: List<Int>,
)




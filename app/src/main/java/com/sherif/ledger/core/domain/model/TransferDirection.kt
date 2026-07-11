package com.sherif.ledger.core.domain.model

/**
 * The direction of a [TransactionType.TRANSFER]: which way money moved relative to
 * the account a transaction is recorded against.
 *
 * This is normalized, structured semantic information decided ONCE, at extraction
 * time (the only place raw notification text is legitimately parsed for meaning).
 * Downstream consumers — [com.sherif.ledger.core.domain.service.transaction.BalanceCalculator],
 * analytics, any other financial calculation — read this field. None of them
 * re-derive direction from text. If a transfer's direction could not be determined
 * upstream, this is null, and downstream code treats that as a visible gap to
 * surface, never as an invitation to guess from raw text itself.
 */
enum class TransferDirection {
    /** Money left this account, moving to another account or liability. */
    OUTGOING,

    /** Money arrived in this account, moving from another account. */
    INCOMING,
}



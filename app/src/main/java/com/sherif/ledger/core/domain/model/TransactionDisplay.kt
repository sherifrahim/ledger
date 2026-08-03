package com.sherif.ledger.core.domain.model

/**
 * Whether this transaction moves money OUT of the user's balance — i.e. it should
 * render negative / red.
 *
 * This is the single source of truth for the *display* sign, and it deliberately
 * mirrors [com.sherif.ledger.core.domain.service.transaction.BalanceCalculator]'s
 * balance sign so the feed and the balance can never disagree:
 *  - EXPENSE and an OUTGOING transfer are outflows (−).
 *  - INCOME, REFUND and an INCOMING transfer are inflows (+).
 *  - A direction-less transfer is treated as an outflow — conservative, since an
 *    unqualified bank transfer is most often the account holder sending money out
 *    (BalanceCalculator contributes zero for it, but showing it as an inflow would
 *    mislead the user into thinking they received money).
 *
 * Previously screens used `type == EXPENSE`, which rendered an outgoing transfer
 * (real money leaving) as a green +amount, as if it were income.
 */
val Transaction.isOutflow: Boolean
    get() = isOutflowOf(type, transferDirection)

/**
 * The same rule as [Transaction.isOutflow], expressed over the two fields it
 * actually depends on, so it can also be applied to a not-yet-persisted
 * [TransactionCandidate].
 *
 * Extracted (rather than duplicated) because
 * [com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine] needs
 * to ask "do these two records move money the same way?" while deciding whether a
 * candidate duplicates an existing transaction — and a second, independently
 * drifting copy of this rule there is exactly how a candidate could be judged an
 * outflow while the row it merged into renders as an inflow.
 *
 * A null [type] (extraction produced no type at all) is treated as an outflow,
 * matching the direction-less-transfer reasoning above: conservative, and it
 * never invents an inflow the user did not receive.
 */
fun isOutflowOf(type: TransactionType?, transferDirection: TransferDirection?): Boolean =
    when (type) {
        TransactionType.EXPENSE -> true
        TransactionType.INCOME -> false
        TransactionType.REFUND -> false
        TransactionType.TRANSFER -> transferDirection != TransferDirection.INCOMING
        null -> true
    }

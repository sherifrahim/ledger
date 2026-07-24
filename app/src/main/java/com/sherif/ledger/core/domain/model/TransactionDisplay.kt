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
    get() = when (type) {
        TransactionType.EXPENSE -> true
        TransactionType.INCOME -> false
        TransactionType.REFUND -> false
        TransactionType.TRANSFER -> transferDirection != TransferDirection.INCOMING
    }

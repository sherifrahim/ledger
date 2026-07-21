package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.common.logging.LedgerLogger
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.model.TransferDirection
import javax.inject.Inject

/**
 * Owns the single rule for how one transaction changes a balance. This is the ONE
 * place this arithmetic is defined — [AccountBalanceService] replays it to derive
 * every balance shown to the user; nothing else reimplements it.
 *
 * Account-type-aware: for a liability account ([AccountType.isLiability]),
 * "balance" means amount owed. A purchase increases what's owed rather than
 * decreasing available funds — the natural asset-account effect is inverted.
 *
 * Consumes only normalized, structured fields ([Transaction.type],
 * [Transaction.transferDirection]) — never raw notification text. Direction is
 * decided exactly once, upstream, at extraction time (see
 * [com.sherif.ledger.feature.capture.parsing.extraction.ExtractionHelpers.inferTransferDirection]).
 */
class BalanceCalculator @Inject constructor() {

    /**
     * The effect (signed minor units) of [transaction] on the balance of the
     * account it is recorded against, given that account's [accountType].
     *
     * [accountCurrencyCode], when supplied, guards a real found-in-production
     * bug: this function used to add `transaction.amount.minorUnits` straight
     * into the running total with NO check that the transaction's currency
     * matches the account's — a foreign-currency transaction misattributed to
     * the wrong account (confirmed via a real diagnostic bundle: an INR
     * transaction logged against an AED account) would have its raw minor
     * units treated as if they were the account's own currency, e.g. INR
     * 50,000 silently becoming "AED 50,000" in the sum. "Every transaction
     * carries its own Currency; there is no implicit app currency" is a
     * stated project invariant — this enforces it instead of assuming it.
     * Optional (defaults null → no check) so the many existing unit tests
     * that only exercise the sign/type logic don't all need updating; every
     * real call site (AccountBalanceService, FinancialTraceCollector) passes
     * the account's actual currency.
     */
    fun effect(transaction: Transaction, accountType: AccountType, accountCurrencyCode: CurrencyCode? = null): Long {
        if (accountCurrencyCode != null && transaction.amount.currencyCode != accountCurrencyCode) {
            LedgerLogger.e(
                "BalanceCalculator: transaction currency (${transaction.amount.currencyCode}) does not match " +
                    "account currency ($accountCurrencyCode) for fingerprint=${transaction.fingerprint.take(8)}; " +
                    "contributing zero effect rather than silently mixing units. This indicates an account-" +
                    "identity gap upstream — a transaction was attributed to an account of the wrong currency."
            )
            return 0L
        }
        val base = when (transaction.type) {
            TransactionType.INCOME -> transaction.amount.minorUnits
            TransactionType.EXPENSE -> -transaction.amount.minorUnits
            TransactionType.REFUND -> transaction.amount.minorUnits
            TransactionType.TRANSFER -> when (transaction.transferDirection) {
                TransferDirection.OUTGOING -> -transaction.amount.minorUnits
                TransferDirection.INCOMING -> transaction.amount.minorUnits
                null -> {
                    // Direction was not normalized upstream. This is a visible gap
                    // in extraction, not something to guess here — contribute
                    // nothing rather than risk a wrong-direction adjustment, and
                    // surface it so the gap is diagnosable rather than silent.
                    LedgerLogger.e(
                        "BalanceCalculator: TRANSFER with no transferDirection " +
                            "(fingerprint=${transaction.fingerprint.take(8)}); " +
                            "contributing zero effect. This indicates an extraction " +
                            "gap — every TRANSFER-typed transaction should carry a " +
                            "direction from extraction."
                    )
                    0L
                }
            }
        }
        return if (accountType.isLiability) -base else base
    }

    /**
     * The effect of a credit-card PAYMENT on the LIABILITY account it targets —
     * always a reduction in what's owed. This is deliberately a separate function
     * from [effect]: it applies to a SECOND account (the card being paid), not the
     * account the payment transaction is itself persisted against. Never mutates
     * anything or stores a cross-reference — the caller (AccountBalanceService)
     * determines which liability account is targeted, using RelationshipEngine
     * plus AccountMatching, and applies this at replay time only.
     */
    fun liabilityPaymentEffect(paymentAmount: Money): Long = -paymentAmount.minorUnits
}


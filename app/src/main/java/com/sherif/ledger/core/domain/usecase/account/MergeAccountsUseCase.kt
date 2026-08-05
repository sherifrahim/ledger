package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.repository.TransactionRunner
import javax.inject.Inject

sealed class MergeAccountsResult {
    data class Success(val transactionsMoved: Int) : MergeAccountsResult()
    data class Failed(val reason: String) : MergeAccountsResult()
}

/**
 * ACCOUNT_IDENTITY_PLAN Steps 4-5: the user-facing fix for an account split that
 * already happened — either from before Steps 1-3 existed, or from any case
 * those steps don't catch (two untailed accounts at the same institution, an
 * institution the resolver has never learned, a manual duplicate). Nothing here
 * is automatic; a merge only ever runs from an explicit user action.
 *
 * [keepAccountId] survives; [mergeAccountId] is soft-deleted. Both must share a
 * currency and [com.sherif.ledger.core.domain.model.AccountType] — merging
 * across either would either mix units or invert the sign of every combined
 * transaction ([com.sherif.ledger.core.domain.service.transaction.BalanceCalculator]
 * flips liability accounts), so this refuses rather than guessing.
 */
class MergeAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val transactionRunner: TransactionRunner,
) {
    suspend fun execute(keepAccountId: Long, mergeAccountId: Long): MergeAccountsResult {
        if (keepAccountId == mergeAccountId) {
            return MergeAccountsResult.Failed("Cannot merge an account into itself")
        }
        val keep = (accountRepository.getAccountById(keepAccountId) as? LedgerResult.Success)?.data
            ?: return MergeAccountsResult.Failed("Account to keep no longer exists")
        val merge = (accountRepository.getAccountById(mergeAccountId) as? LedgerResult.Success)?.data
            ?: return MergeAccountsResult.Failed("Account to merge no longer exists")

        if (keep.openingBalance.currencyCode != merge.openingBalance.currencyCode) {
            return MergeAccountsResult.Failed("These accounts are in different currencies")
        }
        if (keep.type != merge.type) {
            return MergeAccountsResult.Failed("These accounts are different account types")
        }

        val moved = transactionRunner.runInTransaction {
            val count = transactionRepository.reassignAllTransactions(mergeAccountId, keepAccountId)

            // Step 5: [merge]'s opening balance represented real money the user
            // held before Ledger's tracking window too — dropping it rather than
            // folding it in would make the combined replayed balance wrong by
            // exactly that amount, the same shape of error the original
            // split-account bug produced.
            val combined = keep.copy(
                openingBalance = Money(
                    keep.openingBalance.minorUnits + merge.openingBalance.minorUnits,
                    keep.openingBalance.currencyCode,
                ),
                accountNumberTail = keep.accountNumberTail ?: merge.accountNumberTail,
                // Whichever side was the fallback/default account, the survivor
                // now represents a real, user-confirmed account rather than the
                // fallback — RC7 Phase B's invariant that the default account is
                // never bound to as a real identity must keep holding after a
                // merge, not just before one.
                isDefault = false,
            )
            accountRepository.updateAccount(combined)
            accountRepository.deleteAccount(mergeAccountId)
            count
        }

        return MergeAccountsResult.Success(moved)
    }
}

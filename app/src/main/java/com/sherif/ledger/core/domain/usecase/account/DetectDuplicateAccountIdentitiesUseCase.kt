package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.DuplicateAccountFinding
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * RC2: detects — but never repairs — duplicate account identities left behind by
 * the historical AccountIdentityResolver race condition (fixed in this same RC2
 * delivery via @Singleton + Mutex, but that fix only prevents NEW duplicates; it
 * does nothing about accounts a pre-fix database may already contain).
 *
 * A finding means the same real-world (package, card tail) signature has
 * transactions split across more than one non-default account — exactly the
 * shape the race condition produces. Detection only, by explicit instruction:
 * nothing here merges, deletes, or reassigns. Findings are for safe manual
 * review before any future repair logic is introduced.
 *
 * Bounded, not O(n²): one full-transaction fetch, one distinct-signature pass
 * over it, then one indexed countTransactionsByOrigin query per DISTINCT
 * signature — bounded by how many real cards/accounts exist, not by total
 * transaction volume.
 */
class DetectDuplicateAccountIdentitiesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val ensureDefaultAccountUseCase: EnsureDefaultAccountUseCase,
) {
    suspend fun execute(): List<DuplicateAccountFinding> {
        val defaultAccountId = ensureDefaultAccountUseCase.execute()

        val accountsResult = accountRepository.observeAllAccounts().first()
        val accounts = (accountsResult as? LedgerResult.Success)?.data ?: return emptyList()
        val accountNameById = accounts.associate { it.id to it.name }

        val transactionsResult = transactionRepository.observeAllTransactions().first()
        val transactions = (transactionsResult as? LedgerResult.Success)?.data ?: return emptyList()

        val signatures = transactions
            .mapNotNull { txn ->
                val packageName = txn.origin?.packageName
                val cardTail = txn.cardTail
                if (packageName != null && cardTail != null) packageName to cardTail else null
            }
            .distinct()

        val findings = mutableListOf<DuplicateAccountFinding>()
        for ((packageName, cardTail) in signatures) {
            val counts = transactionRepository.countTransactionsByOrigin(packageName, cardTail)
            val nonDefaultCounts = counts.filter { it.accountId != defaultAccountId }
            if (nonDefaultCounts.size >= 2) {
                findings += DuplicateAccountFinding(
                    packageName = packageName,
                    cardTail = cardTail,
                    accountIds = nonDefaultCounts.map { it.accountId },
                    accountNames = nonDefaultCounts.map { accountNameById[it.accountId] ?: "Unknown" },
                    transactionCounts = nonDefaultCounts.map { it.count },
                )
            }
        }
        return findings
    }
}





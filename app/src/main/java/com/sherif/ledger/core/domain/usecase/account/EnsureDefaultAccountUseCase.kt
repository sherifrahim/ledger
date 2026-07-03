package com.sherif.ledger.core.domain.usecase.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.AccountRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Ensures that at least one default account exists in the system.
 * Used for first-launch initialization to prevent pipeline failures.
 */
class EnsureDefaultAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    suspend fun execute(): Long {
        val accountsFlow = accountRepository.observeAllAccounts().first()
        if (accountsFlow is LedgerResult.Success && accountsFlow.data.isNotEmpty()) {
            return accountsFlow.data.first().id
        }

        // Create default primary account
        val defaultAccount = Account(
            id = 0,
            name = "Primary Account",
            type = AccountType.CHECKING,
            balance = Money.zero(CurrencyCode.AED),
            accountNumberTail = null,
            bankBrandId = null
        )

        val result = accountRepository.insertAccount(defaultAccount)
        return if (result is LedgerResult.Success) {
            result.data
        } else {
            1L // Fallback ID if insertion fails, though it shouldn't
        }
    }
}

package com.sherif.ledger.core.domain.usecase.goal

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Goal
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.service.transaction.AccountBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetGoalProgressUseCaseTest {

    private val useCase = GetGoalProgressUseCase()

    private fun goal(accountId: Long, targetMinor: Long, currency: CurrencyCode = CurrencyCode.AED) =
        Goal(id = 1L, name = "Emergency fund", target = Money(targetMinor, currency), accountId = accountId)

    private fun balance(id: Long, minor: Long, currency: CurrencyCode = CurrencyCode.AED) = AccountBalance(
        account = Account(id, "ADCB Account", AccountType.SAVINGS, Money.zero(currency), null, null),
        balance = Money(minor, currency),
    )

    @Test
    fun `progress is the funding account's real balance`() {
        // Never a stored figure — so a goal cannot disagree with the Accounts screen.
        val result = useCase.execute(listOf(goal(1L, 1_000_000L)), listOf(balance(1L, 250_000L))).single()

        assertEquals(250_000L, result.saved.minorUnits)
        assertEquals(750_000L, result.remainingMinor)
        assertEquals("ADCB Account", result.accountName)
        assertEquals(0.25f, result.fraction, 0.001f)
    }

    @Test
    fun `a goal whose funding account is gone is dropped, not shown at zero`() {
        // "You have saved nothing" would be a false statement about a goal that has
        // lost its funding source.
        assertTrue(useCase.execute(listOf(goal(99L, 1_000L)), listOf(balance(1L, 500L))).isEmpty())
    }

    @Test
    fun `a goal is never measured by an account in another currency`() {
        val result = useCase.execute(
            listOf(goal(1L, 1_000_000L, CurrencyCode.AED)),
            listOf(balance(1L, 250_000L, CurrencyCode.USD)),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `an overdrawn account counts as nothing saved, not as negative progress`() {
        val result = useCase.execute(listOf(goal(1L, 1_000_000L)), listOf(balance(1L, -5_000L))).single()

        assertEquals(0L, result.saved.minorUnits)
        assertEquals(0f, result.fraction, 0.001f)
    }

    @Test
    fun `reaching the target is reported`() {
        val result = useCase.execute(listOf(goal(1L, 100_000L)), listOf(balance(1L, 120_000L))).single()

        assertTrue(result.isReached)
        assertEquals(0L, result.remainingMinor)
    }

    @Test
    fun `a zero target is not infinite progress`() {
        val result = useCase.execute(listOf(goal(1L, 0L)), listOf(balance(1L, 5_000L))).single()

        assertEquals(0f, result.fraction, 0.001f)
    }
}

package com.sherif.ledger.feature.goal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.Goal
import com.sherif.ledger.core.domain.model.GoalProgress
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.repository.GoalRepository
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.goal.GetGoalProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** An account the user can point a goal at. */
data class FundingAccountUi(
    val id: Long,
    val name: String,
    val currency: CurrencyCode,
)

data class GoalUiState(
    val goals: List<GoalProgress> = emptyList(),
    val fundingAccounts: List<FundingAccountUi> = emptyList(),
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountBalanceService: AccountBalanceService,
    private val getGoalProgressUseCase: GetGoalProgressUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalUiState())
    val uiState: StateFlow<GoalUiState> = _uiState.asStateFlow()

    init { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            val balances = accountBalanceService.currentBalances()
            goalRepository.observeAll().collect { goals ->
                _uiState.value = GoalUiState(
                    goals = getGoalProgressUseCase.execute(goals, balances),
                    // Only asset accounts. A credit card holds debt, not savings —
                    // pointing a goal at one would measure progress by how much is
                    // owed, which is the opposite of saving.
                    fundingAccounts = balances
                        .filterNot { it.account.type.isLiability }
                        .map {
                            FundingAccountUi(
                                id = it.account.id,
                                name = it.account.name,
                                currency = it.balance.currencyCode,
                            )
                        },
                )
            }
        }
    }

    fun addGoal(name: String, targetMinor: Long, accountId: Long, currency: CurrencyCode) {
        viewModelScope.launch {
            goalRepository.save(
                Goal(id = 0L, name = name, target = Money(targetMinor, currency), accountId = accountId),
            )
            refresh()
        }
    }

    fun removeGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.delete(goalId)
            refresh()
        }
    }
}

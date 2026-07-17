package com.sherif.ledger.feature.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.datastore.UserPreferencesRepository
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import com.sherif.ledger.core.domain.usecase.account.SeedOpeningBalanceUseCase
import com.sherif.ledger.feature.capture.sms.SmsImporter
import com.sherif.ledger.feature.onboarding.presentation.ImportRangeOption
import com.sherif.ledger.feature.onboarding.presentation.resolve
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One account awaiting a real-balance correction at the end of onboarding. */
data class AccountBalanceConfirmation(
    val accountId: Long,
    val accountName: String,
    val computedBalanceMinor: Long,
    val currencyCode: CurrencyCode,
)

@HiltViewModel
class SmsOnboardingViewModel @Inject constructor(
    private val smsImporter: SmsImporter,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val accountBalanceService: AccountBalanceService,
    private val seedOpeningBalanceUseCase: SeedOpeningBalanceUseCase,
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // null = not run yet; >=0 = count found; -1 = error
    private val _importResult = MutableStateFlow<Int?>(null)
    val importResult: StateFlow<Int?> = _importResult.asStateFlow()

    // Part 2: the range-selection step shows first; the user must confirm a
    // choice (default THIS_WEEK, matching "Recommended" in the spec) before
    // the existing permission/scan step appears.
    private val _hasConfirmedRange = MutableStateFlow(false)
    val hasConfirmedRange: StateFlow<Boolean> = _hasConfirmedRange.asStateFlow()

    private val _selectedRangeOption = MutableStateFlow(ImportRangeOption.THIS_WEEK)
    val selectedRangeOption: StateFlow<ImportRangeOption> = _selectedRangeOption.asStateFlow()

    private val _customStartDate = MutableStateFlow<LocalDate?>(null)
    val customStartDate: StateFlow<LocalDate?> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow<LocalDate?>(null)
    val customEndDate: StateFlow<LocalDate?> = _customEndDate.asStateFlow()

    // Shown once, right after a successful scan, so the real balance is
    // fixed before the user ever sees a number on the Dashboard — see
    // SeedOpeningBalanceUseCase for why this step exists at all.
    private val _showBalanceConfirmation = MutableStateFlow(false)
    val showBalanceConfirmation: StateFlow<Boolean> = _showBalanceConfirmation.asStateFlow()

    private val _balanceConfirmationAccounts = MutableStateFlow<List<AccountBalanceConfirmation>>(emptyList())
    val balanceConfirmationAccounts: StateFlow<List<AccountBalanceConfirmation>> = _balanceConfirmationAccounts.asStateFlow()

    fun selectRangeOption(option: ImportRangeOption) {
        _selectedRangeOption.value = option
    }

    fun setCustomRange(start: LocalDate?, end: LocalDate?) {
        _customStartDate.value = start
        _customEndDate.value = end
    }

    /** True once a valid range is chosen — for CUSTOM, both dates must be set. */
    fun canContinue(): Boolean =
        _selectedRangeOption.value != ImportRangeOption.CUSTOM ||
            (_customStartDate.value != null && _customEndDate.value != null)

    fun confirmRange() {
        _hasConfirmedRange.value = true
    }

    fun startImport() {
        viewModelScope.launch {
            com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsOnboardingViewModel: SMS Permission Granted. Starting Import.")
            _isImporting.value = true
            try {
                val range = _selectedRangeOption.value.resolve(_customStartDate.value, _customEndDate.value)
                com.sherif.ledger.core.common.logging.LedgerLogger.d(
                    "SmsOnboardingViewModel: Import window = ${range.label} [${range.start}, ${range.end}]",
                )
                val result = smsImporter.importHistoricalSms(range.start, range.end, range.label)
                userPreferencesRepository.setSmsImported(true)
                _importResult.value = result.found
                com.sherif.ledger.core.common.logging.LedgerLogger.d("SmsOnboardingViewModel: Import Completed. Found=${result.found}")

                val balances = accountBalanceService.currentBalances()
                _balanceConfirmationAccounts.value = balances.map {
                    AccountBalanceConfirmation(
                        accountId = it.account.id,
                        accountName = it.account.name,
                        computedBalanceMinor = it.balance.minorUnits,
                        currencyCode = it.balance.currencyCode,
                    )
                }
            } catch (e: Exception) {
                userPreferencesRepository.setSmsImported(true)
                _importResult.value = -1
                com.sherif.ledger.core.common.logging.LedgerLogger.e("SmsOnboardingViewModel: Import Failed", e)
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Called from the "Continue" button after a successful scan; routes past balance confirmation when there's nothing to confirm. */
    fun proceedPastImport(onComplete: () -> Unit) {
        if (_balanceConfirmationAccounts.value.isNotEmpty()) {
            _showBalanceConfirmation.value = true
        } else {
            onComplete()
        }
    }

    /** [actualBalancesMinor] maps accountId -> user-entered real balance; an account absent from the map is left uncorrected. */
    fun confirmBalances(actualBalancesMinor: Map<Long, Long>, onComplete: () -> Unit) {
        viewModelScope.launch {
            actualBalancesMinor.forEach { (accountId, actualMinor) ->
                seedOpeningBalanceUseCase.execute(accountId, actualMinor)
            }
            onComplete()
        }
    }

    fun skipImport(onComplete: () -> Unit) {
        viewModelScope.launch {
            userPreferencesRepository.setSmsImported(true) // Mark as "handled"
            onComplete()
        }
    }
}

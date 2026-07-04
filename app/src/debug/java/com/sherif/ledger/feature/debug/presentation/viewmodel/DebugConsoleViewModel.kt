package com.sherif.ledger.feature.debug.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.database.LedgerDatabase
import com.sherif.ledger.core.domain.usecase.transaction.ProcessNotificationUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.validation.ParserFixture
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.sherif.ledger.core.common.logging.LedgerLogger

import com.sherif.ledger.core.common.diagnostics.PipelineEvent
import com.sherif.ledger.core.common.diagnostics.PipelineTracker
import com.sherif.ledger.core.common.diagnostics.RealPipelineTracker
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.flatMapLatest

import com.sherif.ledger.core.domain.model.LedgerResult

import com.sherif.ledger.core.domain.model.Transaction

@HiltViewModel
class DebugConsoleViewModel @Inject constructor(
    private val app: Application,
    private val processNotificationUseCase: ProcessNotificationUseCase,
    private val database: LedgerDatabase,
    private val pipelineTracker: PipelineTracker,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiData = MutableStateFlow(DebugUiData())
    
    private val json = Json { ignoreUnknownKeys = true }

    // Summary of DB stats
    private val _dbSummary = MutableStateFlow(DatabaseSummary())
    val dbSummary: StateFlow<DatabaseSummary> = _dbSummary.asStateFlow()

    val uiState: StateFlow<DebugUiState> = combine(
        _uiData, 
        LedgerLogger.logs,
        (pipelineTracker as RealPipelineTracker).events
    ) { data, logs, events ->
        DebugUiState(
            fixtures = data.fixtures,
            selectedFixtureId = data.selectedFixtureId,
            packageName = data.packageName,
            title = data.title,
            text = data.text,
            logs = logs,
            pipelineEvents = events,
            lastTransaction = data.lastTransaction
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DebugUiState()
    )

    init {
        loadFixtures()
        loadDbSummary()
    }

    private fun loadDbSummary() {
        viewModelScope.launch {
            combine(
                accountRepository.observeAllAccounts(),
                transactionRepository.observeRecentTransactions(100)
            ) { accounts, txns ->
                val recentTxns = (txns as? LedgerResult.Success)?.data ?: emptyList()
                val lastTxn = recentTxns.firstOrNull()
                _uiData.update { it.copy(lastTransaction = lastTxn) }
                
                DatabaseSummary(
                    totalTransactions = recentTxns.size, // Showing up to 100 recent
                    totalAccounts = (accounts as? LedgerResult.Success)?.data?.size ?: 0,
                    lastInsertTime = lastTxn?.timestamp?.toEpochMilli()
                )
            }.collect { _dbSummary.value = it }
        }
    }

    private fun loadFixtures() {
        viewModelScope.launch {
            val fixtures = mutableListOf<ParserFixture>()
            val assetManager = app.assets
            val files = assetManager.list("fixtures") ?: emptyArray()
            files.forEach { file ->
                val content = assetManager.open("fixtures/$file").bufferedReader().readText()
                fixtures.addAll(json.decodeFromString<List<ParserFixture>>(content))
            }
            _uiData.update { it.copy(fixtures = fixtures) }
        }
    }

    fun handleAction(action: DebugAction) {
        when (action) {
            is DebugAction.SelectFixture -> {
                val fixture = _uiData.value.fixtures.find { it.id == action.fixtureId }
                fixture?.let { f ->
                    _uiData.update { it.copy(
                        selectedFixtureId = f.id,
                        packageName = getPackageForBank(f.bank),
                        title = "Notification",
                        text = f.raw
                    ) }
                }
            }
            is DebugAction.UpdatePackage -> _uiData.update { it.copy(packageName = action.packageName) }
            is DebugAction.UpdateTitle -> _uiData.update { it.copy(title = action.title) }
            is DebugAction.UpdateText -> _uiData.update { it.copy(text = action.text) }
            DebugAction.InjectOnce -> inject(1)
            is DebugAction.InjectMultiple -> inject(action.count)
            DebugAction.InjectRandom -> injectRandom()
            DebugAction.ClearDatabase -> clearDatabase()
        }
    }

    private fun inject(count: Int) {
        viewModelScope.launch {
            repeat(count) {
                val envelope = NotificationEnvelope(
                    packageName = _uiData.value.packageName,
                    title = _uiData.value.title,
                    text = _uiData.value.text,
                    subText = null,
                    timestamp = Instant.now(),
                    notificationKey = "debug_${System.currentTimeMillis()}_$it"
                )
                processNotificationUseCase.execute(envelope)
            }
        }
    }

    private fun injectRandom() {
        val fixtures = _uiData.value.fixtures
        if (fixtures.isEmpty()) return
        val fixture = fixtures.random()
        viewModelScope.launch {
            val envelope = NotificationEnvelope(
                packageName = getPackageForBank(fixture.bank),
                title = "Notification",
                text = fixture.raw,
                subText = null,
                timestamp = Instant.now(),
                notificationKey = "debug_random_${System.currentTimeMillis()}"
            )
            processNotificationUseCase.execute(envelope)
        }
    }

    private fun clearDatabase() {
        viewModelScope.launch {
            LedgerLogger.d("DebugConsole: Initiating database clear...")
            withContext(Dispatchers.IO) {
                database.clearAllTables()
            }
            LedgerLogger.d("DebugConsole: Database cleared successfully.")
            pipelineTracker.clear()
        }
    }

    private fun getPackageForBank(bank: String): String = when (bank.uppercase()) {
        "ADCB" -> "com.adcb.mobileapp"
        "FAB" -> "com.fab.mobileapp"
        "MASHREQ" -> "com.mashreq.mobile"
        "ENBD" -> "com.emiratesnbd.mobile"
        else -> "com.google.android.apps.messaging"
    }
}

data class DebugUiData(
    val fixtures: List<ParserFixture> = emptyList(),
    val selectedFixtureId: String? = null,
    val packageName: String = "com.google.android.apps.messaging",
    val title: String = "Notification",
    val text: String = "",
    val lastTransaction: Transaction? = null
)

data class DebugUiState(
    val fixtures: List<ParserFixture> = emptyList(),
    val selectedFixtureId: String? = null,
    val packageName: String = "com.google.android.apps.messaging",
    val title: String = "Notification",
    val text: String = "",
    val logs: List<String> = emptyList(),
    val pipelineEvents: List<PipelineEvent> = emptyList(),
    val lastTransaction: Transaction? = null
)

data class DatabaseSummary(
    val totalTransactions: Int = 0,
    val totalAccounts: Int = 0,
    val lastInsertTime: Long? = null
)

sealed interface DebugAction {
    data class SelectFixture(val fixtureId: String) : DebugAction
    data class UpdatePackage(val packageName: String) : DebugAction
    data class UpdateTitle(val title: String) : DebugAction
    data class UpdateText(val text: String) : DebugAction
    data object InjectOnce : DebugAction
    data class InjectMultiple(val count: Int) : DebugAction
    data object InjectRandom : DebugAction
    data object ClearDatabase : DebugAction
}

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
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.sherif.ledger.core.common.logging.LedgerLogger

import com.sherif.ledger.core.common.diagnostics.PipelineEvent
import com.sherif.ledger.core.common.diagnostics.PipelineStage
import com.sherif.ledger.core.common.diagnostics.StageStatus
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.flow

import com.sherif.ledger.core.domain.model.LedgerResult

import com.sherif.ledger.core.domain.model.Transaction

/**
 * Feature audit (Split & Notes) follow-up: the previous data source here,
 * [com.sherif.ledger.core.common.diagnostics.PipelineTracker], turned out to be
 * a lazily-created Hilt singleton whose bridge only wires the moment this
 * ViewModel is first constructed — meaning it silently missed every pipeline
 * event that happened before the console was ever opened, which is almost
 * certainly why it read as "lost." [PipelineTraceSink] doesn't have that
 * problem: it's eagerly populated by [ProcessNotificationUseCase] on every
 * live capture, console open or not. Mapped down into the same
 * [PipelineEvent]/[StageStatus] shape [DebugConsoleScreen] and
 * [PipelineDiagnosticsScreen] already render correctly — zero changes needed
 * in either screen.
 */
@HiltViewModel
class DebugConsoleViewModel @Inject constructor(
    private val app: Application,
    private val processNotificationUseCase: ProcessNotificationUseCase,
    private val database: LedgerDatabase,
    private val pipelineTraceSink: com.sherif.ledger.feature.diagnostics.PipelineTraceSink,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiData = MutableStateFlow(DebugUiData())
    
    private val json = Json { ignoreUnknownKeys = true }

    // Summary of DB stats
    private val _dbSummary = MutableStateFlow(DatabaseSummary())
    val dbSummary: StateFlow<DatabaseSummary> = _dbSummary.asStateFlow()

    // PipelineTraceSink has no reactive stream (it's a bounded ring buffer read
    // via .recent()) -- polling on a short interval is the simplest way to keep
    // this live without adding a new observable API to that sink for what's
    // purely a debug-console convenience.
    private val pipelineEventsFlow = flow {
        while (true) {
            emit(pipelineTraceSink.recent().flatMap { trace -> trace.events.map { it.toLegacyEvent(trace.notificationKey) } })
            delay(1000)
        }
    }

    val uiState: StateFlow<DebugUiState> = combine(
        _uiData, 
        LedgerLogger.logs,
        pipelineEventsFlow,
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
            pipelineTraceSink.clear()
        }
    }

    // RC7: was its own independently-wrong copy of bank package names (none of
    // which matched InstitutionRegistry's real values) — a THIRD drifted list
    // on top of the two already fixed (InstitutionRegistry itself, AdcbParser).
    // Now sourced from the same identifiers InstitutionRegistry seeds, so this
    // debug-only injector actually exercises real institution resolution
    // instead of always falling through to "unknown package".
    private fun getPackageForBank(bank: String): String = when (bank.uppercase()) {
        "ADCB" -> "com.adcb.nexgen"
        "FAB" -> "com.fab.personalbanking"
        "MASHREQ" -> "com.vipera.ts.starter.MashreqAE"
        "ENBD" -> "com.emiratesnbd.android"
        "HDFC" -> "HDFCBK"
        "ICICI" -> "ICICIB"
        "SBI" -> "SBIINB"
        "AXIS" -> "AXISBK"
        "KOTAK" -> "KOTAKB"
        else -> "com.google.android.apps.messaging"
    }
}

/**
 * Down-maps the real, granular [com.sherif.ledger.feature.diagnostics.PipelineEvent]
 * (10 stages, 8 statuses) into the coarser legacy [PipelineEvent]/[StageStatus]
 * shape [DebugConsoleScreen] already renders — accepting some stage-name
 * granularity loss in exchange for touching neither the legacy type nor either
 * screen. [PipelineStage.CAPTURE]/[PipelineStage.FILTER]/etc. below are the
 * legacy enum's own values, not new ones.
 */
private fun com.sherif.ledger.feature.diagnostics.PipelineEvent.toLegacyEvent(traceId: String): PipelineEvent {
    val legacyStage = when (stage) {
        com.sherif.ledger.feature.diagnostics.PipelineStage.NOTIFICATION_RECEIVED -> PipelineStage.CAPTURE
        com.sherif.ledger.feature.diagnostics.PipelineStage.NOTIFICATION_FILTER -> PipelineStage.FILTER
        com.sherif.ledger.feature.diagnostics.PipelineStage.FINANCIAL_EXTRACTORS,
        com.sherif.ledger.feature.diagnostics.PipelineStage.REGISTRY -> PipelineStage.PARSER
        com.sherif.ledger.feature.diagnostics.PipelineStage.VALIDATOR,
        com.sherif.ledger.feature.diagnostics.PipelineStage.INTENT_CLASSIFIER -> PipelineStage.NORMALIZATION
        com.sherif.ledger.feature.diagnostics.PipelineStage.CONFIRMATION_MATCHER,
        com.sherif.ledger.feature.diagnostics.PipelineStage.MERCHANT_RESOLVER,
        com.sherif.ledger.feature.diagnostics.PipelineStage.RELATIONSHIP_ENGINE -> PipelineStage.RECONCILIATION
        com.sherif.ledger.feature.diagnostics.PipelineStage.PERSISTENCE -> PipelineStage.PERSISTENCE
    }

    val detail = buildString {
        append(stage.name)
        reason?.let { append(": ${it.message}") }
        confidence?.let { append(" (confidence $it)") }
    }
    val legacyStatus = when (status) {
        com.sherif.ledger.feature.diagnostics.PipelineStatus.PASSED,
        com.sherif.ledger.feature.diagnostics.PipelineStatus.MATCHED -> StageStatus.SuccessWithDetails(detail)
        com.sherif.ledger.feature.diagnostics.PipelineStatus.IGNORED,
        com.sherif.ledger.feature.diagnostics.PipelineStatus.SKIPPED,
        com.sherif.ledger.feature.diagnostics.PipelineStatus.NOT_EXECUTED,
        com.sherif.ledger.feature.diagnostics.PipelineStatus.NOT_APPLICABLE -> StageStatus.Ignored
        com.sherif.ledger.feature.diagnostics.PipelineStatus.REJECTED,
        com.sherif.ledger.feature.diagnostics.PipelineStatus.FAILED -> StageStatus.Failed(detail)
    }

    return PipelineEvent(
        stage = legacyStage,
        status = legacyStatus,
        timestamp = timestamp.toEpochMilli(),
        traceId = traceId,
        metadata = metadata,
    )
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



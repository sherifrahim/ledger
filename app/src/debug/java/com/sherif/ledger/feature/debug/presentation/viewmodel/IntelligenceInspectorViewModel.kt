package com.sherif.ledger.feature.debug.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.capture.reconciliation.ReconciliationEngine
import com.sherif.ledger.core.domain.service.intelligence.CategoryIntelligenceEngine
import com.sherif.ledger.core.domain.service.intelligence.CategoryResolution
import com.sherif.ledger.core.domain.service.intelligence.ForecastEngine
import com.sherif.ledger.core.domain.service.intelligence.ForecastResult
import com.sherif.ledger.core.domain.service.intelligence.LearnedDecisionStore
import com.sherif.ledger.core.domain.service.intelligence.RecurringSchedule
import com.sherif.ledger.core.domain.service.intelligence.RecurringScheduleAnalyzer
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.settings.AiSettingsRepository
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.relationship.FinancialRelationship
import com.sherif.ledger.feature.relationship.RelationshipEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MerchantResolutionRow(val displayName: String, val confidence: Int, val reason: String, val isResolved: Boolean)

data class CategoryIntelligenceRow(val category: String, val subcategory: String?, val confidence: Int, val reason: String, val source: String)

data class TransactionIntelligenceRow(
    val transactionId: Long,
    val rawText: String,
    val amountMinor: Long,
    val currencyCode: CurrencyCode,
    val merchant: MerchantResolutionRow,
    val category: CategoryIntelligenceRow,
    /** Gated by ConfidenceGate — only true when the deterministic category confidence is below the user's configured threshold. */
    val canAskAi: Boolean,
    val aiResult: CategoryIntelligenceRow? = null,
    val askingAi: Boolean = false,
)

data class RelationshipRow(val type: String, val band: String, val confidencePercent: Int, val reasoning: List<String>, val decision: String)

data class RecurringRow(
    val label: String,
    val kind: String,
    val frequency: String,
    val confidence: Int,
    val lastOccurrenceEpochMillis: Long,
    val nextExpectedEpochMillis: Long,
    val averageAmountMinor: Long,
    val currencyCode: CurrencyCode,
)

data class ForecastRow(
    val currencyCode: CurrencyCode,
    val currentBalanceMinor: Long,
    val expectedBalanceMinor: Long,
    val horizonDays: Int,
    val projectedSalaryEpochMillis: Long?,
)

data class LearnedDecisionRow(val decisionType: String, val subjectKey: String, val learnedValue: String, val confidence: Int)

/** RC9 Phase C: a transaction's best-scoring match among its own recent neighbors, via ReconciliationEngine.explainScoring — real reasoning, not fabricated. */
data class DuplicateReasoningRow(val transactionRawText: String, val bestMatchRawText: String?, val score: Int, val details: String)

data class IntelligenceInspectorUiState(
    val loading: Boolean = true,
    val transactions: List<TransactionIntelligenceRow> = emptyList(),
    val relationships: List<RelationshipRow> = emptyList(),
    val recurringSchedules: List<RecurringRow> = emptyList(),
    val forecast: ForecastRow? = null,
    val learnedDecisions: List<LearnedDecisionRow> = emptyList(),
    val duplicateFingerprintCount: Int = 0,
    val duplicateReasoning: List<DuplicateReasoningRow> = emptyList(),
)

/**
 * RC8 Phase H — Intelligence Inspector: makes every deterministic/AI decision
 * this RC's engines produce explainable in one place. Explains, never
 * decides — this screen calls the exact same engines the rest of the app
 * would (CategoryIntelligenceEngine, RecurringScheduleAnalyzer, ForecastEngine,
 * RelationshipEngine, LearnedDecisionStore, MerchantResolver), it computes
 * nothing new itself. "Ask AI" is the ONE place this RC calls AIOrchestrator
 * outside of tests — always user-triggered, always gated by [ConfidenceGate],
 * and always going through AIOrchestrator's own existing validate/audit/
 * metrics/debug-trace pipeline (see AIOrchestrator.requestSuggestion).
 */
@HiltViewModel
class IntelligenceInspectorViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantResolver: MerchantResolver,
    private val categoryIntelligenceEngine: CategoryIntelligenceEngine,
    private val relationshipEngine: RelationshipEngine,
    private val recurringScheduleAnalyzer: RecurringScheduleAnalyzer,
    private val forecastEngine: ForecastEngine,
    private val learnedDecisionStore: LearnedDecisionStore,
    private val aiSettingsRepository: AiSettingsRepository,
    private val ensureDefaultAccountUseCase: EnsureDefaultAccountUseCase,
    private val reconciliationEngine: ReconciliationEngine,
) : ViewModel() {

    private val _state = MutableStateFlow(IntelligenceInspectorUiState())
    val state: StateFlow<IntelligenceInspectorUiState> = _state.asStateFlow()

    private var lastTransactions: List<Transaction> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)

            val recentResult = transactionRepository.observeRecentTransactions(RECENT_LIMIT).first()
            val recent = (recentResult as? LedgerResult.Success)?.data ?: emptyList()
            val allResult = transactionRepository.observeAllTransactions().first()
            val all = (allResult as? LedgerResult.Success)?.data ?: emptyList()
            lastTransactions = recent

            val relationships = if (all.isNotEmpty()) relationshipEngine.analyze(all) else emptyList()
            val relationshipByTxnId = relationships.groupBy { it.sourceTransactionId }

            // RC9 Phase G: fetched ONCE per screen load, not once per transaction —
            // this was a real, confirmed N+1 DataStore-read pattern (ConfidenceGate.shouldConsultAi
            // re-reads AiSettingsRepository per call) found during this RC's performance review.
            val categoryConfidenceThreshold = aiSettingsRepository.confidenceThresholdForCapability(AICapability.MERCHANT_CLASSIFICATION).first()

            val transactionRows = recent.map { txn ->
                buildRow(txn, relationshipByTxnId[txn.id]?.firstOrNull(), categoryConfidenceThreshold)
            }

            val relationshipRows = relationships
                .sortedByDescending { it.diagnostics.confidence }
                .take(RELATIONSHIP_LIMIT)
                .map {
                    RelationshipRow(
                        type = it.type.name,
                        band = it.confidence.band.name,
                        confidencePercent = it.confidence.value,
                        reasoning = it.reasoning,
                        decision = it.diagnostics.decision,
                    )
                }

            val schedules = recurringScheduleAnalyzer.analyze(all).sortedBy { it.nextExpectedDate }
            val recurringRows = schedules.map { it.toRow() }

            val defaultAccountId = ensureDefaultAccountUseCase.execute()
            val forecast = forecastEngine.forecast(defaultAccountId)?.toRow()

            val learned = learnedDecisionStore.all().map {
                LearnedDecisionRow(it.decisionType, it.subjectKey, it.learnedValue, it.confidence)
            }

            val duplicateCount = all.groupBy { it.fingerprint }.count { it.value.size > 1 }

            // RC9 Phase C: real reconciliation reasoning for each of the recent
            // transactions, scored against its OTHER recent same-account neighbors
            // via the exact same scoring ReconciliationEngine already uses live —
            // never a fabricated explanation.
            val duplicateReasoning = recent.mapNotNull { txn ->
                val neighbors = recent.filter { it.id != txn.id && it.accountId == txn.accountId }
                if (neighbors.isEmpty()) return@mapNotNull null
                val candidate = TransactionCandidate(
                    source = txn.source,
                    rawText = txn.rawText ?: "",
                    merchantName = txn.rawText,
                    amountMinor = txn.amount.minorUnits,
                    currencyCode = txn.amount.currencyCode,
                    timestamp = txn.timestamp,
                    accountHint = txn.cardTail,
                    transactionType = txn.type,
                    transferDirection = txn.transferDirection,
                    origin = txn.origin,
                )
                val best = reconciliationEngine.explainScoring(candidate, neighbors).firstOrNull() ?: return@mapNotNull null
                DuplicateReasoningRow(
                    transactionRawText = txn.rawText ?: "(no text)",
                    bestMatchRawText = best.first.rawText,
                    score = best.second.score,
                    details = best.second.details,
                )
            }.filter { it.score > 0 }.sortedByDescending { it.score }.take(DUPLICATE_REASONING_LIMIT)

            _state.value = IntelligenceInspectorUiState(
                loading = false,
                transactions = transactionRows,
                relationships = relationshipRows,
                recurringSchedules = recurringRows,
                forecast = forecast,
                learnedDecisions = learned,
                duplicateFingerprintCount = duplicateCount,
                duplicateReasoning = duplicateReasoning,
            )
        }
    }

    /** User-triggered only — never automatic. Only enabled (see canAskAi) when the deterministic result is uncertain. */
    fun askAiForCategory(transactionId: Long) {
        val txn = lastTransactions.firstOrNull { it.id == transactionId } ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                transactions = _state.value.transactions.map {
                    if (it.transactionId == transactionId) it.copy(askingAi = true) else it
                },
            )
            val deterministic = categoryIntelligenceEngine.resolveDeterministic(txn.rawText)
            val aiResolution = categoryIntelligenceEngine.resolveWithAiFallback(
                rawMerchantText = txn.rawText ?: "",
                amountMinor = txn.amount.minorUnits,
                currencyCode = txn.amount.currencyCode,
                deterministic = deterministic,
            )
            _state.value = _state.value.copy(
                transactions = _state.value.transactions.map {
                    if (it.transactionId == transactionId) it.copy(askingAi = false, aiResult = aiResolution.toRow()) else it
                },
            )
        }
    }

    private fun buildRow(txn: Transaction, relationshipHint: FinancialRelationship?, categoryConfidenceThreshold: Int): TransactionIntelligenceRow {
        val merchantResolution = merchantResolver.resolve(txn.rawText)
        val merchantRow = when (merchantResolution) {
            is MerchantResolution.Resolved -> MerchantResolutionRow(merchantResolution.displayName, merchantResolution.confidence, merchantResolution.reason, true)
            is MerchantResolution.Unresolved -> MerchantResolutionRow(merchantResolution.displayName, 0, merchantResolution.reason, false)
        }
        val categoryResolution = categoryIntelligenceEngine.resolveDeterministic(txn.rawText, relationshipHint?.type)
        // Same rule ConfidenceGate.shouldConsultAi encodes, applied against the
        // threshold fetched once above instead of once per transaction.
        val canAskAi = categoryResolution.confidence < categoryConfidenceThreshold
        return TransactionIntelligenceRow(
            transactionId = txn.id,
            rawText = txn.rawText ?: "",
            amountMinor = txn.amount.minorUnits,
            currencyCode = txn.amount.currencyCode,
            merchant = merchantRow,
            category = categoryResolution.toRow(),
            canAskAi = canAskAi,
        )
    }

    private fun CategoryResolution.toRow() = CategoryIntelligenceRow(category, subcategory, confidence, reason, source.name)

    private fun RecurringSchedule.toRow() = RecurringRow(
        label = label,
        kind = kind.name,
        frequency = frequency.name,
        confidence = confidence,
        lastOccurrenceEpochMillis = lastOccurrence.toEpochMilli(),
        nextExpectedEpochMillis = nextExpectedDate.toEpochMilli(),
        averageAmountMinor = averageAmountMinor,
        currencyCode = currencyCode,
    )

    private fun ForecastResult.toRow() = ForecastRow(
        currencyCode = currencyCode,
        currentBalanceMinor = currentBalanceMinor,
        expectedBalanceMinor = expectedBalanceMinor,
        horizonDays = horizonDays,
        projectedSalaryEpochMillis = projectedSalaryDate?.toEpochMilli(),
    )

    companion object {
        private const val RECENT_LIMIT = 25
        private const val RELATIONSHIP_LIMIT = 20
        private const val DUPLICATE_REASONING_LIMIT = 15
    }
}

package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.feature.relationship.RelationshipEngine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class RelationshipSummaryDto(
    val relationshipId: String,
    val type: String,
    val sourceTransactionId: Long,
    val targetTransactionId: Long?,
    val confidence: Int,
    val confidenceBand: String,
    val reasoning: List<String>,
)

/** Every relationship RelationshipEngine currently finds across all persisted
 *  transactions — the same evidence FinancialTraceCollector's liability
 *  adjustments and the live Dashboard both already depend on. Relationships
 *  are never persisted (always recomputed fresh from current transactions),
 *  so this is inherently a live snapshot, not a historical log. */
class RelationshipCollector @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val relationshipEngine: RelationshipEngine,
) : DiagnosticCollector {

    override val id: String = "relationships"

    override suspend fun collect(): DiagnosticSection {
        val result = transactionRepository.observeAllTransactions().first()
        val transactions = (result as? LedgerResult.Success)?.data ?: emptyList()
        val relationships = if (transactions.isEmpty()) emptyList() else relationshipEngine.analyze(transactions)

        val dtos = relationships.map {
            RelationshipSummaryDto(
                relationshipId = it.relationshipId,
                type = it.type.name,
                sourceTransactionId = it.sourceTransactionId,
                targetTransactionId = it.targetTransactionId,
                confidence = it.confidence.value,
                confidenceBand = it.confidence.band.name,
                reasoning = it.reasoning,
            )
        }
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(dtos))
    }
}




package com.sherif.ledger.core.domain.service.diagnostic

import com.sherif.ledger.core.domain.service.transaction.AccountBalanceService
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

@Serializable
data class AccountSummaryDto(
    val id: Long,
    val name: String,
    val type: String,
    val isLiability: Boolean,
    val accountNumberTail: String?,
    val currentBalanceMinor: Long,
    val currencyCode: String,
)

/** Every account, as [AccountBalanceService] currently reports it — the same
 *  replay-based balance the Dashboard shows, not a separate computation. */
class AccountCollector @Inject constructor(
    private val accountBalanceService: AccountBalanceService,
) : DiagnosticCollector {

    override val id: String = "accounts"

    override suspend fun collect(): DiagnosticSection {
        val balances = accountBalanceService.currentBalances()
        val dtos = balances.map {
            AccountSummaryDto(
                id = it.account.id,
                name = it.account.name,
                type = it.account.type.name,
                isLiability = it.account.type.isLiability,
                accountNumberTail = it.account.accountNumberTail,
                currentBalanceMinor = it.balance.minorUnits,
                currencyCode = it.balance.currencyCode.name,
            )
        }
        val json = Json { prettyPrint = true }
        return DiagnosticSection.Json(id, json.encodeToString(dtos))
    }
}




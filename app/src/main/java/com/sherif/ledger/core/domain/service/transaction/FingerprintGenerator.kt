package com.sherif.ledger.core.domain.service.transaction

import com.sherif.ledger.core.domain.usecase.transaction.InsertTransactionUseCase
import java.security.MessageDigest
import javax.inject.Inject

/**
 * Service responsible for generating unique transaction fingerprints for deduplication.
 */
class FingerprintGenerator @Inject constructor() {

    fun generate(params: InsertTransactionUseCase.Params): String {
        val bucket = params.timestamp.toEpochMilli() / (3600 * 1000) // 1-hour bucket
        val raw = "${params.accountId}|${params.amountMinor}|${params.currencyCode}|${bucket}|${params.rawMerchantText}"
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

package com.sherif.ledger.feature.capture.parsing.validation

import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import com.sherif.ledger.feature.capture.parsing.ParseResult
import com.sherif.ledger.feature.capture.parsing.ParserRegistry
import java.time.Instant
import javax.inject.Inject

class ParserValidationRunner @Inject constructor(
    private val parserRegistry: ParserRegistry
) {

    fun validate(fixtures: List<ParserFixture>): List<ValidationResult> {
        return fixtures.map { validateFixture(it) }
    }

    private fun validateFixture(fixture: ParserFixture): ValidationResult {
        val timestamp = fixture.expected?.timestamp?.let { Instant.parse(it) } ?: Instant.now()
        
        val envelope = NotificationEnvelope(
            packageName = getPackageForBank(fixture.bank),
            title = "Notification",
            text = fixture.raw,
            subText = null,
            timestamp = timestamp,
            notificationKey = "key_${fixture.id}"
        )

        val parseResult = parserRegistry.parse(envelope)
        val errors = mutableListOf<String>()

        return when (parseResult) {
            is ParseResult.Success -> {
                if (!fixture.shouldParse) {
                    ValidationResult(fixture.id, fixture.bank, fixture.type, false, listOf("Expected Ignore, but got Success"))
                } else {
                    compareResults(fixture, parseResult.candidate, errors)
                    ValidationResult(fixture.id, fixture.bank, fixture.type, errors.isEmpty(), errors)
                }
            }
            ParseResult.Ignore -> {
                if (fixture.shouldParse) {
                    ValidationResult(fixture.id, fixture.bank, fixture.type, false, listOf("Expected Success, but got Ignore"))
                } else {
                    ValidationResult(fixture.id, fixture.bank, fixture.type, true, wasIgnored = true)
                }
            }
            is ParseResult.Failed -> {
                if (fixture.shouldParse) {
                    ValidationResult(fixture.id, fixture.bank, fixture.type, false, listOf("Parsing failed: ${parseResult.reason}"))
                } else {
                    // If it should have been ignored but failed, is it a failure or success? 
                    // Usually we want specific "Ignore" behavior.
                    ValidationResult(fixture.id, fixture.bank, fixture.type, false, listOf("Expected Ignore, but got Failure: ${parseResult.reason}"))
                }
            }
        }
    }

    private fun compareResults(fixture: ParserFixture, actual: TransactionCandidate, errors: MutableList<String>) {
        val expected = fixture.expected ?: return

        if (expected.amount != null && expected.amount != actual.amountMinor) {
            errors.add("Amount mismatch: Expected ${expected.amount}, Actual ${actual.amountMinor}")
        }
        if (expected.currency != null && expected.currency != actual.currencyCode?.name) {
            errors.add("Currency mismatch: Expected ${expected.currency}, Actual ${actual.currencyCode?.name}")
        }
        if (expected.merchant != null && expected.merchant != actual.merchantName) {
            errors.add("Merchant mismatch: Expected ${expected.merchant}, Actual ${actual.merchantName}")
        }
        if (expected.transactionType != null && expected.transactionType != actual.transactionType?.name) {
            errors.add("Type mismatch: Expected ${expected.transactionType}, Actual ${actual.transactionType?.name}")
        }
        if (expected.accountHint != null && expected.accountHint != actual.accountHint) {
            errors.add("AccountHint mismatch: Expected ${expected.accountHint}, Actual ${actual.accountHint}")
        }
    }

    private fun getPackageForBank(bank: String): String = when (bank.uppercase()) {
        "ADCB" -> "com.adcb.mobileapp"
        else -> "unknown"
    }

    fun calculateStats(results: List<ValidationResult>): List<ParserStats> {
        return results.groupBy { it.bank }.map { (bank, bankResults) ->
            val byType = bankResults.groupBy { it.type }.mapValues { (_, typeResults) ->
                TypeStats(typeResults.size, typeResults.count { it.isSuccess })
            }

            ParserStats(
                bank = bank,
                total = bankResults.size,
                success = bankResults.count { it.isSuccess },
                ignored = bankResults.count { it.wasIgnored },
                failed = bankResults.count { !it.isSuccess && !it.wasIgnored },
                falsePositives = bankResults.count { !it.isSuccess && it.errors.any { e -> "Expected Ignore" in e } },
                falseNegatives = bankResults.count { !it.isSuccess && it.errors.any { e -> "Expected Success" in e } },
                byType = byType
            )
        }
    }

    fun formatSummary(stats: List<ParserStats>): String {
        return buildString {
            appendLine("Parser Validation Summary")
            appendLine("=========================")
            stats.forEach { stat ->
                appendLine("Bank: ${stat.bank}")
                appendLine("Overall Accuracy: ${(stat.accuracy * 100).toInt()}%")
                appendLine("Total Fixtures: ${stat.total}")
                appendLine("Success: ${stat.success}")
                appendLine("Ignored: ${stat.ignored}")
                appendLine("Failed: ${stat.failed}")
                appendLine("False Positives: ${stat.falsePositives}")
                appendLine("False Negatives: ${stat.falseNegatives}")
                appendLine("Breakdown by Type:")
                stat.byType.forEach { (type, typeStat) ->
                    appendLine("  $type: ${typeStat.success}/${typeStat.total}")
                }
                appendLine("-------------------------")
            }
        }
    }
}

package com.sherif.ledger.feature.ai.prompt

import com.sherif.ledger.feature.ai.domain.AICapability
import com.sherif.ledger.feature.ai.domain.AIContext
import com.sherif.ledger.feature.ai.domain.DuplicateAnalysisContext
import com.sherif.ledger.feature.ai.domain.FalsePositiveReviewContext
import com.sherif.ledger.feature.ai.domain.ForecastAnalysisContext
import com.sherif.ledger.feature.ai.domain.InsightAnalysisContext
import com.sherif.ledger.feature.ai.domain.MerchantAnalysisContext
import com.sherif.ledger.feature.ai.domain.RelationshipAnalysisContext
import com.sherif.ledger.feature.ai.domain.SearchAnalysisContext

/**
 * RC5's "Prompt Library" — one logical prompt per capability, shared by
 * every provider. Only the provider layer varies HOW a prompt is sent
 * (message array shape, headers); the prompt TEXT itself, and the required
 * response shape, is decided here and only here.
 */
object PromptLibrary {

    /** Every prompt ends with this — the contract AIOrchestrator's response parser relies on, regardless of provider. */
    private const val RESPONSE_FORMAT = """
Respond with ONLY a single JSON object, no other text, matching exactly this shape:
{"confidencePercent": <integer 0-100>, "reason": "<one short sentence>", "fields": {<string keys and string values relevant to this task>}}
"""

    fun promptFor(capability: AICapability, context: AIContext): String = when (context) {
        is MerchantAnalysisContext -> merchant(context)
        is DuplicateAnalysisContext -> duplicate(context)
        is RelationshipAnalysisContext -> relationship(context)
        is InsightAnalysisContext -> insight(context)
        is ForecastAnalysisContext -> forecast(context)
        is SearchAnalysisContext -> search(context)
        is FalsePositiveReviewContext -> falsePositiveReview(context)
    }

    fun merchant(context: MerchantAnalysisContext): String = """
You are a financial merchant-classification assistant for a personal finance app.
A deterministic engine could not confidently classify this transaction and is asking for your opinion.

Raw merchant text: "${context.rawMerchantText}"
Amount: ${context.amountMinor / 100.0} ${context.currencyCode}
Merchants already known to the system: ${context.existingMerchantMatches.joinToString(", ").ifBlank { "none" }}
Known categories: ${context.knownCategories.joinToString(", ")}

Task: suggest a normalized merchant name and a category from the known list (or a reasonable new one if none fit).
Include "merchant" and "category" in fields.
$RESPONSE_FORMAT
""".trimIndent()

    fun duplicate(context: DuplicateAnalysisContext): String = """
You are a duplicate-transaction-detection assistant for a personal finance app.
Two records may represent the SAME real-world financial event (e.g. a notification and an SMS for the same purchase).

Record A: ${context.candidateA.amountMinor / 100.0} ${context.candidateA.currencyCode}, ${context.candidateA.type}, merchant="${context.candidateA.merchant}", at epoch ${context.candidateA.timestampEpochMillis}
Record B: ${context.candidateB.amountMinor / 100.0} ${context.candidateB.currencyCode}, ${context.candidateB.type}, merchant="${context.candidateB.merchant}", at epoch ${context.candidateB.timestampEpochMillis}

Task: decide whether these are duplicates of the same event.
Include "isDuplicate" ("true" or "false") in fields.
$RESPONSE_FORMAT
""".trimIndent()

    fun relationship(context: RelationshipAnalysisContext): String = """
You are a financial-relationship-detection assistant for a personal finance app.
Given this sequence of transactions (oldest first), identify whether any of them relate to each other
(e.g. a transfer between the user's own accounts, a credit card payment, a recurring subscription).

${context.transactions.joinToString("\n") { "- ${it.amountMinor / 100.0} ${it.currencyCode}, ${it.type}, merchant=\"${it.merchant}\", at epoch ${it.timestampEpochMillis}" }}

Task: describe the most likely relationship, if any.
Include "relationshipType" in fields (or "none").
$RESPONSE_FORMAT
""".trimIndent()

    fun insight(context: InsightAnalysisContext): String = """
You are a financial-insight assistant for a personal finance app.
Period: ${context.periodLabel}
Total income: ${context.totalIncomeMinor / 100.0} ${context.currencyCode}
Total expense: ${context.totalExpenseMinor / 100.0} ${context.currencyCode}
Category totals: ${context.categoryTotals.entries.joinToString(", ") { "${it.key}=${it.value / 100.0}" }}

Task: write one short, plain-language observation about this period's spending.
Include "summary" in fields.
$RESPONSE_FORMAT
""".trimIndent()

    fun forecast(context: ForecastAnalysisContext): String = """
You are a spending-forecast assistant for a personal finance app.
Historical monthly totals (oldest first, minor units): ${context.historicalMonthlyTotalsMinor.joinToString(", ")}
Currency: ${context.currencyCode}

Task: estimate next month's likely total spend and explain the trend briefly.
Include "estimatedNextMonthMinor" in fields.
$RESPONSE_FORMAT
""".trimIndent()

    fun falsePositiveReview(context: FalsePositiveReviewContext): String = """
You are a careful financial-reality-check assistant for a personal finance app.
A deterministic rules engine captured the message below as a transaction on the
user's OWN account, but its own confidence was low, and it is asking for a second
opinion before trusting the capture. It gave these reasons for its uncertainty:
${context.deterministicReasoning.joinToString("\n") { "- $it" }}

Raw captured message: "${context.rawMessageText}"
Sender: ${context.senderIdentifier}
Extracted amount: ${context.amountMinor / 100.0} ${context.currencyCode}
Extracted merchant/description: "${context.merchant}"
Extracted direction: ${context.transactionType}

Task: decide whether this message is genuine evidence that money moved into or
out of the user's OWN personal account (a real purchase, transfer, salary,
refund, etc.) — as opposed to a promotion/marketing message, a balance or
statement notice, or a third party's OWN acknowledgement that IT received a
payment the user already made elsewhere (e.g. a buy-now-pay-later provider's
"payment received" receipt, which confirms an EARLIER movement rather than
describing a new one).
Include "isRealTransaction" ("true" or "false") in fields.
$RESPONSE_FORMAT
""".trimIndent()

    fun search(context: SearchAnalysisContext): String = """
You are a natural-language transaction search assistant for a personal finance app.
User query: "${context.query}"

Candidate transactions:
${context.recentTransactions.joinToString("\n") { "- ${it.amountMinor / 100.0} ${it.currencyCode}, ${it.type}, merchant=\"${it.merchant}\", at epoch ${it.timestampEpochMillis}" }}

Task: identify which candidates (if any) match the query's intent.
Include "matchingCount" in fields (a count, as a string).
$RESPONSE_FORMAT
""".trimIndent()
}

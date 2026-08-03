package com.sherif.ledger.feature.relationship

import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.feature.relationship.RelationshipMath.amountDiffMinor
import com.sherif.ledger.feature.relationship.RelationshipMath.daysBetween
import com.sherif.ledger.feature.relationship.RelationshipMath.sameAccount
import com.sherif.ledger.feature.relationship.RelationshipMath.sameAmount
import com.sherif.ledger.feature.relationship.RelationshipMath.sameCard
import com.sherif.ledger.feature.relationship.RelationshipMath.secondsBetween
import com.sherif.ledger.core.domain.model.merchantOrRawText

/** Small helper to assemble a relationship + its diagnostics consistently. */
private fun build(
    key: String,
    type: RelationshipType,
    source: Transaction,
    target: Transaction?,
    confidence: RelationshipConfidence,
    reasoning: List<String>,
    engineVersion: Int,
    merchantMatch: Boolean = false,
): FinancialRelationship {
    val ids = listOfNotNull(source.id, target?.id)
    val diagnostics = RelationshipDiagnostics(
        relationshipType = type.name,
        confidence = confidence.value,
        reasoning = reasoning,
        matchedTransactionIds = ids,
        timeDifferenceSeconds = target?.let { secondsBetween(source, it) },
        amountDifferenceMinor = target?.let { amountDiffMinor(source, it) },
        merchantMatch = merchantMatch,
        cardMatch = target?.let { sameCard(source, it) } ?: false,
        accountMatch = target?.let { sameAccount(source, it) } ?: false,
        decision = "${type.name} @ ${confidence.band}",
    )
    val rid = "$key:${source.id}:${target?.id ?: "single"}"
    return FinancialRelationship(
        relationshipId = rid,
        type = type,
        sourceTransactionId = source.id,
        targetTransactionId = target?.id,
        confidence = confidence,
        reasoning = reasoning,
        createdByEngineVersion = engineVersion,
        diagnostics = diagnostics,
    )
}

/**
 * REFUND -> matching prior PURCHASE. A refund transaction (type REFUND, or an
 * income whose raw text says refund) tied to an earlier expense of equal amount.
 */
class RefundOfPurchaseResolver : RelationshipResolver {
    override val key = "refund_of_purchase"
    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val txns = ctx.transactions
        val refunds = txns.filter { it.type == TransactionType.REFUND }
        for (refund in refunds) {
            // The purchase must precede the refund and match amount.
            val purchase = txns
                .filter { it.type == TransactionType.EXPENSE && it.timestamp <= refund.timestamp }
                .filter { sameAmount(it, refund) }
                .maxByOrNull { it.timestamp } ?: continue
            val merchantMatch = ctx.sameCanonicalMerchant(purchase, refund) ||
                ctx.rawTextOverlap(purchase, refund)
            val days = daysBetween(purchase, refund)
            val conf = when {
                merchantMatch && days <= 30 -> RelationshipConfidence.high(90)
                days <= 30 -> RelationshipConfidence.medium(68)
                else -> RelationshipConfidence.low(45)
            }
            val reasons = buildList {
                add("Refund amount equals prior purchase amount")
                if (merchantMatch) add("Merchant matches between purchase and refund")
                add("$days day(s) between purchase and refund")
            }
            out += build(key, RelationshipType.REFUND_OF_PURCHASE, refund, purchase, conf, reasons, ctx.engineVersion, merchantMatch)
        }
        return out
    }
}

/**
 * CREDIT CARD PAYMENT: an outgoing debit/transfer from one account "towards" a
 * card, paired with a CONFIRMATION_OF_PAYMENT if a matching payment-received
 * transaction exists on the card side.
 */
class CreditCardPaymentResolver : RelationshipResolver {
    override val key = "credit_card_payment"
    private val cardWords = listOf("credit card", "card payment", "towards", "outstanding")

    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val txns = ctx.transactions
        val payments = txns.filter {
            (it.type == TransactionType.TRANSFER || it.type == TransactionType.EXPENSE) &&
                (it.merchantOrRawText?.lowercase()?.let { t -> cardWords.any { w -> t.contains(w) } } ?: false)
        }
        for (pay in payments) {
            // A confirmation is an income/transfer near in time with same amount on a different account.
            val confirmation = txns
                .filter { it.id != pay.id && sameAmount(it, pay) }
                .filter { it.accountId != pay.accountId }
                .filter { secondsBetween(pay, it) <= 3 * 86_400L }
                .minByOrNull { secondsBetween(pay, it) }
            if (confirmation != null) {
                val conf = RelationshipConfidence.high(88)
                val reasons = listOf(
                    "Outgoing card payment matched to payment-received confirmation",
                    "Same amount across two accounts within 3 days",
                )
                out += build(key, RelationshipType.CREDIT_CARD_PAYMENT, pay, confirmation, conf, reasons, ctx.engineVersion)
                out += build("confirmation_of_payment", RelationshipType.CONFIRMATION_OF_PAYMENT, confirmation, pay, RelationshipConfidence.high(85), listOf("Confirms the matched card payment"), ctx.engineVersion)
            } else {
                val conf = RelationshipConfidence.medium(60)
                out += build(key, RelationshipType.CREDIT_CARD_PAYMENT, pay, null, conf, listOf("Payment towards a credit card (no confirmation found)"), ctx.engineVersion)
            }
        }
        return out
    }
}

/**
 * TRANSFER / SAVINGS MOVEMENT: a TRANSFER out of one account matched to a
 * same-amount movement into another account. If the destination raw text signals
 * savings/investment, classify accordingly.
 */
class TransferBetweenAccountsResolver : RelationshipResolver {
    override val key = "transfer_between_accounts"
    private val savingsWords = listOf("saving", "savings")
    private val investWords = listOf("investment", "mutual fund", "brokerage", "portfolio", "sip")

    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val transfers = ctx.transactions.filter { it.type == TransactionType.TRANSFER }
        val used = mutableSetOf<Long>()
        for (src in transfers) {
            if (src.id in used) continue
            val dst = ctx.transactions
                .filter { it.id != src.id && it.id !in used }
                .filter { sameAmount(it, src) }
                .filter { it.accountId != src.accountId }
                .filter { secondsBetween(src, it) <= 2 * 86_400L }
                .minByOrNull { secondsBetween(src, it) } ?: continue
            used += src.id; used += dst.id
            val raw = (src.merchantOrRawText.orEmpty() + " " + dst.merchantOrRawText.orEmpty()).lowercase()
            val (type, reason) = when {
                investWords.any { raw.contains(it) } ->
                    RelationshipType.INVESTMENT_CONTRIBUTION to "Transfer into an investment destination"
                savingsWords.any { raw.contains(it) } ->
                    RelationshipType.SAVINGS_MOVEMENT to "Transfer into a savings destination"
                else ->
                    RelationshipType.TRANSFER_BETWEEN_ACCOUNTS to "Same-amount movement across two accounts"
            }
            val conf = RelationshipConfidence.high(84)
            out += build(key, type, src, dst, conf, listOf(reason, "Matched within 2 days, equal amount"), ctx.engineVersion)
        }
        return out
    }
}

/**
 * SALARY FUNDS EXPENSE: a salary INCOME followed by the expenses it plausibly
 * funds (expenses after the salary, before the next income, same account).
 */
class SalaryFundsExpenseResolver : RelationshipResolver {
    override val key = "salary_funds_expense"
    private val salaryWords = listOf("salary", "payroll", "wps")

    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val txns = ctx.transactions
        val salaries = txns.filter {
            it.type == TransactionType.INCOME &&
                (it.merchantOrRawText?.lowercase()?.let { t -> salaryWords.any { w -> t.contains(w) } } ?: false)
        }
        for (salary in salaries) {
            val nextIncome = txns
                .filter { it.type == TransactionType.INCOME && it.timestamp > salary.timestamp }
                .minByOrNull { it.timestamp }
            val fundedExpenses = txns.filter {
                it.type == TransactionType.EXPENSE &&
                    it.accountId == salary.accountId &&
                    it.timestamp > salary.timestamp &&
                    (nextIncome == null || it.timestamp < nextIncome.timestamp)
            }
            for (exp in fundedExpenses) {
                val days = daysBetween(salary, exp)
                val conf = if (days <= 31) RelationshipConfidence.medium(66) else RelationshipConfidence.low(40)
                out += build(key, RelationshipType.SALARY_FUNDS_EXPENSE, salary, exp, conf, listOf("Expense follows salary within the same pay cycle", "$days day(s) after salary"), ctx.engineVersion)
            }
        }
        return out
    }
}

/**
 * RECURRING MERCHANT + SUBSCRIPTION: the same canonical merchant appearing on
 * multiple expenses. If amounts are equal and roughly monthly, it is a
 * subscription; otherwise a recurring merchant. Single-transaction relationships
 * (target null) tagged on each occurrence after the first.
 */
class RecurringMerchantResolver : RelationshipResolver {
    override val key = "recurring_merchant"

    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val expenses = ctx.transactions.filter { it.type == TransactionType.EXPENSE }
        val groups = expenses.groupBy { ctx.canonicalMerchant(it) }
        for ((merchant, list) in groups) {
            if (merchant == null || list.size < 2) continue
            val sorted = list.sortedBy { it.timestamp }
            val equalAmounts = sorted.map { it.amount.minorUnits }.toSet().size == 1
            val gaps = sorted.zipWithNext { a, b -> daysBetween(a, b) }
            val monthly = gaps.isNotEmpty() && gaps.all { it in 25..35 }
            for (i in 1 until sorted.size) {
                val cur = sorted[i]
                val prev = sorted[i - 1]
                val (type, conf, reason) = when {
                    equalAmounts && monthly -> Triple(RelationshipType.SUBSCRIPTION, RelationshipConfidence.high(88), "Equal monthly charge from $merchant")
                    monthly -> Triple(RelationshipType.RECURRING_MERCHANT, RelationshipConfidence.high(82), "Monthly recurring charge from $merchant")
                    else -> Triple(RelationshipType.RECURRING_MERCHANT, RelationshipConfidence.medium(62), "Repeat charge from $merchant")
                }
                out += build(key, type, cur, prev, conf, listOf(reason), ctx.engineVersion, merchantMatch = true)
            }
        }
        return out
    }
}

/**
 * RECURRING BILL: same account, roughly monthly, raw text signals a utility/bill
 * even when the merchant does not resolve to a canonical brand.
 */
class RecurringBillResolver : RelationshipResolver {
    override val key = "recurring_bill"
    private val billWords = listOf("dewa", "electricity", "water", "utility", "bill", "etisalat", "du ", "internet", "gas")

    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val bills = ctx.transactions.filter {
            it.type == TransactionType.EXPENSE &&
                (it.merchantOrRawText?.lowercase()?.let { t -> billWords.any { w -> t.contains(w) } } ?: false)
        }
        // Group by account; look for monthly repetition.
        val byAccount = bills.groupBy { it.accountId }
        for ((_, list) in byAccount) {
            val sorted = list.sortedBy { it.timestamp }
            for (i in 1 until sorted.size) {
                val cur = sorted[i]; val prev = sorted[i - 1]
                val days = daysBetween(prev, cur)
                if (days in 25..35) {
                    out += build(key, RelationshipType.RECURRING_BILL, cur, prev, RelationshipConfidence.high(80), listOf("Monthly utility/bill payment on the same account", "$days day cadence"), ctx.engineVersion)
                }
            }
        }
        return out
    }
}

/** CASH WITHDRAWAL: single-transaction classification from ATM raw text. */
class CashWithdrawalResolver : RelationshipResolver {
    override val key = "cash_withdrawal"
    private val atmWords = listOf("atm", "cash withdrawal", "cash withdrawn")
    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> =
        ctx.transactions.filter {
            it.type == TransactionType.EXPENSE &&
                (it.merchantOrRawText?.lowercase()?.let { t -> atmWords.any { w -> t.contains(w) } } ?: false)
        }.map {
            build(key, RelationshipType.CASH_WITHDRAWAL, it, null, RelationshipConfidence.high(85), listOf("ATM cash withdrawal"), ctx.engineVersion)
        }
}

/** INTEREST CREDIT: single-transaction classification from interest raw text. */
class InterestCreditResolver : RelationshipResolver {
    override val key = "interest_credit"
    private val words = listOf("interest earned", "interest credited", "profit credited", "profit earned")
    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> =
        ctx.transactions.filter {
            it.type == TransactionType.INCOME &&
                (it.merchantOrRawText?.lowercase()?.let { t -> words.any { w -> t.contains(w) } } ?: false)
        }.map {
            build(key, RelationshipType.INTEREST_CREDIT, it, null, RelationshipConfidence.high(86), listOf("Interest/profit credited"), ctx.engineVersion)
        }
}

/**
 * LOAN REPAYMENT + INSTALLMENT: EMI/loan-repayment expenses. Equal recurring
 * amounts marked as installment payments; a single repayment marked as loan
 * repayment.
 */
class LoanRepaymentResolver : RelationshipResolver {
    override val key = "loan_repayment"
    private val words = listOf("emi", "loan repayment", "loan installment", "installment", "instalment")
    override fun resolve(ctx: RelationshipContext): List<FinancialRelationship> {
        val out = mutableListOf<FinancialRelationship>()
        val loans = ctx.transactions.filter {
            it.type == TransactionType.EXPENSE &&
                (it.merchantOrRawText?.lowercase()?.let { t -> words.any { w -> t.contains(w) } } ?: false)
        }.sortedBy { it.timestamp }
        if (loans.isEmpty()) return out
        for (i in loans.indices) {
            val cur = loans[i]
            val prev = if (i > 0) loans[i - 1] else null
            if (prev != null && sameAmount(prev, cur)) {
                out += build(key, RelationshipType.INSTALLMENT_PAYMENT, cur, prev, RelationshipConfidence.high(84), listOf("Equal recurring loan installment"), ctx.engineVersion)
            } else {
                out += build(key, RelationshipType.LOAN_REPAYMENT, cur, null, RelationshipConfidence.medium(66), listOf("Loan repayment / EMI"), ctx.engineVersion)
            }
        }
        return out
    }
}


package com.sherif.ledger.feature.storygraph

import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.component.graph.GraphData
import com.sherif.ledger.core.designsystem.component.graph.GraphEdge
import com.sherif.ledger.core.designsystem.component.graph.GraphNode
import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.Budget
import com.sherif.ledger.core.domain.model.Goal
import com.sherif.ledger.core.domain.model.Tag
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.isOutflow
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.relationship.RelationshipEngine
import com.sherif.ledger.feature.relationship.RelationshipType
import javax.inject.Inject

/** What a node represents. Used for colour and for the legend. */
enum class StoryNodeKind { ACCOUNT, MERCHANT, CATEGORY, TAG, BUDGET, GOAL, INCOME }

/**
 * One real transaction behind a node, pre-formatted for display.
 *
 * Carries [id] so the graph can hand off to Transaction Details — the graph is a
 * way *into* the ledger, not a dead end.
 */
data class GraphTransactionRef(
    val id: Long,
    val merchant: String,
    val amount: String,
    val date: String,
    val isOutflow: Boolean,
)

/**
 * The graph, plus the transactions sitting behind each node.
 *
 * Deliberately NOT folded into GraphNode. The layout and canvas are generic and
 * know nothing about money; teaching GraphNode about transactions would put
 * finance into the reusable engine and undo the separation the whole design rests
 * on. The mapping lives here, in the feature, keyed by the same node ids.
 */
data class StoryGraphResult(
    val graph: GraphData,
    val transactionsByNode: Map<String, List<GraphTransactionRef>>,
) {
    companion object {
        val EMPTY = StoryGraphResult(GraphData.EMPTY, emptyMap())
    }
}

/**
 * Turns real Ledger data into a graph.
 *
 * **This is where all the finance lives.** The layout engine and the canvas are
 * generic on purpose, so adding an entity type later is a change to this file
 * alone — which is what "design so additional entity types can be added without
 * changing the graph engine" actually requires.
 *
 * Two deliberate departures from a naive rendering:
 *
 * 1. **Transactions are edges, not nodes.** Drawing 380 transaction nodes
 *    produces a hairball nobody can read, and the relationships a user actually
 *    wants to see — which account funds which merchant, what category that rolls
 *    into — are properties of the *aggregate*, not of individual charges. Each
 *    account→merchant link therefore carries the count and total behind it, and
 *    edge strength is the share of spend. Per-transaction expansion belongs
 *    behind a focused node, which is the "expand additional relationships"
 *    interaction, not the default view.
 *
 * 2. **Only merchants above a floor appear.** A one-off AED 3 charge is real but
 *    adds a leaf that will never be looked at, and a hundred of them bury the
 *    structure. The floor is a share of total spend rather than a fixed count, so
 *    it adapts to how much history exists.
 *
 * Nothing here is invented: every node corresponds to a row that exists, and
 * entity types Ledger does not have are simply absent rather than stubbed.
 */
class StoryGraphBuilder @Inject constructor(
    private val relationshipEngine: RelationshipEngine,
) {

    fun build(
        transactions: List<Transaction>,
        accounts: List<Account>,
        merchantNameOf: (Transaction) -> String,
        categoryOf: (Transaction) -> String,
        tagsByTransaction: Map<Long, List<Tag>>,
        budgets: List<Budget>,
        goals: List<Goal>,
        palette: StoryGraphPalette,
    ): StoryGraphResult {
        if (transactions.isEmpty()) return StoryGraphResult.EMPTY

        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()
        // Which real transactions sit behind each node, so selecting one can show
        // them and hand off to Transaction Details.
        val behind = mutableMapOf<String, MutableList<Transaction>>()
        fun attribute(nodeId: String, list: List<Transaction>) {
            behind.getOrPut(nodeId) { mutableListOf() }.addAll(list)
        }
        val accountsById = accounts.associateBy { it.id }

        val outflows = transactions.filter { it.isOutflow }
        val totalSpend = outflows.sumOf { it.amount.minorUnits }.coerceAtLeast(1L)

        // ---- Accounts ----
        val usedAccountIds = transactions.map { it.accountId }.toSet()
        accounts.filter { it.id in usedAccountIds }.forEach { account ->
            val own = transactions.filter { it.accountId == account.id }
            attribute(accountId(account.id), own)
            nodes += GraphNode(
                id = accountId(account.id),
                label = account.name,
                kind = StoryNodeKind.ACCOUNT.name,
                subtitle = "${own.size} transactions",
                color = palette.account,
                weight = 1f,
                isNavigable = true,
            )
        }

        // ---- Merchants, above a share-of-spend floor ----
        val spendByMerchant = outflows.groupBy(merchantNameOf)
            .mapValues { (_, list) -> list.sumOf { it.amount.minorUnits } }
        val floor = (totalSpend * MERCHANT_SHARE_FLOOR).toLong()
        val keptMerchants = spendByMerchant.filterValues { it >= floor }.keys

        keptMerchants.forEach { merchant ->
            val spend = spendByMerchant[merchant] ?: 0L
            val sample = outflows.first { merchantNameOf(it) == merchant }
            attribute(merchantId(merchant), outflows.filter { merchantNameOf(it) == merchant })
            nodes += GraphNode(
                id = merchantId(merchant),
                label = merchant,
                kind = StoryNodeKind.MERCHANT.name,
                subtitle = MoneyFormatter.format(
                    com.sherif.ledger.core.domain.model.Money(spend, sample.amount.currencyCode),
                    includeSymbol = true,
                ),
                color = palette.merchant,
                weight = (spend.toFloat() / totalSpend).coerceIn(0.2f, 1f),
                isNavigable = true,
            )
        }

        // ---- Categories ----
        val keptCategories = outflows
            .filter { merchantNameOf(it) in keptMerchants }
            .map(categoryOf)
            .filter { it != "UNKNOWN" }
            .toSet()
        keptCategories.forEach { category ->
            attribute(
                categoryId(category),
                outflows.filter { merchantNameOf(it) in keptMerchants && categoryOf(it) == category },
            )
            nodes += GraphNode(
                id = categoryId(category),
                label = prettify(category),
                kind = StoryNodeKind.CATEGORY.name,
                color = palette.category,
                weight = 0.6f,
            )
        }

        // ---- account → merchant, and merchant → category ----
        outflows
            .filter { merchantNameOf(it) in keptMerchants }
            .groupBy { it.accountId to merchantNameOf(it) }
            .forEach { (key, list) ->
                val (accId, merchant) = key
                if (accId !in usedAccountIds || accountsById[accId] == null) return@forEach
                val spend = list.sumOf { it.amount.minorUnits }
                edges += GraphEdge(
                    fromId = accountId(accId),
                    toId = merchantId(merchant),
                    label = "${list.size} payments",
                    strength = (spend.toFloat() / totalSpend).coerceIn(0.15f, 1f),
                )
            }

        outflows
            .filter { merchantNameOf(it) in keptMerchants }
            .map { merchantNameOf(it) to categoryOf(it) }
            .filter { it.second in keptCategories }
            .toSet()
            .forEach { (merchant, category) ->
                edges += GraphEdge(merchantId(merchant), categoryId(category), label = "is", strength = 0.35f)
            }

        // ---- Cross-account money movement (RelationshipEngine) ----
        //
        // Everything above is one account's own spend aggregated outward. This is
        // the one place the graph draws a line BETWEEN two accounts — a card
        // payment, an internal transfer, money moved to savings or invested. It is
        // what makes the picture closer to a real incident graph: a path money
        // actually took, not just where each account's own spend went. Built from
        // the SAME RelationshipEngine every other screen's narrative comes from —
        // no separate matching logic invented here.
        run {
            val txnById = transactions.associateBy { it.id }
            relationshipEngine.analyze(transactions)
                .filter { it.type in CROSS_ACCOUNT_RELATIONSHIP_TYPES && it.targetTransactionId != null }
                .mapNotNull { rel ->
                    val source = txnById[rel.sourceTransactionId] ?: return@mapNotNull null
                    val target = txnById[rel.targetTransactionId] ?: return@mapNotNull null
                    if (source.accountId == target.accountId) return@mapNotNull null
                    if (source.accountId !in usedAccountIds || target.accountId !in usedAccountIds) return@mapNotNull null
                    Triple(source.accountId, target.accountId, rel.type)
                }
                .groupingBy { it }
                .eachCount()
                .forEach { (movement, count) ->
                    val (fromAcc, toAcc, type) = movement
                    edges += GraphEdge(
                        fromId = accountId(fromAcc),
                        toId = accountId(toAcc),
                        label = movementLabel(type, count),
                        strength = 0.75f,
                    )
                }
        }

        // ---- Income: where the money comes from, on top of where it goes ----
        val inflows = transactions.filterNot { it.isOutflow }
        if (inflows.isNotEmpty()) {
            val total = inflows.sumOf { it.amount.minorUnits }
            attribute(INCOME_ID, inflows)
            nodes += GraphNode(
                id = INCOME_ID,
                label = "Income",
                kind = StoryNodeKind.INCOME.name,
                subtitle = MoneyFormatter.format(
                    com.sherif.ledger.core.domain.model.Money(total, inflows.first().amount.currencyCode),
                    includeSymbol = true,
                ),
                color = palette.income,
                weight = 1f,
            )
            inflows.groupBy { it.accountId }.forEach { (accId, list) ->
                if (accountsById[accId] == null) return@forEach
                edges += GraphEdge(
                    fromId = INCOME_ID,
                    toId = accountId(accId),
                    label = "funds",
                    strength = (list.sumOf { it.amount.minorUnits }.toFloat() / total).coerceIn(0.3f, 1f),
                )
            }
        }

        // ---- Tags: the only user-authored edges in the whole graph ----
        val tagUse = tagsByTransaction.values.flatten().groupingBy { it.id }.eachCount()
        tagsByTransaction.values.flatten().distinctBy { it.id }.forEach { tag ->
            nodes += GraphNode(
                id = tagId(tag.id),
                label = tag.name,
                kind = StoryNodeKind.TAG.name,
                subtitle = "${tagUse[tag.id] ?: 0} tagged",
                color = palette.tag,
                weight = 0.45f,
            )
        }
        // A tag attaches to a transaction, but transactions are not nodes here, so
        // the honest aggregate edge is tag → the merchant that transaction paid.
        tagsByTransaction.forEach { (transactionId, tags) ->
            val transaction = transactions.firstOrNull { it.id == transactionId } ?: return@forEach
            tags.forEach { tag -> attribute(tagId(tag.id), listOf(transaction)) }
            val merchant = merchantNameOf(transaction)
            if (merchant !in keptMerchants) return@forEach
            tags.forEach { tag ->
                edges += GraphEdge(tagId(tag.id), merchantId(merchant), label = "tagged", strength = 0.3f)
            }
        }

        // ---- Budgets sit on the category they cap ----
        budgets.forEach { budget ->
            if (categoryId(budget.category.uppercase()) !in nodes.map { it.id }) return@forEach
            nodes += GraphNode(
                id = budgetId(budget.category),
                label = "${prettify(budget.category)} budget",
                kind = StoryNodeKind.BUDGET.name,
                subtitle = MoneyFormatter.format(budget.limit, includeSymbol = true),
                color = palette.budget,
                weight = 0.5f,
            )
            attribute(budgetId(budget.category), behind[categoryId(budget.category.uppercase())].orEmpty())
            edges += GraphEdge(budgetId(budget.category), categoryId(budget.category.uppercase()), "caps", 0.5f)
        }

        // ---- Goals hang off the account funding them ----
        goals.forEach { goal ->
            if (goal.accountId !in usedAccountIds) return@forEach
            nodes += GraphNode(
                id = goalId(goal.id),
                label = goal.name,
                kind = StoryNodeKind.GOAL.name,
                subtitle = MoneyFormatter.format(goal.target, includeSymbol = true),
                color = palette.goal,
                weight = 0.6f,
            )
            attribute(goalId(goal.id), behind[accountId(goal.accountId)].orEmpty())
            edges += GraphEdge(goalId(goal.id), accountId(goal.accountId), "funded by", 0.6f)
        }

        // Drop any edge whose endpoints did not survive the floors above, so the
        // canvas never has to defend itself against a dangling link.
        val ids = nodes.map { it.id }.toSet()
        val graph = GraphData(
            nodes = nodes.distinctBy { it.id },
            edges = edges.filter { it.fromId in ids && it.toId in ids }.distinct(),
        )
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM")
        val zone = java.time.ZoneId.systemDefault()
        return StoryGraphResult(
            graph = graph,
            transactionsByNode = behind
                .filterKeys { it in ids }
                .mapValues { (_, list) ->
                    // Newest first, and capped: a node with 226 transactions behind
                    // it does not need all of them in a panel the size of a card.
                    list.distinctBy { it.id }
                        .sortedByDescending { it.timestamp }
                        .take(MAX_TRANSACTIONS_PER_NODE)
                        .map { txn ->
                            GraphTransactionRef(
                                id = txn.id,
                                merchant = merchantNameOf(txn),
                                amount = MoneyFormatter.format(txn.amount, includeSymbol = true),
                                date = txn.timestamp.atZone(zone).format(formatter),
                                isOutflow = txn.isOutflow,
                            )
                        }
                },
        )
    }

    private fun prettify(raw: String) =
        raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    private fun movementLabel(type: RelationshipType, count: Int): String {
        val base = when (type) {
            RelationshipType.CREDIT_CARD_PAYMENT -> "card payment"
            RelationshipType.TRANSFER_BETWEEN_ACCOUNTS -> "transfer"
            RelationshipType.SAVINGS_MOVEMENT -> "to savings"
            RelationshipType.INVESTMENT_CONTRIBUTION -> "invested"
            else -> "moved"
        }
        return if (count > 1) "$base ($count)" else base
    }

    companion object {
        const val INCOME_ID = "income"

        /** A panel is not a list screen; the rest stay one tap away in Transactions. */
        const val MAX_TRANSACTIONS_PER_NODE = 12

        /** A merchant must account for at least this share of spend to earn a node. */
        private const val MERCHANT_SHARE_FLOOR = 0.012f

        /**
         * Relationship types that connect two DIFFERENT accounts, and are
         * therefore drawable as an edge between them. The rest (recurring
         * merchant, salary-funds-expense, cash withdrawal, interest, ...) describe
         * a single account's own activity and already show up through the
         * account→merchant/category edges above.
         */
        private val CROSS_ACCOUNT_RELATIONSHIP_TYPES = setOf(
            RelationshipType.CREDIT_CARD_PAYMENT,
            RelationshipType.TRANSFER_BETWEEN_ACCOUNTS,
            RelationshipType.SAVINGS_MOVEMENT,
            RelationshipType.INVESTMENT_CONTRIBUTION,
        )

        fun accountId(id: Long) = "account:$id"
        fun merchantId(name: String) = "merchant:$name"
        fun categoryId(name: String) = "category:$name"
        fun tagId(id: Long) = "tag:$id"
        fun budgetId(category: String) = "budget:$category"
        fun goalId(id: Long) = "goal:$id"
    }
}

/** Colours per entity type, supplied by the theme so the graph works in both. */
data class StoryGraphPalette(
    val account: Color,
    val merchant: Color,
    val category: Color,
    val tag: Color,
    val budget: Color,
    val goal: Color,
    val income: Color,
)

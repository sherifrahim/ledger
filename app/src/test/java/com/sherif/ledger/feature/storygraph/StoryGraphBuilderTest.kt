package com.sherif.ledger.feature.storygraph

import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.IngestionSource
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.Tag
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StoryGraphBuilderTest {

    private val builder = StoryGraphBuilder()

    private val palette = StoryGraphPalette(
        account = Color.Gray, merchant = Color.Gray, category = Color.Gray,
        tag = Color.Gray, budget = Color.Gray, goal = Color.Gray, income = Color.Gray,
    )

    private fun txn(
        id: Long,
        accountId: Long,
        amountMinor: Long,
        merchant: String,
        type: TransactionType = TransactionType.EXPENSE,
    ) = Transaction(
        id = id, accountId = accountId, brandId = null, categoryId = null,
        amount = Money(amountMinor, CurrencyCode.AED), type = type,
        timestamp = Instant.parse("2026-08-01T10:00:00Z").plusSeconds(id),
        source = IngestionSource.SMS, rawText = merchant, merchantText = merchant,
        fingerprint = "fp-$id",
    )

    private fun account(id: Long, name: String) =
        Account(id, name, AccountType.CHECKING, Money.zero(CurrencyCode.AED), null, null)

    private fun build(
        transactions: List<Transaction>,
        accounts: List<Account>,
        tags: Map<Long, List<Tag>> = emptyMap(),
        budgets: List<com.sherif.ledger.core.domain.model.Budget> = emptyList(),
        goals: List<com.sherif.ledger.core.domain.model.Goal> = emptyList(),
    ) = builder.build(
        transactions = transactions,
        accounts = accounts,
        merchantNameOf = { it.merchantText ?: "?" },
        categoryOf = { "GROCERIES" },
        tagsByTransaction = tags,
        budgets = budgets,
        goals = goals,
        palette = palette,
    ).graph

    @Test
    fun `no transactions means no graph, not an empty frame of nodes`() {
        assertTrue(build(emptyList(), listOf(account(1L, "ADCB"))).isEmpty)
    }

    @Test
    fun `accounts, merchants and categories become nodes with edges between them`() {
        val graph = build(
            transactions = List(10) { txn(it.toLong(), 1L, 10_000L, "Carrefour") },
            accounts = listOf(account(1L, "ADCB")),
        )

        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.accountId(1L) })
        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.merchantId("Carrefour") })
        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.categoryId("GROCERIES") })
        assertTrue(
            graph.edges.any {
                it.fromId == StoryGraphBuilder.accountId(1L) && it.toId == StoryGraphBuilder.merchantId("Carrefour")
            },
        )
    }

    @Test
    fun `a trivial one-off merchant does not earn a node`() {
        // Otherwise a hundred AED-3 charges bury the structure the graph exists to show.
        val graph = build(
            transactions = List(20) { txn(it.toLong(), 1L, 100_000L, "Carrefour") } +
                txn(99L, 1L, 100L, "One Off Kiosk"),
            accounts = listOf(account(1L, "ADCB")),
        )

        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.merchantId("Carrefour") })
        assertFalse(graph.nodes.any { it.id == StoryGraphBuilder.merchantId("One Off Kiosk") })
    }

    @Test
    fun `income appears as where the money comes from`() {
        val graph = build(
            transactions = listOf(
                txn(1L, 1L, 600_000L, "Salary", type = TransactionType.INCOME),
                txn(2L, 1L, 10_000L, "Carrefour"),
            ),
            accounts = listOf(account(1L, "ADCB")),
        )

        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.INCOME_ID })
        assertTrue(
            graph.edges.any {
                it.fromId == StoryGraphBuilder.INCOME_ID && it.toId == StoryGraphBuilder.accountId(1L)
            },
        )
    }

    @Test
    fun `tags connect to the merchant they were applied against`() {
        val graph = build(
            transactions = List(5) { txn(it.toLong(), 1L, 50_000L, "Carrefour") },
            accounts = listOf(account(1L, "ADCB")),
            tags = mapOf(0L to listOf(Tag(7L, "Reimbursable"))),
        )

        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.tagId(7L) })
        assertTrue(
            graph.edges.any {
                it.fromId == StoryGraphBuilder.tagId(7L) && it.toId == StoryGraphBuilder.merchantId("Carrefour")
            },
        )
    }

    @Test
    fun `no edge is ever left pointing at a node that was filtered out`() {
        // The canvas must never have to defend itself against a dangling link.
        val graph = build(
            transactions = List(20) { txn(it.toLong(), 1L, 100_000L, "Carrefour") } +
                txn(99L, 1L, 50L, "Tiny") ,
            accounts = listOf(account(1L, "ADCB")),
            tags = mapOf(99L to listOf(Tag(3L, "Odd"))),
        )

        val ids = graph.nodes.map { it.id }.toSet()
        graph.edges.forEach { edge ->
            assertTrue("dangling from ${edge.fromId}", edge.fromId in ids)
            assertTrue("dangling to ${edge.toId}", edge.toId in ids)
        }
    }

    @Test
    fun `an account with no activity is not drawn`() {
        val graph = build(
            transactions = List(5) { txn(it.toLong(), 1L, 50_000L, "Carrefour") },
            accounts = listOf(account(1L, "ADCB"), account(2L, "Dormant")),
        )

        assertTrue(graph.nodes.any { it.id == StoryGraphBuilder.accountId(1L) })
        assertFalse(graph.nodes.any { it.id == StoryGraphBuilder.accountId(2L) })
    }

    private fun buildResult(
        transactions: List<Transaction>,
        accounts: List<Account>,
        tags: Map<Long, List<Tag>> = emptyMap(),
    ) = builder.build(
        transactions = transactions,
        accounts = accounts,
        merchantNameOf = { it.merchantText ?: "?" },
        categoryOf = { "GROCERIES" },
        tagsByTransaction = tags,
        budgets = emptyList(),
        goals = emptyList(),
        palette = palette,
    )

    @Test
    fun `a node carries the real transactions behind it, newest first`() {
        // The graph has to be a way INTO the ledger. A node that can only say
        // something exists is a dead end.
        val result = buildResult(
            transactions = List(4) { txn(it.toLong(), 1L, 50_000L, "Carrefour") },
            accounts = listOf(account(1L, "ADCB")),
        )

        val behind = result.transactionsByNode[StoryGraphBuilder.merchantId("Carrefour")]!!
        assertEquals(4, behind.size)
        // Newest first, so the panel opens on what just happened.
        assertEquals(listOf(3L, 2L, 1L, 0L), behind.map { it.id })
        assertTrue(behind.all { it.isOutflow })
        assertTrue(behind.all { it.amount.isNotBlank() })
    }

    @Test
    fun `the transaction list per node is capped`() {
        // A 226-transaction account must not try to render 226 rows in a card.
        val result = buildResult(
            transactions = List(40) { txn(it.toLong(), 1L, 50_000L, "Carrefour") },
            accounts = listOf(account(1L, "ADCB")),
        )

        assertEquals(
            StoryGraphBuilder.MAX_TRANSACTIONS_PER_NODE,
            result.transactionsByNode[StoryGraphBuilder.accountId(1L)]!!.size,
        )
    }

    @Test
    fun `no transaction list is kept for a node that was filtered out`() {
        val result = buildResult(
            transactions = List(20) { txn(it.toLong(), 1L, 100_000L, "Carrefour") } + txn(99L, 1L, 50L, "Tiny"),
            accounts = listOf(account(1L, "ADCB")),
        )

        val ids = result.graph.nodes.map { it.id }.toSet()
        assertTrue(result.transactionsByNode.keys.all { it in ids })
    }

    @Test
    fun `node ids are unique so the layout can key positions by them`() {
        val graph = build(
            transactions = List(30) { txn(it.toLong(), 1L, 50_000L, "Carrefour") },
            accounts = listOf(account(1L, "ADCB")),
            tags = mapOf(1L to listOf(Tag(1L, "A")), 2L to listOf(Tag(1L, "A"))),
        )

        assertTrue(graph.nodes.map { it.id }.toSet().size == graph.nodes.size)
    }
}

package com.sherif.ledger.core.designsystem.component.graph

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ForceDirectedLayoutTest {

    private fun node(id: String) = GraphNode(id = id, label = id, kind = "test", color = Color.Gray)

    private fun graph(nodeIds: List<String>, edges: List<Pair<String, String>> = emptyList()) =
        GraphData(
            nodes = nodeIds.map { node(it) },
            edges = edges.map { GraphEdge(it.first, it.second) },
        )

    private fun distance(layout: ForceDirectedLayout, a: String, b: String): Float {
        val pa = layout.positionOf(a)!!
        val pb = layout.positionOf(b)!!
        return hypot(pa.x - pb.x, pa.y - pb.y)
    }

    @Test
    fun `unconnected nodes push apart`() {
        val layout = ForceDirectedLayout()
        val g = graph(listOf("a", "b"))
        layout.sync(g)
        // Force them together first so the test measures repulsion, not the seed.
        layout.place("a", 0f, 0f)
        layout.place("b", 5f, 0f)

        repeat(120) { layout.step(g) }

        assertTrue("Expected separation, got ${distance(layout, "a", "b")}", distance(layout, "a", "b") > 40f)
    }

    @Test
    fun `connected nodes settle near the spring length rather than flying apart`() {
        val layout = ForceDirectedLayout()
        val g = graph(listOf("a", "b"), edges = listOf("a" to "b"))
        layout.sync(g)
        layout.place("a", -600f, 0f)
        layout.place("b", 600f, 0f)

        repeat(400) { layout.step(g) }

        val d = distance(layout, "a", "b")
        assertTrue("Edge should pull them together, got $d", d < 400f)
    }

    @Test
    fun `coincident nodes do not produce NaN`() {
        // Exactly-equal positions give a zero distance and an infinite force; without
        // the nudge the positions become NaN and never recover.
        val layout = ForceDirectedLayout()
        val g = graph(listOf("a", "b", "c"))
        layout.sync(g)
        layout.place("a", 0f, 0f)
        layout.place("b", 0f, 0f)
        layout.place("c", 0f, 0f)

        repeat(60) { layout.step(g) }

        listOf("a", "b", "c").forEach { id ->
            val p = layout.positionOf(id)!!
            assertFalse("$id went NaN", p.x.isNaN() || p.y.isNaN())
            assertFalse("$id went infinite", p.x.isInfinite() || p.y.isInfinite())
        }
    }

    @Test
    fun `the layout eventually settles instead of jittering forever`() {
        val layout = ForceDirectedLayout()
        val g = graph(listOf("a", "b", "c", "d"), edges = listOf("a" to "b", "b" to "c", "c" to "d"))
        layout.sync(g)

        repeat(1_500) { layout.step(g) }

        assertTrue("Layout never settled — the canvas would burn frames forever", layout.isSettled)
    }

    @Test
    fun `a pinned node does not move`() {
        // The node under the user's finger must follow it exactly, not fight springs.
        val layout = ForceDirectedLayout()
        val g = graph(listOf("a", "b"), edges = listOf("a" to "b"))
        layout.sync(g)
        layout.place("a", 10f, 20f)

        repeat(80) { layout.step(g, pinned = "a") }

        val p = layout.positionOf("a")!!
        assertEquals(10f, p.x, 0.001f)
        assertEquals(20f, p.y, 0.001f)
    }

    @Test
    fun `positions survive a rebuild and vanished nodes are forgotten`() {
        // A graph that scattered itself every time it refreshed would be impossible
        // to build familiarity with.
        val layout = ForceDirectedLayout()
        layout.sync(graph(listOf("a", "b")))
        layout.place("a", 42f, 99f)

        layout.sync(graph(listOf("a", "c")))

        assertEquals(42f, layout.positionOf("a")!!.x, 0.001f)
        assertNull("b is gone and should not be retained", layout.positionOf("b"))
        assertNotNull("c is new and should be seeded", layout.positionOf("c"))
    }

    @Test
    fun `seeding is deterministic for the same graph`() {
        val g = graph(listOf("alpha", "beta", "gamma"))
        val first = ForceDirectedLayout().apply { sync(g) }.snapshot()
        val second = ForceDirectedLayout().apply { sync(g) }.snapshot()

        assertEquals(first.keys, second.keys)
        first.forEach { (id, p) ->
            assertEquals(p.x, second[id]!!.x, 0.001f)
            assertEquals(p.y, second[id]!!.y, 0.001f)
        }
    }

    @Test
    fun `no node is seeded exactly on the origin`() {
        // All-at-the-origin is the degenerate start that makes the first frame explode.
        val layout = ForceDirectedLayout()
        layout.sync(graph(listOf("a", "b", "c", "d", "e")))

        layout.snapshot().forEach { (id, p) ->
            assertTrue("$id seeded at the origin", hypot(p.x, p.y) > 1f)
        }
    }

    @Test
    fun `an empty graph is a no-op rather than a crash`() {
        val layout = ForceDirectedLayout()
        layout.sync(GraphData.EMPTY)
        layout.step(GraphData.EMPTY)

        assertNull(layout.bounds())
    }

    @Test
    fun `adjacency is undirected and ignores self-links`() {
        val g = GraphData(
            nodes = listOf(node("a"), node("b")),
            edges = listOf(GraphEdge("a", "b"), GraphEdge("a", "a")),
        )

        assertEquals(setOf("b"), g.neighbours["a"])
        assertEquals(setOf("a"), g.neighbours["b"])
        assertEquals(1, g.degree["a"])
    }
}

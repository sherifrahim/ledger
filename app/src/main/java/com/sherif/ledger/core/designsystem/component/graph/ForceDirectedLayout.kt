package com.sherif.ledger.core.designsystem.component.graph

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/** Where a node currently sits, and how fast it is moving. */
data class NodePosition(val x: Float, val y: Float, val vx: Float = 0f, val vy: Float = 0f)

/**
 * A force-directed layout, stepped one frame at a time.
 *
 * Three forces, which is the classic Fruchterman-Reingold arrangement:
 *  - **Repulsion** between every pair, so nodes do not pile up.
 *  - **Attraction** along each edge, so connected things sit together.
 *  - **Centering**, a gentle pull to the origin so the graph does not drift off
 *    into empty space over time.
 *
 * Deliberately a plain data class stepped by the caller rather than a coroutine
 * that owns its own clock: that makes it a pure function of its state, testable
 * without a frame loop, and lets the canvas stop stepping the moment the layout
 * settles instead of burning frames on a graph that has stopped moving.
 *
 * Repulsion is O(n²). For the graph sizes this screen actually produces (a few
 * hundred nodes after the builder's aggregation) that is cheaper than the
 * quad-tree it would take to avoid it, and honest about its limit: past roughly a
 * thousand nodes this needs Barnes-Hut, which is a change to this file alone.
 */
class ForceDirectedLayout(
    private val repulsion: Float = 9_000f,
    private val springLength: Float = 130f,
    private val springStiffness: Float = 0.045f,
    private val centering: Float = 0.006f,
    private val damping: Float = 0.86f,
    /** Below this total movement the layout is considered settled. */
    private val settleThreshold: Float = 0.35f,
) {

    private val positions = mutableMapOf<String, NodePosition>()
    private var lastMovement = Float.MAX_VALUE

    /** True once the graph has stopped meaningfully moving. */
    val isSettled: Boolean get() = lastMovement < settleThreshold

    fun positionOf(id: String): NodePosition? = positions[id]

    fun snapshot(): Map<String, NodePosition> = positions.toMap()

    /** Moves a node under the user's finger and freezes its velocity there. */
    fun place(id: String, x: Float, y: Float) {
        positions[id] = NodePosition(x, y, 0f, 0f)
        lastMovement = Float.MAX_VALUE // dragging re-energises the simulation
    }

    /**
     * Seeds any node that has no position yet, and forgets nodes that are gone.
     *
     * New nodes start on a ring rather than at the origin: dropped at a single
     * point they would all repel from exactly the same place, which explodes on
     * the first frame. The seed is deterministic per id, so the same graph lays
     * out the same way twice — a graph that rearranged itself on every open would
     * be impossible to build familiarity with.
     */
    fun sync(graph: GraphData) {
        positions.keys.retainAll(graph.nodesById.keys)
        graph.nodes.forEachIndexed { index, node ->
            if (positions.containsKey(node.id)) return@forEachIndexed
            val random = Random(node.id.hashCode())
            val angle = (index.toFloat() / graph.nodes.size.coerceAtLeast(1)) * TWO_PI +
                random.nextFloat() * 0.4f
            val radius = 140f + random.nextFloat() * 220f
            positions[node.id] = NodePosition(
                x = kotlin.math.cos(angle) * radius,
                y = kotlin.math.sin(angle) * radius,
            )
        }
        lastMovement = Float.MAX_VALUE
    }

    /**
     * Advances the simulation one frame.
     *
     * [pinned] is excluded from force integration entirely — a node the user is
     * dragging should follow the finger exactly, not fight the springs.
     */
    fun step(graph: GraphData, pinned: String? = null) {
        if (graph.isEmpty) return
        val ids = graph.nodes.map { it.id }
        val forces = HashMap<String, Pair<Float, Float>>(ids.size)
        ids.forEach { forces[it] = 0f to 0f }

        // Repulsion, computed once per unordered pair and applied to both.
        for (i in ids.indices) {
            val a = positions[ids[i]] ?: continue
            for (j in i + 1 until ids.size) {
                val b = positions[ids[j]] ?: continue
                var dx = a.x - b.x
                var dy = a.y - b.y
                var distanceSquared = dx * dx + dy * dy
                if (distanceSquared < MIN_DISTANCE_SQUARED) {
                    // Exactly-coincident nodes give an infinite force and a NaN
                    // position that never recovers. Nudge them apart instead.
                    dx = (Random(ids[i].hashCode() + j).nextFloat() - 0.5f)
                    dy = (Random(ids[j].hashCode() + i).nextFloat() - 0.5f)
                    distanceSquared = MIN_DISTANCE_SQUARED
                }
                val distance = sqrt(distanceSquared)
                val magnitude = repulsion / distanceSquared
                val fx = dx / distance * magnitude
                val fy = dy / distance * magnitude
                forces[ids[i]] = forces[ids[i]]!!.let { it.first + fx to it.second + fy }
                forces[ids[j]] = forces[ids[j]]!!.let { it.first - fx to it.second - fy }
            }
        }

        // Attraction along edges.
        graph.edges.forEach { edge ->
            val a = positions[edge.fromId] ?: return@forEach
            val b = positions[edge.toId] ?: return@forEach
            val dx = b.x - a.x
            val dy = b.y - a.y
            val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(0.01f)
            val displacement = distance - springLength
            val magnitude = displacement * springStiffness * (0.5f + edge.strength)
            val fx = dx / distance * magnitude
            val fy = dy / distance * magnitude
            forces[edge.fromId]?.let { forces[edge.fromId] = it.first + fx to it.second + fy }
            forces[edge.toId]?.let { forces[edge.toId] = it.first - fx to it.second - fy }
        }

        var movement = 0f
        ids.forEach { id ->
            if (id == pinned) return@forEach
            val position = positions[id] ?: return@forEach
            val (fxRaw, fyRaw) = forces[id] ?: return@forEach

            val fx = fxRaw - position.x * centering
            val fy = fyRaw - position.y * centering

            var vx = (position.vx + fx) * damping
            var vy = (position.vy + fy) * damping

            // Cap speed. Without it a dense cluster can fling a node across the
            // canvas in one frame, which reads as a glitch rather than as physics.
            val speed = sqrt(vx * vx + vy * vy)
            if (speed > MAX_SPEED) {
                val scale = MAX_SPEED / speed
                vx *= scale
                vy *= scale
            }

            positions[id] = NodePosition(position.x + vx, position.y + vy, vx, vy)
            movement += kotlin.math.abs(vx) + kotlin.math.abs(vy)
        }

        lastMovement = movement / ids.size.coerceAtLeast(1)
    }

    /** Drops every position so the next [sync] re-seeds from scratch. */
    fun reset() {
        positions.clear()
        lastMovement = Float.MAX_VALUE
    }

    /** The bounding box of the laid-out graph, for fit-to-screen. */
    fun bounds(): Bounds? {
        if (positions.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        positions.values.forEach {
            minX = min(minX, it.x)
            minY = min(minY, it.y)
            maxX = kotlin.math.max(maxX, it.x)
            maxY = kotlin.math.max(maxY, it.y)
        }
        return Bounds(minX, minY, maxX, maxY)
    }

    data class Bounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
        val width: Float get() = (maxX - minX).coerceAtLeast(1f)
        val height: Float get() = (maxY - minY).coerceAtLeast(1f)
        val centerX: Float get() = (minX + maxX) / 2f
        val centerY: Float get() = (minY + maxY) / 2f
    }

    private companion object {
        const val TWO_PI = 6.2831855f
        const val MIN_DISTANCE_SQUARED = 4f
        const val MAX_SPEED = 30f
    }
}

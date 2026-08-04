package com.sherif.ledger.core.designsystem.component.graph

import androidx.compose.ui.graphics.Color

/**
 * A node in a relationship graph.
 *
 * Deliberately knows nothing about finance. [kind] is an opaque string and
 * [subtitle] is pre-formatted by whoever built the graph, so adding tax records
 * or shared accounts later means teaching the *builder* about them, never this
 * engine. That is the whole point of the separation: the layout and the canvas
 * should be reusable for anything with entities and links between them.
 */
data class GraphNode(
    /** Stable across rebuilds — the layout keeps a node's position by this id, so
     *  a graph that refreshes does not scatter itself. */
    val id: String,
    val label: String,
    val kind: String,
    val subtitle: String? = null,
    val color: Color,
    /**
     * Visual weight, 0f..1f. Drives radius, so a node with more behind it reads as
     * more important without anyone hard-coding sizes per entity type.
     */
    val weight: Float = 0.5f,
    /** True for a node the user can open elsewhere in the app. */
    val isNavigable: Boolean = false,
)

/**
 * A directed link. [label] is the relationship in the user's language ("paid to",
 * "tagged with"), shown on demand rather than always — a graph with a caption on
 * every line is unreadable at any useful density.
 */
data class GraphEdge(
    val fromId: String,
    val toId: String,
    val label: String? = null,
    /** 0f..1f. Stronger links pull harder in the layout and draw brighter. */
    val strength: Float = 0.5f,
)

/**
 * A whole graph, plus the adjacency the canvas needs constantly (for highlight,
 * for neighbour lookup) computed once here rather than rebuilt every frame.
 */
data class GraphData(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
) {
    val nodesById: Map<String, GraphNode> = nodes.associateBy { it.id }

    /** Every node directly connected to a given node, in either direction. */
    val neighbours: Map<String, Set<String>> = buildMap {
        edges.forEach { edge ->
            if (edge.fromId == edge.toId) return@forEach
            merge(edge.fromId, setOf(edge.toId)) { a, b -> a + b }
            merge(edge.toId, setOf(edge.fromId)) { a, b -> a + b }
        }
    }

    /** How many links a node has. Used to size nodes when the builder gives no weight. */
    val degree: Map<String, Int> = neighbours.mapValues { it.value.size }

    val isEmpty: Boolean get() = nodes.isEmpty()

    companion object {
        val EMPTY = GraphData(emptyList(), emptyList())
    }
}

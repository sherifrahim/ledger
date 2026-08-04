package com.sherif.ledger.core.designsystem.component.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.haptics.LedgerHaptics
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import kotlinx.coroutines.android.awaitFrame
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * An interactive relationship canvas: pan, pinch-zoom, drag nodes, tap to focus.
 *
 * Knows nothing about money — it draws [GraphData] and reports which node was
 * touched. Everything financial lives in whoever built the graph.
 *
 * A note on the interaction model this deliberately does NOT copy from desktop
 * link-analysis tools: Android has no right-click and no mouse wheel. Context
 * menus become long-press and wheel-zoom becomes pinch. Hover tooltips have no
 * equivalent at all, so the information a tooltip would carry is put in the
 * node's own subtitle instead of hidden behind a gesture that cannot happen.
 *
 * The simulation is stepped from the frame clock only while it is still moving.
 * Once [ForceDirectedLayout.isSettled] the loop suspends, so a graph left open on
 * screen costs nothing — a physics canvas that spins forever is the fastest way
 * to drain a battery.
 */
@Composable
fun LedgerGraphCanvas(
    graph: GraphData,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    onNodeTap: (String) -> Unit = {},
    onNodeLongPress: (String) -> Unit = {},
    onBackgroundTap: () -> Unit = {},
) {
    val colors = LedgerTheme.colors
    val haptics = LedgerHaptics.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val layout = remember { ForceDirectedLayout() }
    var frame by remember { mutableIntStateOf(0) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var draggingId by remember { mutableStateOf<String?>(null) }

    // Re-seed when the graph's membership changes, not on every recomposition —
    // sync() preserves existing positions so a refresh does not scatter the layout.
    LaunchedEffect(graph) {
        layout.sync(graph)
        // Frame the whole graph the first time it appears, so the user never opens
        // onto an empty patch of canvas with the content off-screen.
        if (offset == Offset.Zero && canvasSize != Size.Zero) {
            layout.bounds()?.let { bounds ->
                scale = fitScale(bounds, canvasSize)
                offset = Offset(-bounds.centerX * scale, -bounds.centerY * scale)
            }
        }
    }

    // The physics loop. Suspends the moment the graph stops moving.
    LaunchedEffect(graph, draggingId) {
        while (true) {
            if (layout.isSettled && draggingId == null) {
                awaitFrame()
                continue
            }
            layout.step(graph, pinned = draggingId)
            frame++
            awaitFrame()
        }
    }

    val highlighted: Set<String> = remember(selectedId, graph) {
        selectedId?.let { setOf(it) + graph.neighbours[it].orEmpty() } ?: emptySet()
    }

    Box(modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(graph) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hitId = nodeAt(down.position, graph, layout, scale, offset, size.toSize())
                        var totalPan = Offset.Zero
                        var isDrag = false
                        var longPressFired = false
                        val downTime = System.currentTimeMillis()

                        if (hitId != null) draggingId = hitId

                        while (true) {
                            val event = awaitPointerEvent()
                            val changes = event.changes.filter { it.pressed }
                            if (changes.isEmpty()) break

                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            totalPan += pan
                            if (totalPan.getDistance() > TOUCH_SLOP_PX) isDrag = true

                            if (!longPressFired && !isDrag && hitId != null &&
                                System.currentTimeMillis() - downTime > LONG_PRESS_MS
                            ) {
                                longPressFired = true
                                haptics.selection()
                                onNodeLongPress(hitId)
                            }

                            if (changes.size > 1) {
                                // Pinch. Zoom about the gesture centroid so the point
                                // under the fingers stays put, which is what makes a
                                // pinch feel like manipulating the canvas rather than
                                // watching it rescale.
                                val centroid = event.calculateCentroid(useCurrent = true)
                                val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                val canvasCentre = Offset(size.width / 2f, size.height / 2f)
                                val before = (centroid - canvasCentre - offset) / scale
                                offset = centroid - canvasCentre - before * newScale
                                scale = newScale
                                draggingId = null
                            } else if (draggingId != null) {
                                val world = screenToWorld(
                                    changes.first().position, scale, offset, size.toSize(),
                                )
                                layout.place(draggingId!!, world.x, world.y)
                                frame++
                            } else {
                                offset += pan
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }

                        val wasTap = !isDrag && !longPressFired &&
                            System.currentTimeMillis() - downTime < LONG_PRESS_MS
                        if (wasTap) {
                            if (hitId != null) {
                                haptics.selection()
                                onNodeTap(hitId)
                            } else {
                                onBackgroundTap()
                            }
                        }
                        draggingId = null
                    }
                },
        ) {
            canvasSize = size
            frame // read so the canvas redraws each simulation step

            drawGrid(colors.textTertiary.copy(alpha = 0.08f), scale, offset)

            val centre = Offset(size.width / 2f, size.height / 2f)
            fun project(p: NodePosition) = Offset(p.x * scale + centre.x + offset.x, p.y * scale + centre.y + offset.y)

            // Edges first, so nodes sit on top of their own connections.
            graph.edges.forEach { edge ->
                val from = layout.positionOf(edge.fromId) ?: return@forEach
                val to = layout.positionOf(edge.toId) ?: return@forEach
                val dimmed = selectedId != null &&
                    !(highlighted.contains(edge.fromId) && highlighted.contains(edge.toId))
                drawLine(
                    color = colors.textTertiary.copy(alpha = if (dimmed) 0.06f else 0.28f * (0.4f + edge.strength)),
                    start = project(from),
                    end = project(to),
                    strokeWidth = with(density) { (if (dimmed) 0.8f else 1.4f).dp.toPx() },
                )
            }

            graph.nodes.forEach { node ->
                val position = layout.positionOf(node.id) ?: return@forEach
                val point = project(position)
                val radius = with(density) { nodeRadiusDp(node, graph).dp.toPx() } * scale
                // Off-screen nodes are skipped entirely — this is the whole of the
                // "render only what is visible" budget, and at these graph sizes it
                // is enough.
                if (point.x < -radius * 4 || point.x > size.width + radius * 4 ||
                    point.y < -radius * 4 || point.y > size.height + radius * 4
                ) return@forEach

                val faded = selectedId != null && !highlighted.contains(node.id)
                val alpha = if (faded) 0.18f else 1f
                val isSelected = node.id == selectedId

                if (isSelected) {
                    drawCircle(node.color.copy(alpha = 0.22f), radius * 1.7f, point)
                }
                drawCircle(node.color.copy(alpha = alpha), radius, point)
                drawCircle(
                    color = colors.surfaceBase.copy(alpha = alpha * 0.9f),
                    radius = radius,
                    center = point,
                    style = Stroke(width = with(density) { 1.5.dp.toPx() }),
                )

                // Labels only when they can actually be read. Drawing them at every
                // zoom turns a dense graph into a wall of overlapping text.
                if (scale > LABEL_SCALE_THRESHOLD && !faded) {
                    drawNodeLabel(textMeasurer, node, point, radius, colors.textPrimary, colors.textTertiary)
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(color: Color, scale: Float, offset: Offset) {
    val spacing = 42f * scale
    if (spacing < 12f) return // too dense to read; skip rather than draw mush
    val startX = (offset.x % spacing) - spacing
    val startY = (offset.y % spacing) - spacing
    var x = startX
    while (x < size.width + spacing) {
        var y = startY
        while (y < size.height + spacing) {
            drawCircle(color, radius = 1f, center = Offset(x, y))
            y += spacing
        }
        x += spacing
    }
}

private fun DrawScope.drawNodeLabel(
    measurer: TextMeasurer,
    node: GraphNode,
    point: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
) {
    val labelStyle = TextStyle(fontSize = 11.sp, color = primary)
    val measured = measurer.measure(node.label, labelStyle, maxLines = 1)
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(point.x - measured.size.width / 2f, point.y + radius + 6f),
    )
    node.subtitle?.let { subtitle ->
        val subtitleStyle = TextStyle(fontSize = 9.sp, color = secondary)
        val measuredSubtitle = measurer.measure(subtitle, subtitleStyle, maxLines = 1)
        drawText(
            textLayoutResult = measuredSubtitle,
            topLeft = Offset(
                point.x - measuredSubtitle.size.width / 2f,
                point.y + radius + 6f + measured.size.height,
            ),
        )
    }
}

/** Radius grows with the node's own weight, falling back to how connected it is. */
private fun nodeRadiusDp(node: GraphNode, graph: GraphData): Float {
    val degreeWeight = ((graph.degree[node.id] ?: 0).toFloat() / 12f).coerceIn(0f, 1f)
    val weight = maxOf(node.weight, degreeWeight)
    return 9f + weight * 13f
}

private fun screenToWorld(point: Offset, scale: Float, offset: Offset, size: Size): Offset {
    val centre = Offset(size.width / 2f, size.height / 2f)
    return (point - centre - offset) / scale
}

private fun nodeAt(
    point: Offset,
    graph: GraphData,
    layout: ForceDirectedLayout,
    scale: Float,
    offset: Offset,
    size: Size,
): String? {
    val centre = Offset(size.width / 2f, size.height / 2f)
    // Nearest wins, so overlapping nodes in a cluster resolve predictably instead
    // of whichever happens to come first in the list.
    return graph.nodes
        .mapNotNull { node ->
            val position = layout.positionOf(node.id) ?: return@mapNotNull null
            val screen = Offset(position.x * scale + centre.x + offset.x, position.y * scale + centre.y + offset.y)
            val distance = hypot(point.x - screen.x, point.y - screen.y)
            val radius = (nodeRadiusDp(node, graph) * scale).coerceAtLeast(MIN_TOUCH_RADIUS_PX)
            if (distance <= radius) node.id to distance else null
        }
        .minByOrNull { it.second }
        ?.first
}

private fun fitScale(bounds: ForceDirectedLayout.Bounds, size: Size): Float {
    if (size.width <= 0f || size.height <= 0f) return 1f
    val scaleX = size.width / (bounds.width * 1.4f)
    val scaleY = size.height / (bounds.height * 1.4f)
    return minOf(scaleX, scaleY).coerceIn(MIN_SCALE, 1.2f)
}

private fun androidx.compose.ui.unit.IntSize.toSize() = Size(width.toFloat(), height.toFloat())

private const val MIN_SCALE = 0.25f
private const val MAX_SCALE = 3.5f
private const val LABEL_SCALE_THRESHOLD = 0.55f
private const val TOUCH_SLOP_PX = 14f
private const val LONG_PRESS_MS = 450L

/** Fingers are bigger than small nodes; never let a target be untappable. */
private const val MIN_TOUCH_RADIUS_PX = 22f

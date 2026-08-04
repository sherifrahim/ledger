package com.sherif.ledger.feature.storygraph.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerCard
import com.sherif.ledger.core.designsystem.component.LedgerEmptyState
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerSearchBar
import com.sherif.ledger.core.designsystem.component.graph.LedgerGraphCanvas
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.storygraph.StoryGraphPalette
import com.sherif.ledger.feature.storygraph.StoryNodeKind
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.StoryGraphUiState

/**
 * Story Graph — the whole ledger as a set of connected things rather than a list.
 *
 * Every node is an entity that genuinely exists in the database. Entity types
 * Ledger does not yet have are absent rather than drawn empty: a graph of
 * invented nodes would be worse than no graph, and this screen's value rests
 * entirely on the user believing what it shows.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryGraphScreen(
    state: StoryGraphUiState,
    onBackClick: () -> Unit = {},
    onSelect: (String?) -> Unit = {},
    onSearch: (String) -> Unit = {},
    onPaletteReady: (StoryGraphPalette) -> Unit = {},
) {
    val colors = LedgerTheme.colors

    // The palette lives in the theme, which only a composable can read, so it is
    // handed to the ViewModel rather than the ViewModel reaching for it.
    val palette = StoryGraphPalette(
        // Distinct from income below: accent and positive are both green in this
        // theme, which made the two most important kinds indistinguishable.
        account = Color(0xFF5B9BD5),
        merchant = Color(0xFF9B8CF8),
        category = Color(0xFF4FB477),
        tag = Color(0xFFE0A458),
        budget = Color(0xFFB07CC6),
        goal = Color(0xFF3FBFB2),
        income = Color(0xFF43C08A),
    )
    LaunchedEffect(Unit) { onPaletteReady(palette) }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.surfaceBase),
    ) {
        if (!state.isLoading && state.graph.isEmpty) {
            LedgerEmptyState(
                title = "Nothing to connect yet",
                subtitle = "As transactions are captured, this becomes a map of where your " +
                    "money comes from and where it goes — accounts, merchants, categories, " +
                    "and anything you have tagged.",
                icon = Icons.Outlined.Hub,
                modifier = Modifier.align(Alignment.Center).padding(LedgerSpacing.Large),
            )
        } else {
            LedgerGraphCanvas(
                graph = state.graph,
                modifier = Modifier.fillMaxSize(),
                selectedId = state.selectedId,
                onNodeTap = { onSelect(it) },
                onBackgroundTap = { onSelect(null) },
            )
        }

        // Controls float over the canvas rather than shrinking it — the canvas is
        // the point, and a graph in a letterbox is a graph you cannot explore.
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(LedgerSpacing.Medium),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedgerIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBackClick,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                )
                Spacer(Modifier.width(LedgerSpacing.Small))
                Text("Story Graph", style = LedgerTextStyles.Title, color = colors.textPrimary)
            }
            if (!state.graph.isEmpty) {
                Spacer(Modifier.height(LedgerSpacing.Small))
                LedgerSearchBar(
                    query = state.query,
                    onQueryChange = onSearch,
                    placeholder = "Find a merchant, account or tag",
                )
            }
        }

        val selected = state.selectedId?.let { state.graph.nodesById[it] }
        if (selected != null) {
            // A detail card rather than a hover tooltip: Android has no hover, so the
            // information a desktop tool would hide behind one has to live somewhere
            // a finger can reach.
            LedgerCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(LedgerSpacing.Medium),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(selected.color))
                    Spacer(Modifier.width(LedgerSpacing.Small))
                    Column(Modifier.weight(1f)) {
                        Text(
                            selected.label,
                            style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                        Text(
                            listOfNotNull(prettyKind(selected.kind), selected.subtitle).joinToString(" · "),
                            style = LedgerTextStyles.Caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
                val connected = state.graph.neighbours[selected.id].orEmpty()
                if (connected.isNotEmpty()) {
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    Text(
                        "CONNECTED TO ${connected.size}",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.textTertiary,
                    )
                    Spacer(Modifier.height(LedgerSpacing.Tiny))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
                        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
                    ) {
                        connected.take(6).forEach { id ->
                            state.graph.nodesById[id]?.let { neighbour ->
                                Text(
                                    neighbour.label,
                                    style = LedgerTextStyles.Caption,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surfaceInset)
                                        .padding(horizontal = LedgerSpacing.Small, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun prettyKind(kind: String): String =
    runCatching { StoryNodeKind.valueOf(kind) }.getOrNull()
        ?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
        ?: kind

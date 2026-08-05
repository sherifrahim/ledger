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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.storygraph.StoryGraphPalette
import com.sherif.ledger.feature.storygraph.StoryNodeKind
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.FilterAccountUi
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.FilterTagUi
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.StoryGraphDateRange
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.StoryGraphFilterOptions
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.StoryGraphFilters
import com.sherif.ledger.feature.storygraph.presentation.viewmodel.StoryGraphUiState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.signedAmount
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

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
    onOpenTransaction: (Long) -> Unit = {},
    onDateRangeSelected: (StoryGraphDateRange) -> Unit = {},
    onToggleAccountFilter: (Long) -> Unit = {},
    onToggleCategoryFilter: (String) -> Unit = {},
    onToggleTagFilter: (Long) -> Unit = {},
    onToggleKindVisible: (StoryNodeKind) -> Unit = {},
    onClearFilters: () -> Unit = {},
) {
    val colors = LedgerTheme.colors
    var filtersExpanded by remember { mutableStateOf(false) }

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
        if (!state.isLoading && state.graph.isEmpty && state.filters.isActive) {
            // A graph the DATA made empty and a graph the FILTERS made empty are
            // different problems — the first needs more captured activity, the
            // second needs the user to widen what they asked to see.
            LedgerEmptyState(
                title = "No matches for these filters",
                subtitle = "Nothing in your captured activity matches the current filters.",
                icon = Icons.Filled.FilterAlt,
                modifier = Modifier.align(Alignment.Center).padding(LedgerSpacing.Large),
            )
        } else if (!state.isLoading && state.graph.isEmpty) {
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
        Column(Modifier.fillMaxWidth()) {
            LedgerScreenHeader(
                title = "Story Graph",
                onBackClick = onBackClick,
                modifier = Modifier.padding(horizontal = LedgerSpacing.Medium),
                actions = {
                    LedgerIconButton(
                        icon = Icons.Filled.FilterAlt,
                        onClick = { filtersExpanded = !filtersExpanded },
                        contentDescription = "Filters",
                        tint = if (state.filters.isActive) colors.accent else colors.textPrimary,
                    )
                },
            )
            if (!state.graph.isEmpty || state.filters.isActive) {
                Spacer(Modifier.height(LedgerSpacing.Small))
                Box(Modifier.padding(horizontal = LedgerSpacing.Medium)) {
                    LedgerSearchBar(
                        query = state.query,
                        onQueryChange = onSearch,
                        placeholder = "Find a merchant, account or tag",
                    )
                }
            }
            if (filtersExpanded) {
                Spacer(Modifier.height(LedgerSpacing.Small))
                GraphFilterPanel(
                    filters = state.filters,
                    options = state.filterOptions,
                    onDateRangeSelected = onDateRangeSelected,
                    onToggleAccount = onToggleAccountFilter,
                    onToggleCategory = onToggleCategoryFilter,
                    onToggleTag = onToggleTagFilter,
                    onToggleKind = onToggleKindVisible,
                    onClear = onClearFilters,
                    modifier = Modifier.padding(horizontal = LedgerSpacing.Medium),
                )
            }
        }

        val selected = state.selectedId?.let { state.graph.nodesById[it] }
        if (selected != null) {
            // A detail card rather than a hover tooltip: Android has no hover, so what
            // a desktop tool hides behind one has to live where a finger can reach.
            //
            // It lists the entity's real transactions, because a graph that can only
            // tell you something EXISTS is a dead end. The point of selecting
            // Carrefour is to see what you actually spent there — and to get from
            // that to the transaction itself.
            LedgerCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(LedgerSpacing.Medium)
                    .fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(selected.color))
                    Spacer(Modifier.width(LedgerSpacing.Small))
                    Column(Modifier.weight(1f)) {
                        Text(
                            selected.label,
                            style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = 1,
                        )
                        Text(
                            listOfNotNull(prettyKind(selected.kind), selected.subtitle).joinToString(" \u00b7 "),
                            style = LedgerTextStyles.Caption,
                            color = colors.textSecondary,
                            maxLines = 1,
                        )
                    }
                    Text(
                        "Close",
                        style = LedgerTextStyles.Caption,
                        color = colors.textTertiary,
                        modifier = Modifier.ledgerClickable { onSelect(null) },
                    )
                }

                val transactions = state.selectedTransactions
                if (transactions.isNotEmpty()) {
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    LedgerDivider(alpha = 0.06f)
                    Spacer(Modifier.height(LedgerSpacing.Tiny))
                    Text(
                        "TRANSACTIONS",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.textTertiary,
                    )
                    Spacer(Modifier.height(LedgerSpacing.Tiny))
                    // Bounded so the panel never grows past roughly half the canvas —
                    // the graph behind it is still the subject.
                    LazyColumn(Modifier.heightIn(max = 200.dp)) {
                        items(transactions, key = { it.id }) { txn ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .ledgerClickable { onOpenTransaction(txn.id) }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        txn.merchant,
                                        style = LedgerTextStyles.Label,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    Text(txn.date, style = LedgerTextStyles.Caption, color = colors.textTertiary)
                                }
                                Spacer(Modifier.width(LedgerSpacing.Small))
                                LedgerAmount(
                                    amount = signedAmount(txn.amount, isExpense = txn.isOutflow),
                                    style = LedgerAmountStyle.Small,
                                    color = if (txn.isOutflow) colors.textPrimary else colors.positive,
                                )
                            }
                        }
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
                                    // Tapping a neighbour walks the graph, rather than
                                    // making the user hunt for it on the canvas again.
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(colors.surfaceInset)
                                        .ledgerClickable { onSelect(id) }
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

/**
 * Narrows what feeds the graph — a real subset of the same captured data, never
 * a second dataset. Date range and account/category/tag narrow the underlying
 * transactions before the graph is built; entity-type chips narrow which node
 * KINDS are drawn from what's left, so a user overwhelmed by categories can
 * hide them without losing the accounts and merchants underneath.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GraphFilterPanel(
    filters: StoryGraphFilters,
    options: StoryGraphFilterOptions,
    onDateRangeSelected: (StoryGraphDateRange) -> Unit,
    onToggleAccount: (Long) -> Unit,
    onToggleCategory: (String) -> Unit,
    onToggleTag: (Long) -> Unit,
    onToggleKind: (StoryNodeKind) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerTheme.colors
    LedgerCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("FILTERS", style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold), color = colors.textTertiary)
                if (filters.isActive) {
                    Text(
                        "Clear",
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = colors.accent,
                        modifier = Modifier.ledgerClickable(onClick = onClear),
                    )
                }
            }

            FilterSection("Date range") {
                StoryGraphDateRange.entries.forEach { range ->
                    GraphFilterChip(range.label, selected = filters.dateRange == range, onClick = { onDateRangeSelected(range) })
                }
            }

            FilterSection("Show") {
                StoryNodeKind.entries.forEach { kind ->
                    GraphFilterChip(
                        prettyKind(kind.name),
                        selected = kind !in filters.hiddenKinds,
                        onClick = { onToggleKind(kind) },
                    )
                }
            }

            if (options.accounts.isNotEmpty()) {
                FilterSection("Accounts") {
                    options.accounts.forEach { account ->
                        GraphFilterChip(account.name, selected = account.id in filters.accountIds, onClick = { onToggleAccount(account.id) })
                    }
                }
            }

            if (options.categories.isNotEmpty()) {
                FilterSection("Categories") {
                    options.categories.forEach { category ->
                        GraphFilterChip(
                            category.lowercase().replaceFirstChar { it.uppercase() },
                            selected = category in filters.categories,
                            onClick = { onToggleCategory(category) },
                        )
                    }
                }
            }

            if (options.tags.isNotEmpty()) {
                FilterSection("Tags") {
                    options.tags.forEach { tag ->
                        GraphFilterChip(tag.name, selected = tag.id in filters.tagIds, onClick = { onToggleTag(tag.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(LedgerSpacing.Small))
    Text(title, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
    Spacer(Modifier.height(LedgerSpacing.Tiny))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Tiny),
        content = { content() },
    )
}

@Composable
private fun GraphFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LedgerTheme.colors
    val bg = if (selected) colors.textPrimary else colors.surfaceInset
    val fg = if (selected) colors.surfaceBase else colors.textSecondary
    Box(
        modifier = Modifier
            .clip(LedgerRadius.Full)
            .background(bg)
            .ledgerClickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Small, vertical = 6.dp),
    ) {
        Text(label, style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
    }
}

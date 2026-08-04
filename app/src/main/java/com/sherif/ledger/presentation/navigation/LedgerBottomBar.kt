package com.sherif.ledger.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.LocalNavHazeState
import com.sherif.ledger.core.designsystem.theme.ledgerGlassSurface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.sherif.ledger.core.designsystem.theme.LedgerMotion

// The five primary destinations, per spec Chapter 34: Dashboard, Story, Review,
// Search, Settings. Secondary experiences (Accounts, Transactions/Activity,
// Insights, Merchant, Institution, Forecast, …) are reached from these, not from
// the bottom bar. The Settings destination is served by the existing Profile hub.
private enum class BottomTab(
    val route: String,
    val label: String,
    val iconOutlined: ImageVector,
    val iconFilled: ImageVector,
) {
    // Glyphs follow the visual reference: a house for Dashboard, an open book for
    // Story, a review/inbox tray, a magnifier, and a gear. Each tab is outlined
    // when idle and fills in when selected — the reference's selected treatment.
    Dashboard(LedgerRoute.Home.route, "Dashboard", Icons.Outlined.Home, Icons.Filled.Home),
    Story(LedgerRoute.Story.route, "Story", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    Review(LedgerRoute.ReviewInbox.route, "Review", Icons.Outlined.Inbox, Icons.Filled.Inbox),
    Search(LedgerRoute.Search.route, "Search", Icons.Outlined.Search, Icons.Filled.Search),
    Settings(LedgerRoute.Profile.route, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

/**
 * Ledger navigation — a **floating island** in the design language.
 *
 * A single rounded surface that floats above the bottom edge (side + bottom margins,
 * clear of the system gesture bar) rather than a full-width footer. It follows the
 * card philosophy: paper-white lifted by a soft shadow in light, one-step-lighter
 * charcoal with a whisper border in dark — the same treatment as [LedgerCard], so it
 * reads correctly in both themes. Content scrolls underneath it.
 */
@Composable
fun LedgerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val colors = LedgerTheme.colors
    val navHaze = LocalNavHazeState.current
    // Glass only when enabled AND the content layer exists to blur beneath it.
    val glass = LedgerTheme.glass && navHaze != null
    val shape = LedgerRadius.Full

    // Same surface language as LedgerCard: real backdrop-blur glass (blurring the
    // scrolling content passing beneath the island) when enabled, otherwise the
    // solid island fill.
    val surfaceMod = if (glass) {
        Modifier.ledgerGlassSurface(navHaze!!, shape, colors.isDark, colors.surfaceCard)
    } else {
        Modifier.clip(shape).background(colors.surfaceCard).border(LedgerTheme.border.Hairline, colors.cardBorder, shape)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = LedgerSpacing.Medium, vertical = LedgerSpacing.Tiny)
            .then(
                if (!glass && !colors.isDark) Modifier.shadow(
                    elevation = 16.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = colors.shadowColor,
                    spotColor = colors.shadowColor,
                ) else Modifier,
            )
            .then(surfaceMod)
            .padding(horizontal = LedgerSpacing.Tiny, vertical = LedgerSpacing.Tiny),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomTab.entries.forEach { tab ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            LedgerTabItem(
                modifier = Modifier.weight(1f),
                icon = if (isSelected) tab.iconFilled else tab.iconOutlined,
                label = tab.label,
                isSelected = isSelected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}

@Composable
private fun LedgerTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerTheme.colors
    val color by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textTertiary,
        animationSpec = tween(LedgerMotion.Short),
        label = "tabColor",
    )

    // The glyph swaps outlined→filled on selection, which is a hard cut. Springing
    // the scale through that swap is what turns it from "the icon changed" into
    // "the tab was chosen": a small, quick overshoot that lands with the colour.
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) LedgerMotion.SelectedIconScale else 1f,
        animationSpec = LedgerMotion.selectionSpring(),
        label = "tabIconScale",
    )

    Column(
        modifier = modifier
            .ledgerClickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
        )
        Text(
            text = label,
            style = LedgerTextStyles.Caption.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.SemiBold
                             else androidx.compose.ui.text.font.FontWeight.Normal,
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        )
    }
}

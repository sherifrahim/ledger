#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Premium Experience Pass..."

# ==== PHASE 1: LDL PURGE ====

# ---- 1. Custom LDL Bottom Bar (replaces Material NavigationBar) ----
cat > "$B/presentation/navigation/LedgerBottomBar.kt" << 'EOF'
package com.sherif.ledger.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

private enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(LedgerRoute.Home.route, "Home", Icons.Filled.Home),
    Accounts(LedgerRoute.Accounts.route, "Accounts", Icons.Filled.AccountCircle),
    Activity(LedgerRoute.Transactions.route, "Activity", Icons.Filled.List),
    Review(LedgerRoute.ReviewInbox.route, "Review", Icons.Filled.CheckCircle),
}

/**
 * LDL bottom navigation. No Material NavigationBar, NavigationBarItem,
 * or indicator. Pure Compose Row with spring-animated tab items and
 * no ripple. The only Material import remaining is Icon and Text.
 */
@Composable
fun LedgerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LedgerTheme.colors.surfaceLevel1)
            .navigationBarsPadding()
            .padding(top = LedgerSpacing.Content, bottom = LedgerSpacing.Content),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            LedgerTabItem(
                icon = tab.icon,
                label = tab.label,
                selected = selected,
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
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = LedgerAnimations.microSpring(),
        label = "${label}_scale",
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.45f,
        animationSpec = LedgerAnimations.microSpring(),
        label = "${label}_iconAlpha",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = LedgerAnimations.microSpring(),
        label = "${label}_labelAlpha",
    )

    val color = if (selected) LedgerTheme.colors.tint else LedgerTheme.colors.tertiaryLabel

    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
            .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Inline),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .scale(iconScale)
                .graphicsLayer { alpha = iconAlpha },
        )
        if (labelAlpha > 0.01f) {
            Text(
                label,
                style = LedgerTextStyles.Caption,
                color = color,
                modifier = Modifier
                    .padding(top = LedgerSpacing.XxSmall)
                    .graphicsLayer { alpha = labelAlpha },
            )
        }
    }
}
EOF

# ---- 2. Custom LDL Search Bar (replaces Material OutlinedTextField) ----
cat > "$B/core/designsystem/component/LedgerSearchBar.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL search field. No Material OutlinedTextField, no Material border
 * animation, no Material label. Uses BasicTextField with a custom
 * decoration box for full visual control.
 */
@Composable
fun LedgerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        textStyle = LedgerTextStyles.Body.copy(color = LedgerTheme.colors.label),
        cursorBrush = SolidColor(LedgerTheme.colors.tint),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LedgerRadius.Medium)
                    .background(LedgerTheme.colors.surfaceLevel1)
                    .padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = LedgerTheme.colors.tertiaryLabel,
                    modifier = Modifier.size(20.dp),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = LedgerSpacing.Content),
                ) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = LedgerTextStyles.Body,
                            color = LedgerTheme.colors.tertiaryLabel,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = LedgerTheme.colors.tertiaryLabel,
                        modifier = Modifier
                            .size(20.dp)
                            .noRippleClickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}

/** Clickable without Material ripple indication. */
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = androidx.compose.runtime.remember {
                androidx.compose.foundation.interaction.MutableInteractionSource()
            },
            onClick = onClick,
        )
    )
EOF

# ---- 3. LedgerAnimations update with Phase 2 motion choreography ----
cat > "$B/core/designsystem/theme/LedgerAnimations.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally

/**
 * Canonical animation vocabulary for Ledger.
 *
 * Every animation references these presets. Tuning motion is a
 * single-file change. No inline animation specs anywhere in the app.
 */
object LedgerAnimations {

    // ── Springs ──

    /** Tight spring for icon scale, chip selection, micro-interactions. */
    fun <T> microSpring() = spring<T>(dampingRatio = 0.7f, stiffness = 600f)

    /** Standard spring for cards, expansion, surface transitions. */
    fun <T> standardSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Calm spring for hero collapse, screen settle. */
    fun <T> calmSpring() = spring<T>(
        dampingRatio = LedgerMotion.HeroSpringDamping,
        stiffness = LedgerMotion.HeroSpringStiffness,
    )

    // ── List choreography ──

    /** Standard list item entrance: fade + slight upward slide. */
    fun listEnter(delayMs: Int = 0): EnterTransition =
        fadeIn(tween(LedgerMotion.StandardTweenMs, delayMillis = delayMs)) +
            slideInVertically(tween(LedgerMotion.StandardTweenMs, delayMillis = delayMs)) { it / 8 }

    /** Standard content exit. */
    fun contentExit(): ExitTransition =
        fadeOut(tween(LedgerMotion.FastTweenMs))

    /** Stagger delay for item at [index] in a list. */
    fun staggerDelay(index: Int, baseMs: Int = LedgerMotion.StaggerBaseMs): Int =
        index * baseMs

    // ── Navigation transitions ──

    val screenEnter: EnterTransition =
        slideInHorizontally(tween(LedgerMotion.StandardTweenMs)) { it / 5 } +
            fadeIn(tween(LedgerMotion.StandardTweenMs))

    val screenExit: ExitTransition =
        slideOutHorizontally(tween(LedgerMotion.StandardTweenMs)) { -it / 5 } +
            fadeOut(tween(LedgerMotion.FastTweenMs))

    val screenPopEnter: EnterTransition =
        slideInHorizontally(tween(LedgerMotion.StandardTweenMs)) { -it / 5 } +
            fadeIn(tween(LedgerMotion.StandardTweenMs))

    val screenPopExit: ExitTransition =
        slideOutHorizontally(tween(LedgerMotion.StandardTweenMs)) { it / 5 } +
            fadeOut(tween(LedgerMotion.FastTweenMs))
}
EOF

# ---- 4. NavHost uses centralized transitions ----
cat > "$B/presentation/navigation/LedgerNavHost.kt" << 'EOF'
package com.sherif.ledger.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.review.presentation.ReviewInboxScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.presentation.dashboard.DashboardScreen

@Composable
fun LedgerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = LedgerRoute.Home.route,
        modifier = modifier,
        enterTransition = { LedgerAnimations.screenEnter },
        exitTransition = { LedgerAnimations.screenExit },
        popEnterTransition = { LedgerAnimations.screenPopEnter },
        popExitTransition = { LedgerAnimations.screenPopExit },
    ) {
        composable(LedgerRoute.Home.route) {
            DashboardScreen(
                onNavigateToTransactions = {
                    navController.navigate(LedgerRoute.Transactions.route) { launchSingleTop = true }
                },
            )
        }

        composable(LedgerRoute.Accounts.route) {
            AccountsScreen()
        }

        composable(LedgerRoute.Transactions.route) {
            TransactionsScreen(
                onTransactionClick = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id))
                },
            )
        }

        composable(LedgerRoute.ReviewInbox.route) {
            ReviewInboxScreen(
                onReviewItemClick = { id ->
                    navController.navigate(LedgerRoute.TransactionDetails.create(id))
                },
            )
        }

        composable(
            route = LedgerRoute.TransactionDetails.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType }),
        ) {
            TransactionDetailsScreen()
        }
    }
}
EOF

echo "Done. Premium Experience Pass complete."
echo "Phase 1: Material NavigationBar → custom LDL Row. Material OutlinedTextField → BasicTextField."
echo "Phase 2: Navigation transitions centralized in LedgerAnimations."
echo ""
echo "Run: git add -A && git commit -m 'refactor(ldl): premium experience pass — purge Material navigation and search'"
echo "Then: ./gradlew assembleDebug"

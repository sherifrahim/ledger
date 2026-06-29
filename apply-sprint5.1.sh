#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 5.1 — Premium Experience Layer..."

# ---- 1. LedgerAnimations.kt — reusable motion vocabulary ----
cat > "$B/core/designsystem/theme/LedgerAnimations.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically

/**
 * Canonical animation vocabulary for Ledger.
 *
 * Every animation in the app should reference these presets rather than
 * constructing inline specs. This ensures motion feels consistent across
 * screens and makes tuning a single-file change.
 */
object LedgerAnimations {

    /** Gentle spring for icon scale, chip selection, micro-interactions. */
    fun <T> microSpring() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = 600f,
    )

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

    /** Standard list item entrance: fade + slight upward slide. */
    fun listEnter(delayMs: Int = 0): EnterTransition =
        fadeIn(tween(LedgerMotion.StandardTweenMs, delayMillis = delayMs)) +
            slideInVertically(tween(LedgerMotion.StandardTweenMs, delayMillis = delayMs)) { it / 8 }

    /** Standard content exit. */
    fun contentExit(): ExitTransition =
        fadeOut(tween(LedgerMotion.FastTweenMs))

    /** Stagger delay for item at [index] in a list. */
fun staggerDelay(
    index: Int,
    baseMs: Int = LedgerMotion.StaggerBaseMs,
): Int = index * baseMs
}
EOF

# ---- 2. Premium Bottom Bar with animated selection ----
cat > "$B/presentation/navigation/LedgerBottomBar.kt" << 'EOF'
package com.sherif.ledger.presentation.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
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

@Composable
fun LedgerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(
        containerColor = LedgerTheme.colors.surfaceLevel1,
        contentColor = LedgerTheme.colors.label,
    ) {
        BottomTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true

            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.12f else 1.0f,
                animationSpec = LedgerAnimations.microSpring(),
                label = "${tab.label}_scale",
            )
            val labelAlpha by animateFloatAsState(
                targetValue = if (selected) 1.0f else 0.0f,
                animationSpec = LedgerAnimations.microSpring(),
                label = "${tab.label}_label",
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp).scale(iconScale),
                    )
                },
                label = {
                    Text(
                        tab.label,
                        style = LedgerTextStyles.Caption,
                        modifier = Modifier.graphicsLayer { alpha = labelAlpha },
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LedgerTheme.colors.tint,
                    selectedTextColor = LedgerTheme.colors.tint,
                    unselectedIconColor = LedgerTheme.colors.tertiaryLabel,
                    unselectedTextColor = LedgerTheme.colors.tertiaryLabel,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
EOF

# ---- 3. Update LedgerMotion with missing tween constants ----
cat > "$B/core/designsystem/theme/LedgerMotion.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * LDL motion vocabulary.
 *
 * Every animation in Ledger should reference these instead of local constants.
 * Springs produce natural settling for user-initiated motion, while tweens
 * provide predictable timing for system-initiated events.
 *
 * This file is the single source of truth for motion across Ledger.
 */
object LedgerMotion {

    // -----------------------------------------------------------------------
    // Duration presets (milliseconds)
    // -----------------------------------------------------------------------

    const val Fast = 120
    const val Medium = 220
    const val Slow = 360

    const val FastTweenMs = 150
    const val StandardTweenMs = 250
    const val SlowTweenMs = 400

    /**
     * Base delay between sequential list items.
     *
     * Used for staggered entrance animations throughout the app.
     */
    const val StaggerBaseMs = 40

    // -----------------------------------------------------------------------
    // Spring presets
    // -----------------------------------------------------------------------

    const val HeroSpringDamping = 0.88f
    const val HeroSpringStiffness = 300f

    const val CardSpringDamping = 0.85f
    const val CardSpringStiffness = 400f

    const val ScreenSpringDamping = 0.92f
    const val ScreenSpringStiffness = 200f

    /**
     * Hero transitions.
     *
     * Large surfaces, collapsing headers, balance hero.
     */
    fun heroSpring(): SpringSpec<Float> = spring(
        dampingRatio = HeroSpringDamping,
        stiffness = HeroSpringStiffness,
    )

    /**
     * Card expansion / collapse.
     */
    fun cardSpring(): SpringSpec<Float> = spring(
        dampingRatio = CardSpringDamping,
        stiffness = CardSpringStiffness,
    )

    /**
     * Screen-level transitions.
     */
    fun screenSpring(): SpringSpec<Float> = spring(
        dampingRatio = ScreenSpringDamping,
        stiffness = ScreenSpringStiffness,
    )
}
EOF

# ---- 4. Architectural cleanup ----

# Remove presentation/theme wrapper if it still exists (orphan from ChatGPT)
rm -f "$B/presentation/theme/Theme.kt"
rm -f "$B/presentation/theme/Color.kt"
rm -f "$B/presentation/theme/Type.kt"
rmdir "$B/presentation/theme" 2>/dev/null || true

# Remove legacy hero card if still present
rm -f "$B/core/designsystem/component/hero/LedgerHeroCard.kt"

# Remove any remaining empty .gitkeep files
find "$B/feature" -name ".gitkeep" -delete 2>/dev/null || true

echo "Done. Sprint 5.1 complete."
echo "Run: git add -A && git commit -m 'feat(motion): sprint 5.1 animation vocabulary, premium bottom bar, cleanup'"
echo "Then: ./gradlew assembleDebug"

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

package com.sherif.ledger.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

private enum class BottomTab(val route: String, val label: String, val icon: ImageVector) {
    Home(LedgerRoute.Home.route, "Home", Icons.Rounded.Home),
    Accounts(LedgerRoute.Accounts.route, "Accounts", Icons.Rounded.AccountBalance),
    Activity(LedgerRoute.Transactions.route, "Activity", Icons.Rounded.ReceiptLong),
    Review(LedgerRoute.ReviewInbox.route, "Review", Icons.Rounded.FactCheck),
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
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = LedgerTextStyles.Caption) },
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

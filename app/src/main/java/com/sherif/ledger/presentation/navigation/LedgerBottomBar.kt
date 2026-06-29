package com.sherif.ledger.presentation.navigation

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.component.ledgerClickable
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
    val color = if (selected) LedgerTheme.colors.tint else LedgerTheme.colors.tertiaryLabel
    val alpha = if (selected) 1.0f else 0.45f

    Column(
        modifier = Modifier
            .ledgerClickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Inline),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier
                .size(LedgerTheme.iconSize.Medium)
                .graphicsLayer { this.alpha = alpha },
        )
        if (selected) {
            Text(
                label,
                style = LedgerTextStyles.Caption,
                color = color,
                modifier = Modifier.padding(top = LedgerSpacing.XxSmall),
            )
        }
    }
}

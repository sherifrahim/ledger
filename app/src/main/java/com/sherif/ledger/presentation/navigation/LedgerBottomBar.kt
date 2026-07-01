package com.sherif.ledger.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

private enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(LedgerRoute.Home.route, "Home", Icons.Filled.Home),
    Accounts(LedgerRoute.Accounts.route, "Accounts", Icons.Filled.AccountBalanceWallet),
    Activity(LedgerRoute.Transactions.route, "Activity", Icons.AutoMirrored.Filled.List),
    Profile(LedgerRoute.Profile.route, "Profile", Icons.Filled.Person),
}

/**
 * LDL bottom navigation.
 *
 * Designed as a floating "Dock" instead of a standard Android bottom bar.
 * This moves Ledger's DNA away from the OS defaults and towards a high-end
 * productivity tool aesthetic (Arc, Craft, Things 3).
 */
@Composable
fun LedgerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = LedgerSpacing.Medium, end = LedgerSpacing.Medium, bottom = LedgerSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .ledgerSurface(
                    level = LedgerSurfaceLevel.Level1,
                    shape = LedgerRadius.Full,
                    borderColor = Color.White.copy(alpha = 0.08f) // Refined depth
                )
                .padding(horizontal = LedgerSpacing.Small, vertical = 8.dp),
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
}

@Composable
private fun LedgerTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) LedgerTheme.colors.tint else LedgerTheme.colors.tertiaryLabel
    val contentAlpha = if (selected) 1.0f else 0.35f // Heavier calm contrast

    Column(
        modifier = Modifier
            .ledgerClickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Small, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier
                .size(LedgerTheme.iconSize.Navigation)
                .graphicsLayer { alpha = contentAlpha },
        )
        if (selected) {
            Text(
                text = label,
                style = LedgerTextStyles.Caption,
                color = tint,
                modifier = Modifier.padding(top = LedgerSpacing.XxSmall),
            )
        }
    }
}

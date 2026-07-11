package com.sherif.ledger.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

private enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(LedgerRoute.Home.route, "Home", Icons.Filled.Home),
    Accounts(LedgerRoute.Accounts.route, "Accounts", Icons.Filled.AccountBalanceWallet),
    Activity(LedgerRoute.Transactions.route, "Activity", Icons.AutoMirrored.Filled.List),
    Insights(LedgerRoute.Insights.route, "Insights", Icons.Filled.PieChart),
    Profile(LedgerRoute.Profile.route, "Profile", Icons.Filled.Person),
}

/**
 * Ledger V3 Navigation Bar
 * 
 * Replaces the V2 floating dock with a clean, editorial architectural footer.
 */
@Composable
fun LedgerBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Column {
        LedgerDivider(alpha = 0.05f) // Architectural separation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LedgerTheme.colors.surfaceBase.copy(alpha = 0.95f))
                .navigationBarsPadding()
                .padding(vertical = LedgerSpacing.Small),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTab.entries.forEach { tab ->
                val isSelected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                LedgerTabItem(
                    icon = tab.icon,
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
}

@Composable
private fun LedgerTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LedgerTheme.colors
    val color = if (isSelected) colors.textPrimary else colors.textTertiary

    Column(
        modifier = Modifier
            .ledgerClickable(onClick = onClick)
            .padding(horizontal = LedgerSpacing.Small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = color
        )
    }
}

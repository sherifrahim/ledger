package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.Large, bottom = LedgerSpacing.ScreenBottom + 100.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item("header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(vertical = LedgerSpacing.Medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    style = LedgerTextStyles.Headline,
                    color = LedgerTheme.colors.label,
                )
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = LedgerTheme.colors.label,
                    modifier = Modifier
                        .size(LedgerTheme.iconSize.Medium)
                        .ledgerClickable { onNavigateToSettings() }
                )
            }
        }

        item("user_card") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = LedgerSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Monolith
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .ledgerSurface(
                            level = LedgerSurfaceLevel.Level1, 
                            shape = LedgerRadius.Full,
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SR",
                        style = LedgerTextStyles.Section,
                        color = LedgerTheme.colors.label
                    )
                }
                
                Spacer(Modifier.width(LedgerSpacing.Medium))

                Column {
                    Text(
                        text = "Sherif Rahim",
                        style = LedgerTextStyles.Section,
                        color = LedgerTheme.colors.label
                    )
                    Text(
                        text = "sherif.rahim@email.com",
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.tertiaryLabel
                    )
                }
            }
        }

        item("actions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .ledgerSurface(level = LedgerSurfaceLevel.Level1)
            ) {
                ProfileRow(icon = Icons.Filled.Person, label = "Personal information")
                LedgerHairline(Modifier.padding(start = 56.dp))
                ProfileRow(icon = Icons.Filled.Security, label = "Security")
                LedgerHairline(Modifier.padding(start = 56.dp))
                ProfileRow(icon = Icons.Filled.CreditCard, label = "Payment methods")
                LedgerHairline(Modifier.padding(start = 56.dp))
                ProfileRow(icon = Icons.Filled.Notifications, label = "Notifications")
                LedgerHairline(Modifier.padding(start = 56.dp))
                ProfileRow(icon = Icons.Filled.HelpOutline, label = "Help & support")
                LedgerHairline(Modifier.padding(start = 56.dp))
                ProfileRow(icon = Icons.Filled.Info, label = "About Ledger")
            }
        }

        item("logout") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .ledgerSurface(level = LedgerSurfaceLevel.Level1)
                    .ledgerClickable { /* TODO */ }
                    .padding(LedgerSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    tint = LedgerTheme.colors.expense,
                    modifier = Modifier.size(LedgerTheme.iconSize.Small)
                )
                Spacer(Modifier.width(LedgerSpacing.Medium))
                Text(
                    text = "Log out",
                    style = LedgerTextStyles.Label,
                    color = LedgerTheme.colors.expense
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerClickable { /* TODO */ }
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LedgerTheme.colors.secondaryLabel,
                modifier = Modifier.size(LedgerTheme.iconSize.Small)
            )
            Spacer(Modifier.width(LedgerSpacing.Medium))
            Text(label, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = LedgerTheme.colors.tertiaryLabel,
            modifier = Modifier.size(LedgerTheme.iconSize.Small)
        )
    }
}

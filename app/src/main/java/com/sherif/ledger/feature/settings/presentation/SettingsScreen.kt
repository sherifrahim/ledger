package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerTopBar
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.Large, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item("nav") {
            LedgerTopBar(
                title = "Settings",
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LedgerTheme.colors.label,
                        modifier = Modifier
                            .size(LedgerTheme.iconSize.Medium)
                            .ledgerClickable { onBackClick() }
                    )
                }
            )
        }

        item("appearance") {
            SettingsGroup(title = "Appearance") {
                SettingsRow(label = "Dark mode", trailing = {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LedgerTheme.colors.success
                        )
                    )
                })
                LedgerHairline()
                SettingsRow(label = "Currency", value = "AED")
                LedgerHairline()
                SettingsRow(label = "Language", value = "English")
            }
        }

        item("preferences") {
            SettingsGroup(title = "Preferences") {
                SettingsRow(label = "Default account", value = "Personal Account")
                LedgerHairline()
                SettingsRow(label = "Expense reminders", value = "On")
                LedgerHairline()
                SettingsRow(label = "Weekly insights", value = "On")
            }
        }

        item("data") {
            SettingsGroup(title = "Data & Privacy") {
                SettingsRow(label = "Export data")
                LedgerHairline()
                SettingsRow(label = "Delete account", labelColor = LedgerTheme.colors.expense)
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Group)) {
        LedgerSectionHeader(title = title.uppercase())
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .ledgerSurface(level = LedgerSurfaceLevel.Level1)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    labelColor: Color = LedgerTheme.colors.label,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerClickable { /* TODO */ }
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = LedgerTextStyles.Label, color = labelColor)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, style = LedgerTextStyles.Label, color = LedgerTheme.colors.secondaryLabel)
                Spacer(Modifier.width(LedgerSpacing.Small))
            }
            
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = LedgerTheme.colors.tertiaryLabel,
                    modifier = Modifier.size(LedgerTheme.iconSize.Small)
                )
            }
        }
    }
}

package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerTopBar
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.LedgerThemeType
import com.sherif.ledger.feature.settings.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeType by viewModel.themeType.collectAsState()

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
                ThemeSelectionRow(
                    selectedTheme = themeType,
                    onThemeSelected = { viewModel.setThemeType(it) }
                )
                LedgerHairline()
                SettingsRow(label = "Dark mode", trailing = {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LedgerTheme.colors.onTint,
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
private fun ThemeSelectionRow(
    selectedTheme: LedgerThemeType,
    onThemeSelected: (LedgerThemeType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LedgerSpacing.Medium)
    ) {
        Text(
            text = "Theme",
            style = LedgerTextStyles.Label,
            color = LedgerTheme.colors.label
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
        ) {
            LedgerThemeType.entries.forEach { theme ->
                val isSelected = theme == selectedTheme
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .ledgerSurface(
                            level = if (isSelected) LedgerSurfaceLevel.Level2 else LedgerSurfaceLevel.Level1,
                            backgroundColor = if (isSelected) LedgerTheme.colors.tint else LedgerTheme.colors.surfaceLevel1,
                            borderColor = if (isSelected) Color.Transparent else LedgerTheme.colors.separator.copy(alpha = 0.1f),
                            onClick = { onThemeSelected(theme) }
                        )
                        .padding(vertical = LedgerSpacing.Small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = theme.name,
                        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) LedgerTheme.colors.onTint else LedgerTheme.colors.secondaryLabel
                    )
                }
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

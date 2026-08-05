package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.LedgerThemeType
import com.sherif.ledger.feature.settings.presentation.viewmodel.SettingsViewModel
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeType by viewModel.themeType.collectAsState()
    val liquidGlass by viewModel.liquidGlass.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.Large, bottom = ledgerScreenBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item("nav") {
            LedgerScreenHeader(title = "Appearance", onBackClick = onBackClick)
        }

        // Only genuinely-functional settings remain. The previous Dark-mode toggle
        // (hard-coded on, no-op) and the Currency / Language / Default account /
        // Expense reminders / Weekly insights / Export data / Delete account rows were
        // fabricated (static values, TODO no-op clicks) and were removed — a settings
        // row must do what it says. Theme is the one wired control.
        item("appearance") {
            SettingsGroup(title = "Appearance") {
                ThemeSelectionRow(
                    selectedTheme = themeType,
                    onThemeSelected = { viewModel.setThemeType(it) }
                )
                LedgerHairline()
                LiquidGlassRow(
                    enabled = liquidGlass,
                    onToggle = { viewModel.setLiquidGlass(it) },
                )
            }
        }
    }
}

/**
 * Liquid Glass — on by default. When on, content cards and the nav island
 * become translucent frosted glass over the base light/dark theme. A single
 * honest switch: what it says is what it does.
 */
@Composable
private fun LiquidGlassRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerClickable { onToggle(!enabled) }
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Liquid Glass",
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Translucent, frosted cards and navigation",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.secondaryLabel,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
        )
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
            // Explicit Light / Dark — the user picks, no "follow system". Liquid
            // Glass is the separate switch below.
            val options = listOf(
                LedgerThemeType.Light to "Light",
                LedgerThemeType.Dark to "Dark",
            )
            options.forEach { (theme, label) ->
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
                        text = label,
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


package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.settings.presentation.viewmodel.UserProfileViewModel

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDebugConsole: () -> Unit = {},
    onNavigateToAdjustBalance: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToReviewInbox: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            ProfileTopBar(onNavigateToSettings)
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = LedgerSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.SectionGap)
        ) {
            item {
                UserProfileHeader(onEditClick = onNavigateToEditProfile)
            }

            item {
                PreferencesSection(onNavigateToDebugConsole, onNavigateToAdjustBalance, onNavigateToReviewInbox, onNavigateToAiSettings)
            }

            item {
                TextButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log Out", color = LedgerTheme.colors.negative, fontWeight = FontWeight.Bold)
                }
            }
            
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun ProfileTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = LedgerSpacing.ScreenPadding, vertical = LedgerSpacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(44.dp))

        Text(
            text = "Profile",
            style = LedgerTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = LedgerTheme.colors.textPrimary
        )

        LedgerIconButton(
            icon = Icons.Default.Settings,
            onClick = onSettingsClick,
            tint = LedgerTheme.colors.textPrimary
        )
    }
}

@Composable
private fun UserProfileHeader(onEditClick: () -> Unit, viewModel: UserProfileViewModel = hiltViewModel()) {
    val profile by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Profile Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(LedgerTheme.colors.surfaceInset),
            contentAlignment = Alignment.Center
        ) {
            Text(profile.initials, style = LedgerTheme.typography.headlineLarge, color = LedgerTheme.colors.textPrimary)
        }

        Spacer(Modifier.height(LedgerSpacing.Medium))

        Text(profile.name, style = LedgerTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(profile.email, style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textSecondary)

        Spacer(Modifier.height(LedgerSpacing.Small))

        TextButton(onClick = onEditClick) {
            Text("Edit Profile", color = LedgerTheme.colors.system, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PreferencesSection(onDebugConsoleClick: () -> Unit, onAdjustBalanceClick: () -> Unit, onReviewInboxClick: () -> Unit, onAiSettingsClick: () -> Unit) {
    Column {
        Text("PREFERENCES", style = LedgerTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(LedgerSpacing.Small))

        LedgerSurface(
            level = com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel.Inset,
            shape = com.sherif.ledger.core.designsystem.tokens.LedgerRadius.Large,
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRow(icon = Icons.Default.Payments, label = "Currency", value = "AED")
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.Palette, label = "Theme", value = "System")
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.Language, label = "Language", value = "English")
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.Notifications, label = "Notifications")
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.Security, label = "Data & Privacy")
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.AccountBalanceWallet, label = "Adjust Starting Balance", onClick = onAdjustBalanceClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.RateReview, label = "Review Uncategorized Transactions", onClick = onReviewInboxClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.SmartToy, label = "AI Settings", onClick = onAiSettingsClick)

            if (com.sherif.ledger.BuildConfig.DEBUG) {
                LedgerDivider(alpha = 0.05f)
                PreferenceRow(icon = Icons.Default.BugReport, label = "Developer Console", onClick = onDebugConsoleClick)
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ledgerClickable(onClick = onClick)
            .padding(LedgerSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = LedgerTheme.colors.textPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(LedgerSpacing.Medium))
        Text(label, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textPrimary, modifier = Modifier.weight(1f))
        
        if (value != null) {
            Text(value, style = LedgerTheme.typography.bodySmall, color = LedgerTheme.colors.textSecondary)
            Spacer(Modifier.width(8.dp))
        }
        
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LedgerTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
    }
}

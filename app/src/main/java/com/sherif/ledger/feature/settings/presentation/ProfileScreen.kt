package com.sherif.ledger.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDebugConsole: () -> Unit = {},
    onNavigateToAdjustBalance: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToReviewInbox: () -> Unit = {},
    onNavigateToAiSettings: () -> Unit, onNavigateToBudgets: () -> Unit, onNavigateToGoals: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            LedgerScreenHeader(
                title = "Settings",
                modifier = Modifier.padding(horizontal = LedgerSpacing.ScreenPadding),
                actions = {
                    LedgerIconButton(
                        icon = Icons.Default.Settings,
                        onClick = onNavigateToSettings,
                        contentDescription = "Settings",
                        tint = LedgerTheme.colors.textPrimary,
                    )
                },
            )
        },
        containerColor = LedgerTheme.colors.surfaceBase
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding,
                end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.SectionGap)
        ) {
            item {
                UserProfileHeader(onEditClick = onNavigateToEditProfile)
            }

            // Overview: secondary financial destinations, kept reachable here after
            // the bottom bar was reduced to the five primary destinations. Their
            // proper home is the Dashboard; this section is an interim access point.
            item {
                OverviewSection(onNavigateToAccounts, onNavigateToActivity, onNavigateToInsights)
            }

            item {
                CaptureCoverageCard()
            }

            item {
                PreferencesSection(onNavigateToDebugConsole, onNavigateToAdjustBalance, onNavigateToReviewInbox, onNavigateToAiSettings, onNavigateToBudgets, onNavigateToGoals)
            }

            item {
                AboutSection(onNavigateToPrivacyPolicy, onNavigateToLicenses)
            }

            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun UserProfileHeader(onEditClick: () -> Unit, viewModel: UserProfileViewModel = hiltViewModel()) {
    val profile by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        com.sherif.ledger.core.designsystem.component.LedgerAvatar(initials = profile.initials, size = 100.dp)

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
private fun OverviewSection(
    onAccountsClick: () -> Unit,
    onActivityClick: () -> Unit,
    onInsightsClick: () -> Unit,
) {
    Column {
        Text("OVERVIEW", style = LedgerTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(LedgerSpacing.Small))

        LedgerSurface(
            level = com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel.Inset,
            shape = com.sherif.ledger.core.designsystem.tokens.LedgerRadius.Large,
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRow(icon = Icons.Default.AccountBalanceWallet, label = "Accounts", onClick = onAccountsClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.AutoMirrored.Filled.ReceiptLong, label = "Activity", onClick = onActivityClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.PieChart, label = "Insights", onClick = onInsightsClick)
        }
    }
}

@Composable
private fun PreferencesSection(onDebugConsoleClick: () -> Unit, onAdjustBalanceClick: () -> Unit, onReviewInboxClick: () -> Unit, onAiSettingsClick: () -> Unit, onBudgetsClick: () -> Unit, onGoalsClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column {
        Text("PREFERENCES", style = LedgerTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(LedgerSpacing.Small))

        LedgerSurface(
            level = com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel.Inset,
            shape = com.sherif.ledger.core.designsystem.tokens.LedgerRadius.Large,
            contentPadding = PaddingValues(0.dp)
        ) {
            // Removed fabricated static rows (Currency/Theme/Language/Notifications/
            // Data & Privacy) — they displayed hard-coded values and did nothing. Theme
            // now lives in the gear → Settings. Only real, wired actions remain here.
            PreferenceRow(icon = Icons.Default.PieChart, label = "Budgets", onClick = onBudgetsClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.Savings, label = "Goals", onClick = onGoalsClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.AccountBalanceWallet, label = "Adjust Starting Balance", onClick = onAdjustBalanceClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.RateReview, label = "Review Uncategorized Transactions", onClick = onReviewInboxClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.SmartToy, label = "AI Settings", onClick = onAiSettingsClick)
            LedgerDivider(alpha = 0.05f)
            // Skippable during onboarding (SMS-only capture works standalone) —
            // this is where a user who skipped it comes back to enable
            // app-notification capture (Careem, wallet apps) later.
            PreferenceRow(
                icon = Icons.Default.NotificationsActive,
                label = "Notification Access",
                onClick = {
                    runCatching {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                },
            )

            if (com.sherif.ledger.BuildConfig.DEBUG) {
                LedgerDivider(alpha = 0.05f)
                PreferenceRow(icon = Icons.Default.BugReport, label = "Developer Console", onClick = onDebugConsoleClick)
            }
        }
    }
}

@Composable
private fun AboutSection(onPrivacyClick: () -> Unit, onLicensesClick: () -> Unit) {
    Column {
        Text("ABOUT", style = LedgerTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(LedgerSpacing.Small))

        LedgerSurface(
            level = com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel.Inset,
            shape = com.sherif.ledger.core.designsystem.tokens.LedgerRadius.Large,
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRow(icon = Icons.Default.Shield, label = "Privacy Policy", onClick = onPrivacyClick)
            LedgerDivider(alpha = 0.05f)
            PreferenceRow(icon = Icons.Default.Description, label = "Open Source Licenses", onClick = onLicensesClick)
            LedgerDivider(alpha = 0.05f)
            UpdateCheckRow()
        }
    }
}

/** Manual, user-triggered counterpart to MainActivity's silent launch-time check. */
@Composable
private fun UpdateCheckRow(
    viewModel: com.sherif.ledger.feature.update.presentation.UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    PreferenceRow(
        icon = Icons.Default.SystemUpdate,
        label = "Check for Updates",
        onClick = {
            showDialog = true
            viewModel.checkNow()
        },
    )

    if (showDialog) {
        com.sherif.ledger.feature.update.presentation.UpdateDialog(
            state = state,
            onDismiss = { showDialog = false; viewModel.dismiss() },
            onInstall = viewModel::downloadAndInstall,
        )
    }
}

@Composable
private fun CaptureCoverageCard(
    viewModel: com.sherif.ledger.feature.settings.presentation.viewmodel.CaptureCoverageViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsState()
    val s = summary ?: return

    Column {
        Text("IMPORT COVERAGE", style = LedgerTheme.typography.labelLarge.copy(letterSpacing = 1.sp), color = LedgerTheme.colors.textTertiary)
        Spacer(Modifier.height(LedgerSpacing.Small))
        LedgerSurface(
            level = com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel.Inset,
            shape = com.sherif.ledger.core.designsystem.tokens.LedgerRadius.Large,
            contentPadding = PaddingValues(LedgerSpacing.Medium),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
                CoverageRow("Window", s.windowLabel.ifBlank { "—" })
                CoverageRow("Messages scanned", s.smsScanned.toString())
                CoverageRow("In your window", s.smsWithinWindow.toString())
                CoverageRow("Skipped (outside window)", s.smsIgnoredOutsideWindow.toString())
                CoverageRow("Transactions captured", s.transactionsCreated.toString())
                CoverageRow("Merged / duplicates", s.transactionsMerged.toString())
                CoverageRow("Not a transaction", s.transactionsDiscarded.toString())
                if (s.smsIgnoredOutsideWindow > 0L) {
                    Spacer(Modifier.height(LedgerSpacing.Tiny))
                    Text(
                        "Skipped messages were outside the window you chose — they weren't captured. " +
                            "Import a wider range, or set your starting balance under \"Adjust Starting Balance\", to reconcile.",
                        style = LedgerTheme.typography.bodySmall,
                        color = LedgerTheme.colors.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverageRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = LedgerTheme.typography.bodyMedium, color = LedgerTheme.colors.textSecondary)
        Text(value, style = LedgerTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LedgerTheme.colors.textPrimary)
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

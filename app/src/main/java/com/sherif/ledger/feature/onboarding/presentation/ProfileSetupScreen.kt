package com.sherif.ledger.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerIconButton
import com.sherif.ledger.core.designsystem.component.LedgerTextField
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.onboarding.presentation.viewmodel.ProfileSetupViewModel

/**
 * Two entry points, one screen/ViewModel:
 * 1. First launch, before notification access or SMS import onboarding —
 *    "who are you" comes before "let's import your transactions." Fields
 *    start blank. Local-only: no password, no server, no account creation.
 * 2. Profile → "Edit Profile" ([isEditMode] = true), reachable anytime.
 *    Fields start pre-filled with whatever was saved before.
 *
 * Replaces what used to be a hardcoded "Sherif Rahim" / "SR" everywhere the
 * app showed a name or avatar (Dashboard's avatar, Profile's header) — see
 * UserProfileViewModel, the single place those are now derived from.
 */
@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    isEditMode: Boolean = false,
    onBackClick: () -> Unit = {},
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
    ) {
        if (isEditMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LedgerIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBackClick)
            }
            Spacer(Modifier.height(LedgerSpacing.Medium))
        } else {
            Spacer(Modifier.height(LedgerSpacing.XLarge))
        }

        Text(
            text = if (isEditMode) "Edit Profile" else "Welcome to Ledger",
            style = LedgerTextStyles.Headline,
            color = LedgerTheme.colors.textPrimary,
        )
        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            text = "Tell us who you are — this stays on your device.",
            style = LedgerTextStyles.BodyMedium,
            color = LedgerTheme.colors.textSecondary,
        )

        Spacer(Modifier.height(LedgerSpacing.Large))

        Text("Name", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        LedgerTextField(
            value = name,
            onValueChange = viewModel::setName,
            placeholder = "e.g. Jane Doe",
            keyboardType = KeyboardType.Text,
        )

        Spacer(Modifier.height(LedgerSpacing.Medium))

        Text("Email", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textSecondary)
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        LedgerTextField(
            value = email,
            onValueChange = viewModel::setEmail,
            placeholder = "e.g. jane@example.com",
            keyboardType = KeyboardType.Email,
        )

        Spacer(Modifier.weight(1f))

        LedgerButton(
            text = if (isEditMode) "Save" else "Continue",
            onClick = { viewModel.saveAndContinue(onComplete) },
            enabled = viewModel.canContinue(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(LedgerSpacing.Medium))
    }
}

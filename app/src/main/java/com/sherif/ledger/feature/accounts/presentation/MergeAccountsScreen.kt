package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerButtonStyle
import com.sherif.ledger.core.designsystem.component.LedgerDivider
import com.sherif.ledger.core.designsystem.component.LedgerScreenHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.theme.ledgerScreenBottomPadding
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius
import com.sherif.ledger.feature.accounts.presentation.viewmodel.MergeAccountOptionUi
import com.sherif.ledger.feature.accounts.presentation.viewmodel.MergeAccountsUiState

/**
 * ACCOUNT_IDENTITY_PLAN Steps 4-5: the explicit fix for an account that split
 * in two — pick which one survives, everything else moves onto it. Reachable
 * from Accounts, never triggered automatically.
 */
@Composable
fun MergeAccountsScreen(
    state: MergeAccountsUiState,
    onBackClick: () -> Unit = {},
    onSelectKeep: (Long) -> Unit = {},
    onSelectMerge: (Long) -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    val colors = LedgerTheme.colors

    LaunchedEffect(state.merged) {
        if (state.merged) onBackClick()
    }

    Scaffold(containerColor = colors.surfaceBase) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = LedgerSpacing.ScreenPadding, end = LedgerSpacing.ScreenPadding,
                bottom = ledgerScreenBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Large),
        ) {
            item {
                LedgerScreenHeader(
                    title = "Merge Accounts",
                    subtitle = "For two rows that are really the same account",
                    onBackClick = onBackClick,
                )
            }

            item {
                Text(
                    "Pick the account to keep, then the duplicate to merge into it. " +
                        "Every transaction moves onto the kept account and the duplicate " +
                        "is removed. This cannot be undone.",
                    style = LedgerTextStyles.BodyMedium,
                    color = colors.textSecondary,
                )
            }

            item { SectionLabel("KEEP") }
            items(state.accounts, key = { "keep_${it.id}" }) { account ->
                AccountPickRow(
                    account = account,
                    selected = state.keepAccountId == account.id,
                    disabled = state.mergeAccountId == account.id,
                    onClick = { onSelectKeep(account.id) },
                )
            }

            item { SectionLabel("MERGE INTO KEEP") }
            items(state.accounts, key = { "merge_${it.id}" }) { account ->
                AccountPickRow(
                    account = account,
                    selected = state.mergeAccountId == account.id,
                    disabled = state.keepAccountId == account.id,
                    onClick = { onSelectMerge(account.id) },
                )
            }

            state.errorMessage?.let { message ->
                item {
                    Text(message, style = LedgerTextStyles.Label, color = colors.negative)
                }
            }

            item {
                LedgerButton(
                    text = if (state.isSubmitting) "Merging…" else "Merge Accounts",
                    onClick = onConfirm,
                    style = LedgerButtonStyle.Solid,
                    enabled = state.canConfirm,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
        color = LedgerTheme.colors.textTertiary,
    )
}

@Composable
private fun AccountPickRow(
    account: MergeAccountOptionUi,
    selected: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LedgerTheme.colors
    LedgerSurface(
        level = if (selected) LedgerSurfaceLevel.Level2 else LedgerSurfaceLevel.Inset,
        shape = LedgerRadius.Medium,
        contentPadding = PaddingValues(LedgerSpacing.Medium),
        onClick = if (disabled) null else onClick,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (selected) colors.accent else colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (disabled) colors.textTertiary else colors.textPrimary,
                    maxLines = 1,
                )
                Text(account.subtitle, style = LedgerTextStyles.Caption, color = colors.textTertiary, maxLines = 1)
            }
            Text(
                account.balance,
                style = LedgerTextStyles.Label,
                color = if (disabled) colors.textTertiary else colors.textSecondary,
            )
        }
    }
}

#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 1 — Accounts screen..."

# ---- AccountsUiState.kt ----
rm -f "$B/feature/accounts/presentation/.gitkeep"
cat > "$B/feature/accounts/presentation/AccountsUiState.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation

data class AccountsUiState(
    val netWorth: String,
    val netWorthCurrency: String,
    val accountCount: Int,
    val sections: List<AccountSectionUi>,
)

data class AccountSectionUi(
    val title: String,
    val accounts: List<AccountUi>,
)

data class AccountUi(
    val id: String,
    val name: String,
    val subtitle: String,
    val balance: String,
    val isNegative: Boolean = false,
    val accentHue: Long = 0xFF6E6E6E,
)
EOF

# ---- AccountsPreviewData.kt ----
rm -f "$B/feature/accounts/presentation/preview/.gitkeep"
cat > "$B/feature/accounts/presentation/preview/AccountsPreviewData.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation.preview

import com.sherif.ledger.feature.accounts.presentation.AccountSectionUi
import com.sherif.ledger.feature.accounts.presentation.AccountUi
import com.sherif.ledger.feature.accounts.presentation.AccountsUiState

object AccountsPreviewData {

    val state = AccountsUiState(
        netWorth = "47,250.75",
        netWorthCurrency = "AED",
        accountCount = 5,
        sections = listOf(
            AccountSectionUi(
                title = "Bank Accounts",
                accounts = listOf(
                    AccountUi("1", "ADCB", "Current Account", "AED 32,450.50", accentHue = 0xFF2563EB),
                    AccountUi("2", "FAB", "Savings Account", "AED 8,200.25", accentHue = 0xFF7C3AED),
                    AccountUi("3", "Wio", "Digital Account", "AED 5,100.00", accentHue = 0xFF0D9488),
                ),
            ),
            AccountSectionUi(
                title = "Other",
                accounts = listOf(
                    AccountUi("4", "Cash Wallet", "Cash", "AED 1,500.00", accentHue = 0xFF78716C),
                    AccountUi("5", "Credit Card", "ADCB Touchpoints", "AED 0.00", accentHue = 0xFFDC2626),
                ),
            ),
        ),
    )
}
EOF

# ---- AccountRow.kt ----
rm -f "$B/feature/accounts/presentation/components/.gitkeep"
cat > "$B/feature/accounts/presentation/components/AccountRow.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountUi

@Composable
fun AccountRow(
    account: AccountUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val accent = Color(account.accentHue)
    val balanceColor = if (account.isNegative) LedgerTheme.colors.expense else LedgerTheme.colors.label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = LedgerTheme.opacity.Fill)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                account.name.take(1).uppercase(),
                style = LedgerTextStyles.Label,
                color = accent,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                account.name,
                style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label,
            )
            Text(
                account.subtitle,
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.tertiaryLabel,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            account.balance,
            style = LedgerTextStyles.Label,
            color = balanceColor,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = LedgerTheme.colors.tertiaryLabel,
            modifier = Modifier.size(18.dp),
        )
    }
}
EOF

# ---- AccountsScreen.kt ----
cat > "$B/feature/accounts/presentation/AccountsScreen.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.components.AccountRow
import com.sherif.ledger.feature.accounts.presentation.preview.AccountsPreviewData

@Composable
fun AccountsScreen(
    state: AccountsUiState = AccountsPreviewData.state,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen,
            end = LedgerSpacing.Screen,
            top = LedgerSpacing.XLarge,
            bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        // Net worth header
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = LedgerSpacing.Content),
            ) {
                Text(
                    "Accounts",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                    ),
                    color = LedgerTheme.colors.label,
                )
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                Text(
                    "Net Worth",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
                Spacer(Modifier.height(LedgerSpacing.Inline))
                Text(
                    "${state.netWorthCurrency} ${state.netWorth}",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        lineHeight = 40.sp,
                        letterSpacing = (-1).sp,
                    ),
                    color = Color.White,
                )
                Spacer(Modifier.height(LedgerSpacing.Inline))
                Text(
                    "${state.accountCount} accounts",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
            }
        }

        // Account sections
        state.sections.forEach { section ->
            item(key = "section_${section.title}") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
                    Text(
                        section.title,
                        style = LedgerTextStyles.Section,
                        color = LedgerTheme.colors.secondaryLabel,
                    )
                    LedgerSurface(
                        level = LedgerSurfaceLevel.Level1,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        section.accounts.forEachIndexed { index, account ->
                            AccountRow(account = account)
                            if (index != section.accounts.lastIndex) {
                                LedgerHairline(
                                    modifier = Modifier.padding(start = 70.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "Done. 4 files written."
echo "Run: git add -A && git commit -m 'feat(accounts): sprint 1 accounts screen with grouped surfaces and preview data'"
echo "Then: ./gradlew assembleDebug"

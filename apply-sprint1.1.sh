#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 1.1 — Product Spine..."

# ---- Part 1: Developer Navigation ----
cat > "$B/MainActivity.kt" << 'EOF'
package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Developer screen selector. Change [DEV_ACTIVE_SCREEN] to review
 * different features during development. This enum and the when-block
 * are temporary and will be replaced by production navigation.
 */
enum class DevScreen { Dashboard, Accounts }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LedgerTheme {
                when (DEV_ACTIVE_SCREEN) {
                    DevScreen.Dashboard -> LedgerNavHost()
                    DevScreen.Accounts -> AccountsScreen()
                }
            }
        }
    }

    companion object {
        /** Change this value to launch a different screen. */
        val DEV_ACTIVE_SCREEN = DevScreen.Dashboard
    }
}
EOF

# ---- Part 4: Shared LDL components ----

cat > "$B/core/designsystem/component/LedgerSectionHeader.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL section header for grouped content lists.
 *
 * Title on the left, optional trailing text on the right. Used above
 * inset grouped surfaces to label them. Two consumers today: Accounts
 * (section title + total) and prepared for Transactions (section title
 * + "See all").
 */
@Composable
fun LedgerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = LedgerTheme.colors.secondaryLabel,
    trailing: String? = null,
    trailingColor: Color = LedgerTheme.colors.tertiaryLabel,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = LedgerTextStyles.Section, color = titleColor)
        if (trailing != null) {
            Text(trailing, style = LedgerTextStyles.Caption, color = trailingColor)
        }
    }
}
EOF

cat > "$B/core/designsystem/component/LedgerAvatar.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL avatar for accounts, merchants, and categories.
 *
 * Displays an initial inside a tinted circle. When [painter] is provided
 * (e.g. from Coil or a resource), the image fills the circle instead.
 * This component is shared between Accounts (bank avatars) and prepared
 * for Transactions (merchant avatars).
 */
@Composable
fun LedgerAvatar(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    painter: Painter? = null,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(color.copy(alpha = LedgerTheme.opacity.Muted)),
        contentAlignment = Alignment.Center,
    ) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier.size(size).clip(CircleShape),
            )
        } else {
            Text(
                name.take(1).uppercase(),
                style = LedgerTextStyles.Label,
                color = color,
            )
        }
    }
}
EOF

# ---- Part 2 + 3: AccountRow polished + LDL audit ----

cat > "$B/feature/accounts/presentation/components/AccountRow.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.theme.LedgerMotion
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountUi

/**
 * Stateless expandable account row.
 *
 * Expansion state is owned by the caller so the screen holds state today
 * and a ViewModel holds it tomorrow without changing this component.
 * Canonical LDL pattern for expandable rows in Ledger.
 *
 * Touch target meets the 48dp accessibility minimum (row height is 56dp+).
 */
@Composable
fun AccountRow(
    account: AccountUi,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier,
    logoPainter: Painter? = null,
) {
    val accent = Color(account.accentHue)
    val balanceColor = if (account.isNegative) LedgerTheme.colors.expense else LedgerTheme.colors.label

    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = LedgerMotion.CardSpringDamping,
            stiffness = LedgerMotion.CardSpringStiffness,
        ),
        label = "chevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExpandToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerAvatar(name = account.name, color = accent, painter = logoPainter)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall),
            ) {
                Text(account.name, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
                Text(account.subtitle, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            }
            Spacer(Modifier.width(LedgerSpacing.Content))
            Text(account.balance, style = LedgerTextStyles.Label, color = balanceColor)
            Spacer(Modifier.width(LedgerSpacing.Inline))
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = LedgerTheme.colors.tertiaryLabel,
                modifier = Modifier.size(18.dp).rotate(chevronRotation),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = LedgerMotion.CardSpringDamping,
                    stiffness = LedgerMotion.CardSpringStiffness,
                ),
            ) + fadeIn(
                animationSpec = spring(
                    dampingRatio = LedgerMotion.CardSpringDamping,
                    stiffness = LedgerMotion.CardSpringStiffness,
                ),
            ),
            exit = shrinkVertically(
                animationSpec = spring(
                    dampingRatio = LedgerMotion.CardSpringDamping,
                    stiffness = LedgerMotion.CardSpringStiffness,
                ),
            ) + fadeOut(
                animationSpec = spring(
                    dampingRatio = LedgerMotion.CardSpringDamping,
                    stiffness = LedgerMotion.CardSpringStiffness,
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 68.dp,
                        end = LedgerSpacing.Group,
                        bottom = LedgerSpacing.Small,
                    ),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
            ) {
                if (account.accountNumber.isNotEmpty()) {
                    DetailLine("Account", account.accountNumber)
                }
                if (account.lastActivity.isNotEmpty()) {
                    DetailLine("Last activity", account.lastActivity)
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Text(
            text = value,
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.secondaryLabel,
        )
    }
}

EOF

# ---- AccountsScreen: use LedgerSectionHeader ----

cat > "$B/feature/accounts/presentation/AccountsScreen.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
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
    var expandedAccountId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.XLarge, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth()) {
                Text("Accounts", style = LedgerTextStyles.Headline, color = LedgerTheme.colors.label)
                Spacer(Modifier.height(LedgerSpacing.XLarge))
                Text("Net Worth", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                Spacer(Modifier.height(LedgerSpacing.Inline))
                Text(
                    "${state.netWorthCurrency} ${state.netWorth}",
                    style = LedgerTextStyles.Amount,
                    color = LedgerTheme.colors.label,
                )
                Spacer(Modifier.height(LedgerSpacing.Group))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Assets", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Spacer(Modifier.height(LedgerSpacing.XxSmall))
                        Text(state.assetsTotal, style = LedgerTextStyles.Label, color = LedgerTheme.colors.income)
                    }
                    Column {
                        Text("Liabilities", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Spacer(Modifier.height(LedgerSpacing.XxSmall))
                        Text(state.liabilitiesTotal, style = LedgerTextStyles.Label, color = LedgerTheme.colors.expense)
                    }
                }
            }
        }

        state.sections.forEach { section ->
            item(key = "section_${section.title}") {
                Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content)) {
                    LedgerSectionHeader(
                        title = section.title,
                        trailing = section.total,
                    )
                    LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(0.dp)) {
                        section.accounts.forEachIndexed { index, account ->
                            AccountRow(
                                account = account,
                                expanded = expandedAccountId == account.id,
                                onExpandToggle = { expandedAccountId = if (expandedAccountId == account.id) null else account.id },
                            )
                            if (index != section.accounts.lastIndex) {
                                LedgerHairline(modifier = Modifier.padding(start = 68.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
EOF

echo "Done. 5 files written."
echo "Run: git add -A && git commit -m 'feat(spine): sprint 1.1 dev navigation, interaction polish, shared LDL components'"
echo "Then: ./gradlew assembleDebug"

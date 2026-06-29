#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 5.2 — Premium Surface System..."

# ---- 1. LedgerInteraction — press compression + no-ripple click ----
cat > "$B/core/designsystem/component/LedgerInteraction.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.sherif.ledger.core.designsystem.theme.LedgerAnimations

/**
 * LDL click modifier: no ripple, subtle press compression.
 *
 * On press the surface scales to 0.985 with a micro-spring, then
 * bounces back on release. The compression communicates "this is
 * interactive" without the Material ripple splash.
 *
 * Consumers: LedgerSurface (when onClick provided), AccountRow,
 * ReviewCard, and any future interactive surface.
 */
fun Modifier.ledgerClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.985f else 1.0f,
        animationSpec = LedgerAnimations.microSpring(),
        label = "press_scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
}
EOF

# ---- 2. LedgerSurface — no Material Surface, subtle border, press support ----
cat > "$B/core/designsystem/component/LedgerSurface.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL grouped content surface.
 *
 * Replaces Material [androidx.compose.material3.Surface] entirely.
 * Depth comes from three signals:
 * 1. Tonal shift (background color via [level])
 * 2. Hairline border (separator at low opacity)
 * 3. Press compression (when [onClick] is provided)
 *
 * No Material elevation, no Material shadow, no Material ripple.
 * The surface feels carved into the page rather than floating above it.
 */
@Composable
fun LedgerSurface(
    modifier: Modifier = Modifier,
    level: LedgerSurfaceLevel = LedgerSurfaceLevel.Level1,
    contentPadding: PaddingValues = PaddingValues(LedgerSpacing.Group),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = LedgerRadius.Small
    val borderColor = LedgerTheme.colors.separator.copy(alpha = 0.20f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .clip(shape)
            .background(LedgerTheme.colors.surface(level))
            .border(width = 0.5.dp, color = borderColor, shape = shape)
            .padding(contentPadding),
        content = content,
    )
}
EOF

# ---- 3. LedgerHairline — ensure it uses tokens ----
cat > "$B/core/designsystem/component/LedgerHairline.kt" << 'EOF'
package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * LDL hairline separator.
 *
 * A 0.5dp line at separator color. Used inside grouped surfaces to
 * divide rows without creating visual weight. Thinner than Material
 * Divider (1dp) for a more refined appearance.
 */
@Composable
fun LedgerHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(LedgerTheme.colors.separator),
    )
}
EOF

# ---- 4. Update AccountRow to use ledgerClickable ----
cat > "$B/feature/accounts/presentation/components/AccountRow.kt" << 'EOF'
package com.sherif.ledger.feature.accounts.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerMotion
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountUi

/**
 * Stateless expandable account row with LDL press compression.
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
        animationSpec = spring(dampingRatio = LedgerMotion.CardSpringDamping, stiffness = LedgerMotion.CardSpringStiffness),
        label = "chevron",
    )

    Column(modifier = modifier.fillMaxWidth().ledgerClickable(onClick = onExpandToggle)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerAvatar(name = account.name, color = accent, modifier = Modifier.size(40.dp), painter = logoPainter)
            Spacer(Modifier.width(LedgerSpacing.Small))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall)) {
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
            enter = expandVertically(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)) + fadeIn(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)),
            exit = shrinkVertically(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)) + fadeOut(spring(LedgerMotion.CardSpringDamping, LedgerMotion.CardSpringStiffness)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 68.dp, end = LedgerSpacing.Group, bottom = LedgerSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
            ) {
                if (account.accountNumber.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Account", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Text(account.accountNumber, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
                    }
                }
                if (account.lastActivity.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last activity", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                        Text(account.lastActivity, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.secondaryLabel)
                    }
                }
            }
        }
    }
}
EOF

# ---- 5. Update TransactionRow to use ledgerClickable ----
# Read current file to preserve category utilities
TXROW="$B/feature/transactions/presentation/components/TransactionRow.kt"
if [ -f "$TXROW" ]; then
    # Extract category utilities (toColor + displayName) from current file
    UTILS=$(sed -n '/private fun MerchantCategory.toColor/,$ p' "$TXROW")
fi

cat > "$TXROW" << 'TXEOF'
package com.sherif.ledger.feature.transactions.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.transactions.presentation.MerchantCategory
import com.sherif.ledger.feature.transactions.presentation.TransactionState
import com.sherif.ledger.feature.transactions.presentation.TransactionUi

/**
 * Stateless transaction row with LDL press compression.
 */
@Composable
fun TransactionRow(
    transaction: TransactionUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val catColor = transaction.category.toColor()
    // TODO: Direction will become a dedicated domain concept.
    val isIncome = transaction.category == MerchantCategory.Salary
    val amountColor = if (isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense
    val sign = if (isIncome) "+" else "-"
    val dimmed = transaction.state == TransactionState.Pending

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = LedgerSpacing.Group, vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LedgerAvatar(name = transaction.merchant, color = catColor, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(LedgerSpacing.Small))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XxSmall)) {
            Text(
                transaction.merchant, style = LedgerTextStyles.Label,
                color = LedgerTheme.colors.label.let { if (dimmed) it.copy(alpha = LedgerTheme.opacity.Secondary) else it },
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${transaction.subtitle} \u00B7 ${transaction.category.displayName()}",
                style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(LedgerSpacing.Content))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${sign}AED ${transaction.amount.stripTrailingZeros().toPlainString()}",
                style = LedgerTextStyles.Label,
                color = amountColor.let { if (dimmed) it.copy(alpha = LedgerTheme.opacity.Secondary) else it },
            )
            when (transaction.state) {
                TransactionState.Pending -> Text("\u25CF Pending", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.pending)
                TransactionState.Recurring -> Text("\u21BA Monthly", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                TransactionState.Posted -> {}
            }
        }
    }
}

// TODO: Category colors will migrate into semantic LDL merchant palette tokens
// once multiple screens consume them.
private fun MerchantCategory.toColor(): Color = when (this) {
    MerchantCategory.Grocery -> Color(0xFF22C55E)
    MerchantCategory.Shopping -> Color(0xFFA855F7)
    MerchantCategory.Coffee -> Color(0xFF92400E)
    MerchantCategory.Fuel -> Color(0xFFEF4444)
    MerchantCategory.Salary -> Color(0xFF047857)
    MerchantCategory.Bills -> Color(0xFFEAB308)
    MerchantCategory.Transport -> Color(0xFF3B82F6)
    MerchantCategory.Entertainment -> Color(0xFFEC4899)
    MerchantCategory.Electronics -> Color(0xFF6B7280)
    MerchantCategory.Food -> Color(0xFFF97316)
    MerchantCategory.Healthcare -> Color(0xFF14B8A6)
    MerchantCategory.Travel -> Color(0xFF0EA5E9)
    MerchantCategory.Education -> Color(0xFF8B5CF6)
}

private fun MerchantCategory.displayName(): String = when (this) {
    MerchantCategory.Grocery -> "Groceries"
    MerchantCategory.Shopping -> "Shopping"
    MerchantCategory.Coffee -> "Coffee"
    MerchantCategory.Fuel -> "Fuel"
    MerchantCategory.Salary -> "Income"
    MerchantCategory.Bills -> "Bills"
    MerchantCategory.Transport -> "Transport"
    MerchantCategory.Entertainment -> "Entertainment"
    MerchantCategory.Electronics -> "Electronics"
    MerchantCategory.Food -> "Food"
    MerchantCategory.Healthcare -> "Healthcare"
    MerchantCategory.Travel -> "Travel"
    MerchantCategory.Education -> "Education"
}
TXEOF

echo "Done. 5 files written."
echo "Run: git add -A && git commit -m 'feat(ldl): sprint 5.2 premium surface system — borders, press compression, no Material Surface'"
echo "Then: ./gradlew assembleDebug"

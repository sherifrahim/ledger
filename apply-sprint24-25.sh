#!/bin/bash
set -e
B=app/src/main/java/com/sherif/ledger
echo "Applying Sprint 2.4 + 2.5..."
mkdir -p "$B/core/preview"
mkdir -p "$B/feature/transactions/presentation/detail/components"

# ==== Sprint 2.4: Canonical Preview Repository ====

cat > "$B/core/preview/PreviewMerchants.kt" << 'EOF'
package com.sherif.ledger.core.preview

/**
 * Canonical merchant catalog for preview data across all features.
 *
 * Every feature that displays a merchant should reference these objects
 * so the same merchant always carries the same name, category, and color.
 */
data class PreviewMerchant(
    val id: String,
    val name: String,
    val category: String,
    val accentHue: Long,
)

object PreviewMerchants {
    val amazon = PreviewMerchant("m_amazon", "Amazon", "Shopping", 0xFFA855F7)
    val carrefour = PreviewMerchant("m_carrefour", "Carrefour", "Groceries", 0xFF22C55E)
    val lulu = PreviewMerchant("m_lulu", "Lulu Hypermarket", "Groceries", 0xFF22C55E)
    val costaCoffee = PreviewMerchant("m_costa", "Costa Coffee", "Coffee", 0xFF92400E)
    val adnoc = PreviewMerchant("m_adnoc", "ADNOC", "Fuel", 0xFFEF4444)
    val noon = PreviewMerchant("m_noon", "Noon", "Shopping", 0xFFA855F7)
    val uber = PreviewMerchant("m_uber", "Uber", "Transport", 0xFF3B82F6)
    val careem = PreviewMerchant("m_careem", "Careem", "Transport", 0xFF3B82F6)
    val netflix = PreviewMerchant("m_netflix", "Netflix", "Entertainment", 0xFFEC4899)
    val spotify = PreviewMerchant("m_spotify", "Spotify", "Entertainment", 0xFFEC4899)
    val appleOne = PreviewMerchant("m_apple", "Apple One", "Technology", 0xFF6B7280)
    val du = PreviewMerchant("m_du", "du", "Bills", 0xFFEAB308)
    val etisalat = PreviewMerchant("m_etisalat", "Etisalat", "Bills", 0xFFEAB308)
    val zomato = PreviewMerchant("m_zomato", "Zomato", "Food", 0xFFF97316)
    val salary = PreviewMerchant("m_salary", "Salary", "Income", 0xFF047857)

    val all = listOf(amazon, carrefour, lulu, costaCoffee, adnoc, noon, uber, careem,
        netflix, spotify, appleOne, du, etisalat, zomato, salary)
}
EOF

cat > "$B/core/preview/PreviewAccounts.kt" << 'EOF'
package com.sherif.ledger.core.preview

data class PreviewAccount(
    val id: String,
    val name: String,
    val subtitle: String,
    val balance: String,
    val accountNumber: String,
    val accentHue: Long,
)

object PreviewAccounts {
    val adcb = PreviewAccount("a_adcb", "ADCB", "Current Account", "AED 32,450.50", "\u2022\u20224521", 0xFF2563EB)
    val fab = PreviewAccount("a_fab", "FAB", "Savings Account", "AED 8,200.25", "\u2022\u20227803", 0xFF7C3AED)
    val wio = PreviewAccount("a_wio", "Wio", "Digital Account", "AED 5,100.00", "\u2022\u20221190", 0xFF0D9488)
    val cash = PreviewAccount("a_cash", "Cash Wallet", "Cash", "AED 1,500.00", "", 0xFF78716C)
    val creditCard = PreviewAccount("a_cc", "ADCB Touchpoints", "Credit Card", "AED 0.00", "\u2022\u20228847", 0xFFDC2626)
}
EOF

cat > "$B/core/preview/PreviewTransactions.kt" << 'EOF'
package com.sherif.ledger.core.preview

/**
 * Canonical transaction samples for preview data.
 *
 * Each transaction references a [PreviewMerchant] and [PreviewAccount]
 * so data stays consistent across Dashboard, Transactions, Transaction
 * Details, and future screens.
 */
data class PreviewTransaction(
    val id: String,
    val merchant: PreviewMerchant,
    val account: PreviewAccount,
    val amount: String,
    val isIncome: Boolean = false,
    val date: String,
    val time: String,
    val status: String = "Posted",
    val paymentMethod: String = "Debit Card",
    val reference: String = "",
    val isRecurring: Boolean = false,
)

object PreviewTransactions {
    val amazonToday = PreviewTransaction("t1", PreviewMerchants.amazon, PreviewAccounts.adcb, "52", date = "27 Jun 2026", time = "2:14 PM", reference = "TXN-2026-4851")
    val carrefourToday = PreviewTransaction("t2", PreviewMerchants.carrefour, PreviewAccounts.adcb, "126", date = "27 Jun 2026", time = "11:02 AM", reference = "TXN-2026-4850")
    val costaToday = PreviewTransaction("t3", PreviewMerchants.costaCoffee, PreviewAccounts.wio, "19", date = "27 Jun 2026", time = "9:30 AM", paymentMethod = "Apple Pay", reference = "TXN-2026-4849")
    val adnocYesterday = PreviewTransaction("t4", PreviewMerchants.adnoc, PreviewAccounts.adcb, "120", date = "26 Jun 2026", time = "3:15 PM", reference = "TXN-2026-4842")
    val noonPending = PreviewTransaction("t5", PreviewMerchants.noon, PreviewAccounts.adcb, "245", date = "26 Jun 2026", time = "1:20 PM", status = "Pending", reference = "TXN-2026-4841")
    val uberYesterday = PreviewTransaction("t6", PreviewMerchants.uber, PreviewAccounts.wio, "32", date = "26 Jun 2026", time = "8:45 AM", reference = "TXN-2026-4838")
    val salaryMonday = PreviewTransaction("t7", PreviewMerchants.salary, PreviewAccounts.fab, "9,500", isIncome = true, date = "23 Jun 2026", time = "Bank Transfer", paymentMethod = "Bank Transfer", reference = "SAL-2026-06")
    val netflixMonday = PreviewTransaction("t8", PreviewMerchants.netflix, PreviewAccounts.adcb, "55", date = "23 Jun 2026", time = "12:00 AM", isRecurring = true, reference = "TXN-2026-4820")
    val duMonday = PreviewTransaction("t9", PreviewMerchants.du, PreviewAccounts.adcb, "399", date = "23 Jun 2026", time = "12:00 AM", isRecurring = true, reference = "TXN-2026-4819")

    val all = listOf(amazonToday, carrefourToday, costaToday, adnocYesterday, noonPending, uberYesterday, salaryMonday, netflixMonday, duMonday)
}
EOF

# ==== Sprint 2.5: Transaction Details ====

cat > "$B/feature/transactions/presentation/detail/TransactionDetailsUiState.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.detail

data class TransactionDetailsUiState(
    val merchant: String,
    val merchantCategory: String,
    val merchantAccentHue: Long,
    val amount: String,
    val sign: String,
    val isIncome: Boolean,
    val date: String,
    val time: String,
    val status: String,
    val paymentMethod: String,
    val accountName: String,
    val accountNumber: String,
    val reference: String,
    val history: List<MerchantHistoryItem>,
    val notes: String? = null,
)

data class MerchantHistoryItem(
    val amount: String,
    val date: String,
    val isIncome: Boolean = false,
)
EOF

cat > "$B/feature/transactions/presentation/detail/components/MerchantHeader.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.component.LedgerAvatar
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Merchant identity header for transaction detail and review screens.
 *
 * Centered avatar above merchant name and category. Future consumers:
 * Review Inbox (confirming a parsed merchant) and Merchant Profile.
 */
@Composable
fun MerchantHeader(
    name: String,
    category: String,
    accentHue: Long,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Content),
    ) {
        LedgerAvatar(
            name = name,
            color = Color(accentHue),
            modifier = Modifier.size(56.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, style = LedgerTextStyles.Title, color = LedgerTheme.colors.label)
            Spacer(Modifier.height(LedgerSpacing.XxSmall))
            Text(category, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        }
    }
}
EOF

cat > "$B/feature/transactions/presentation/detail/components/AmountHero.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Large centered amount display for transaction detail.
 *
 * The amount is the emotional anchor: the user instantly knows
 * how much. Status and time sit below as quiet context.
 * Future consumers: Review Inbox and Capture confirmation.
 */
@Composable
fun AmountHero(
    amount: String,
    sign: String,
    isIncome: Boolean,
    status: String,
    time: String,
    modifier: Modifier = Modifier,
) {
    val amountColor = if (isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Inline),
    ) {
        Text(
            "${sign}AED $amount",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
            color = amountColor,
        )
        Text(
            "$status \u00B7 $time",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
    }
}
EOF

cat > "$B/feature/transactions/presentation/detail/components/InformationSection.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Key-value information group inside a grouped surface.
 *
 * Renders label-value pairs separated by hairlines inside a
 * LedgerSurface. Future consumers: Review Inbox (parsed fields)
 * and Merchant Profile (merchant metadata).
 */
@Composable
fun InformationSection(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    LedgerSurface(
        modifier = modifier,
        level = LedgerSurfaceLevel.Level1,
        contentPadding = PaddingValues(LedgerSpacing.Inline),
    ) {
        items.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                Text(value, style = LedgerTextStyles.Label, color = LedgerTheme.colors.label)
            }
            if (index != items.lastIndex) {
                LedgerHairline()
            }
        }
    }
}
EOF

cat > "$B/feature/transactions/presentation/detail/TransactionDetailsScreen.kt" << 'EOF'
package com.sherif.ledger.feature.transactions.presentation.detail

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.preview.PreviewTransactions
import com.sherif.ledger.feature.transactions.presentation.detail.components.AmountHero
import com.sherif.ledger.feature.transactions.presentation.detail.components.InformationSection
import com.sherif.ledger.feature.transactions.presentation.detail.components.MerchantHeader

private val previewState = run {
    val t = PreviewTransactions.amazonToday
    TransactionDetailsUiState(
        merchant = t.merchant.name,
        merchantCategory = t.merchant.category,
        merchantAccentHue = t.merchant.accentHue,
        amount = t.amount,
        sign = if (t.isIncome) "+" else "-",
        isIncome = t.isIncome,
        date = t.date,
        time = t.time,
        status = t.status,
        paymentMethod = t.paymentMethod,
        accountName = t.account.name,
        accountNumber = t.account.accountNumber,
        reference = t.reference,
        history = listOf(
            MerchantHistoryItem("AED 89", "20 Jun"),
            MerchantHistoryItem("AED 245", "14 Jun"),
            MerchantHistoryItem("AED 32", "3 Jun"),
        ),
        notes = null,
    )
}

@Composable
fun TransactionDetailsScreen(
    state: TransactionDetailsUiState = previewState,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0),
        contentPadding = PaddingValues(
            start = LedgerSpacing.Screen, end = LedgerSpacing.Screen,
            top = LedgerSpacing.XxLarge, bottom = LedgerSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.XLarge),
    ) {
        item("merchant") {
            MerchantHeader(
                name = state.merchant,
                category = state.merchantCategory,
                accentHue = state.merchantAccentHue,
            )
        }

        item("amount") {
            AmountHero(
                amount = state.amount,
                sign = state.sign,
                isIncome = state.isIncome,
                status = state.status,
                time = state.time,
            )
        }

        item("info") {
            InformationSection(
                items = listOf(
                    "Date" to state.date,
                    "Time" to state.time,
                    "Payment" to state.paymentMethod,
                    "Account" to "${state.accountName} ${state.accountNumber}",
                    "Reference" to state.reference,
                ),
            )
        }

        if (state.history.isNotEmpty()) {
            item("history_header") {
                LedgerSectionHeader(
                    title = "Previous at ${state.merchant}",
                    trailing = "${state.history.size} transactions",
                )
            }
            item("history") {
                LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(LedgerSpacing.Inline)) {
                    state.history.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Small),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(item.date, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                            Text(
                                "${if (item.isIncome) "+" else "-"}${item.amount}",
                                style = LedgerTextStyles.Label,
                                color = if (item.isIncome) LedgerTheme.colors.income else LedgerTheme.colors.expense,
                            )
                        }
                        if (index != state.history.lastIndex) LedgerHairline()
                    }
                }
            }
        }

        item("notes") {
            LedgerSectionHeader(title = "Notes")
            Spacer(Modifier.height(LedgerSpacing.Content))
            LedgerSurface(level = LedgerSurfaceLevel.Level1, contentPadding = PaddingValues(LedgerSpacing.Group)) {
                Text(
                    state.notes ?: "No notes yet",
                    style = LedgerTextStyles.Body,
                    color = LedgerTheme.colors.tertiaryLabel,
                )
            }
        }
    }
}
EOF

# ---- DevScreen update ----
cat > "$B/MainActivity.kt" << 'EOF'
package com.sherif.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.AccountsScreen
import com.sherif.ledger.feature.transactions.presentation.TransactionsScreen
import com.sherif.ledger.feature.transactions.presentation.detail.TransactionDetailsScreen
import com.sherif.ledger.presentation.navigation.LedgerNavHost
import dagger.hilt.android.AndroidEntryPoint

enum class DevScreen { Dashboard, Accounts, Transactions, TransactionDetails }

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
                    DevScreen.Transactions -> TransactionsScreen()
                    DevScreen.TransactionDetails -> TransactionDetailsScreen()
                }
            }
        }
    }

    companion object {
        val DEV_ACTIVE_SCREEN = DevScreen.Dashboard
    }
}
EOF

echo "Done. 9 files written."
echo "Run: git add -A && git commit -m 'feat: sprint 2.4-2.5 canonical preview repo + transaction details'"
echo "Then: ./gradlew assembleDebug"

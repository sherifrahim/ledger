package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * The amount as it should be typeset: exactly one leading sign, and a real
 * U+2212 MINUS SIGN rather than a hyphen.
 *
 * The distinction matters because amounts are set in a tabular ("tnum") face,
 * where every glyph is padded to one digit width. A hyphen is drawn narrow, so
 * tabular spacing adds the difference as whitespace and the row reads "− 21.50"
 * with a visible gap between sign and number. The true minus is designed at digit
 * width and sits tight — which is the reason it exists as a separate character.
 *
 * Callers pass amounts in either shape (already signed, or bare), so this
 * normalises rather than assumes.
 */
internal fun signedAmount(amount: String, isExpense: Boolean): String {
    val bare = amount.removePrefix("-").removePrefix("−").removePrefix("+")
    val negative = amount.startsWith("-") || amount.startsWith("−") || (isExpense && !amount.startsWith("+"))
    return if (negative) "−$bare" else "+$bare"
}

/**
 * Ledger V3 Transaction Activity Row
 *
 * Focuses on Intelligence: Explains the event rather than just listing it.
 */
@Composable
fun LedgerTransactionRow(
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    explanation: String = "", // "Intelligence Layer"
    currency: String = "AED",
    isExpense: Boolean = true,
    subtitle: String? = null,
    metadata: String? = null,
    tag: String? = null,
    status: String? = null,
    amountColor: Color? = null,
    dimmed: Boolean = false,
    time: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = LedgerTheme.colors
    val finalExplanation = explanation.ifEmpty { subtitle ?: "" }
    val finalAmountColor = amountColor ?: if (isExpense) colors.textPrimary else colors.positive

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.ledgerClickable(onClick = onClick) else Modifier)
            .padding(vertical = LedgerSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 1. Merchant Identity
        LedgerBrandIcon(
            name = title,
            size = 40.dp
        )

        Spacer(Modifier.width(LedgerSpacing.Medium))

        // 2. Narrative Intelligence.
        // The gap to the amount is deliberately tighter than the gap after the
        // brand mark: every dp given to the gutter is a dp taken from the merchant
        // name, and merchant names are what people actually scan for.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LedgerTextStyles.BodyMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (finalExplanation.isNotEmpty()) {
                Text(
                    text = finalExplanation,
                    style = LedgerTextStyles.Label,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(LedgerSpacing.Small))

        // 3. Authority of Amount
        Column(horizontalAlignment = Alignment.End) {
            LedgerAmount(
                amount = signedAmount(amount, isExpense),
                currency = currency,
                style = LedgerAmountStyle.Regular,
                color = finalAmountColor
            )
            if (time != null || status != null) {
                Text(
                    text = time ?: status ?: "",
                    style = LedgerTextStyles.Caption,
                    color = colors.textTertiary
                )
            }
        }
    }
}

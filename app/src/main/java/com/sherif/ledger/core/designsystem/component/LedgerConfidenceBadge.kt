package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * Confidence tiers, aligned to the canonical confidence ladder (spec D5).
 * The engine emits a 0–100 score; the UI only ever renders these bands so the
 * language of certainty is consistent everywhere (Review, Dashboard, detail).
 */
enum class LedgerConfidence(val label: String) {
    Deterministic("Verified"),     // 95–100 — reconciled fact
    VeryHigh("High confidence"),   // 85–94
    High("Likely"),                // 70–84
    NeedsReview("Needs review"),   // 50–69
    Low("Low confidence");         // < 50

    companion object {
        fun fromScore(score: Int): LedgerConfidence = when {
            score >= 95 -> Deterministic
            score >= 85 -> VeryHigh
            score >= 70 -> High
            score >= 50 -> NeedsReview
            else -> Low
        }
    }
}

@Composable
private fun LedgerConfidence.tint(): Color = when (this) {
    LedgerConfidence.Deterministic, LedgerConfidence.VeryHigh -> LedgerTheme.colors.positive
    LedgerConfidence.High, LedgerConfidence.NeedsReview -> LedgerTheme.colors.attention
    LedgerConfidence.Low -> LedgerTheme.colors.negative
}

/**
 * The compact confidence chip used on review cards: a large tabular percentage
 * over its tier label, tinted by band. Explainability over decoration — the
 * number is always paired with a word.
 */
@Composable
fun LedgerConfidenceBadge(
    score: Int,
    modifier: Modifier = Modifier,
    tier: LedgerConfidence = LedgerConfidence.fromScore(score),
) {
    val tint = tier.tint()
    Column(
        modifier = modifier
            .clip(LedgerRadius.Medium)
            .background(tint.copy(alpha = if (LedgerTheme.colors.isDark) 0.16f else 0.10f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$score%",
            style = LedgerTextStyles.Title.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = tint,
        )
        Text(
            text = tier.label,
            style = LedgerTextStyles.Caption.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
            color = tint,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Inline pill variant (percentage + label on one line) for tighter contexts
 * such as list rows and headers.
 */
@Composable
fun LedgerConfidencePill(
    score: Int,
    modifier: Modifier = Modifier,
    tier: LedgerConfidence = LedgerConfidence.fromScore(score),
) {
    val tint = tier.tint()
    Text(
        text = "$score% · ${tier.label}",
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.SemiBold),
        color = tint,
        modifier = modifier
            .clip(LedgerRadius.Full)
            .background(tint.copy(alpha = if (LedgerTheme.colors.isDark) 0.16f else 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

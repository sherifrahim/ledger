package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * The user's own avatar — one signature gradient (accent/system pairing, the
 * same one LedgerHeroCard and the Liquid Glass ambient backdrop use), never a
 * flat surface-color circle. Design review finding F2 (2026-08-06): the
 * Dashboard greeting used a flat gray circle while Settings used this
 * gradient for the identical initials — the same person rendering two
 * different ways depending which screen you're on. This is now the one place
 * that decision is made, so both stay in sync by construction.
 */
@Composable
fun LedgerAvatar(
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(LedgerTheme.colors.positive, LedgerTheme.colors.system),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Scales with the avatar itself rather than a fixed style — a 44dp nav
        // avatar and a 100dp profile avatar need different type sizes to keep
        // the initials reading as centered and proportionate, not lost or
        // overflowing at either extreme.
        Text(
            initials,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

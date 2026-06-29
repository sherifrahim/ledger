package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL atomic tag.
 *
 * Purged of Material 3 Surface. Used for categories, transaction
 * metadata and status indicators.
 */
@Composable
fun LedgerTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LedgerTheme.colors.tint.copy(alpha = LedgerTheme.opacity.Fill),
    contentColor: Color = LedgerTheme.colors.tint,
) {
    val shape = LedgerRadius.Small
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .padding(
                horizontal = LedgerSpacing.Small,
                vertical = LedgerSpacing.XxSmall,
            ),
    ) {
        Text(
            text = text,
            style = LedgerTextStyles.Caption,
            color = contentColor,
        )
    }
}

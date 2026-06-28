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

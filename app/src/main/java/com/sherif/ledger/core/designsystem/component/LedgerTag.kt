package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerTag(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LedgerTheme.colors.tint.copy(alpha = LedgerTheme.opacity.Fill),
    contentColor: Color = LedgerTheme.colors.tint,
) {
    Surface(modifier = modifier, shape = LedgerShapes.small, color = containerColor, contentColor = contentColor) {
        Text(text, Modifier.padding(PaddingValues(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.XxSmall)), style = LedgerTextStyles.Caption)
    }
}

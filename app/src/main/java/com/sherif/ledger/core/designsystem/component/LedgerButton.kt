package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

enum class LedgerButtonStyle { Primary, Secondary, Text }

@Composable
fun LedgerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: LedgerButtonStyle = LedgerButtonStyle.Primary,
) {
    val contentPadding = PaddingValues(horizontal = LedgerSpacing.XLarge, vertical = LedgerSpacing.Small)
    val shape = LedgerShapes.small
    when (style) {
        LedgerButtonStyle.Primary -> Button(onClick, modifier, enabled, shape = shape, contentPadding = contentPadding) { Text(text) }
        LedgerButtonStyle.Secondary -> OutlinedButton(onClick, modifier, enabled, shape = shape, contentPadding = contentPadding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LedgerTheme.colors.tint)) { Text(text) }
        LedgerButtonStyle.Text -> TextButton(onClick, modifier, enabled, shape = shape, contentPadding = contentPadding) { Text(text) }
    }
}

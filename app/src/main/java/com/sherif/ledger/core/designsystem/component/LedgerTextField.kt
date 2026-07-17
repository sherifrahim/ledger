package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL free-text input — same visual language as [LedgerSearchBar]/
 * [LedgerAmountInputField] (a plain surface + BasicTextField), generalized
 * for names, emails, and similar single-line text rather than search queries
 * or signed decimal amounts specifically.
 */
@Composable
fun LedgerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LedgerTextStyles.BodyLarge.copy(color = LedgerTheme.colors.textPrimary),
        cursorBrush = SolidColor(LedgerTheme.colors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LedgerRadius.Medium)
                    .background(LedgerTheme.colors.surfaceLevel1)
                    .padding(horizontal = LedgerSpacing.Small, vertical = LedgerSpacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = LedgerTextStyles.BodyLarge,
                            color = LedgerTheme.colors.textTertiary,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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

/** LDL-styled signed decimal amount input — digits, at most one leading '-', at most one '.'. */
@Composable
fun LedgerAmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    currencySymbol: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = { new ->
            if (new.matches(Regex("^-?\\d*(\\.\\d*)?$"))) onValueChange(new)
        },
        singleLine = true,
        textStyle = LedgerTextStyles.BodyLarge.copy(color = LedgerTheme.colors.textPrimary),
        cursorBrush = SolidColor(LedgerTheme.colors.textPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                Text(currencySymbol, style = LedgerTextStyles.BodyLarge, color = LedgerTheme.colors.textTertiary)
                Box(modifier = Modifier.width(LedgerSpacing.Tiny))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "e.g. $placeholder",
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

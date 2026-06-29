package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.core.designsystem.tokens.LedgerRadius

/**
 * LDL search field.
 *
 * Uses BasicTextField instead of Material OutlinedTextField so Ledger owns
 * the entire visual language and interaction model.
 */
@Composable
fun LedgerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        textStyle = LedgerTextStyles.Body.copy(
            color = LedgerTheme.colors.label,
        ),
        cursorBrush = SolidColor(LedgerTheme.colors.tint),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search,
        ),
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() },
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(LedgerRadius.Medium)
                    .background(LedgerTheme.colors.surfaceLevel1)
                    .padding(
                        horizontal = LedgerSpacing.Small,
                        vertical = LedgerSpacing.Small,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = LedgerTheme.colors.tertiaryLabel,
                    modifier = Modifier.size(LedgerTheme.iconSize.Medium),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = LedgerSpacing.Content),
                ) {

                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = LedgerTextStyles.Body,
                            color = LedgerTheme.colors.tertiaryLabel,
                        )
                    }

                    innerTextField()
                }

                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = LedgerTheme.colors.tertiaryLabel,
                        modifier = Modifier
                            .size(LedgerTheme.iconSize.Medium)
                            .ledgerClickable {
                                onQueryChange("")
                            },
                    )
                }
            }
        },
    )
}

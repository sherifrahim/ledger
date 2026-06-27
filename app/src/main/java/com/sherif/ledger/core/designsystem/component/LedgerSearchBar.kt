package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import com.sherif.ledger.core.designsystem.theme.LedgerShapes
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun LedgerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    enabled: Boolean = true,
    searchIcon: ImageVector = Icons.Filled.Search,
    clearIcon: ImageVector = Icons.Filled.Close,
) {
    val colors = LedgerTheme.colors
    OutlinedTextField(
        value = query, onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        textStyle = LedgerTextStyles.Body,
        placeholder = { Text(placeholder, style = LedgerTextStyles.Body) },
        leadingIcon = { Icon(searchIcon, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(clearIcon, "Clear search") }
        },
        shape = LedgerShapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.tint,
            unfocusedBorderColor = colors.separator,
            focusedContainerColor = colors.surfaceLevel1,
            unfocusedContainerColor = colors.surfaceLevel1,
        ),
    )
}

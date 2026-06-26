package com.sherif.ledger.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    com.sherif.ledger.core.designsystem.theme.LedgerTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content,
    )
}

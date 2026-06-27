package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LedgerTheme.colors
    CenterAlignedTopAppBar(
        title = { Text(title, style = LedgerTextStyles.Section) },
        modifier = modifier, navigationIcon = navigationIcon, actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = colors.surfaceLevel0,
            titleContentColor = colors.label,
            navigationIconContentColor = colors.label,
            actionIconContentColor = colors.label,
        ),
    )
}

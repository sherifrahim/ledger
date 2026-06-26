package com.sherif.ledger.presentation.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Dashboard greeting shown at the top of the Home screen.
 */
@Composable
fun GreetingHeader(

    greeting: String,

    userName: String,

    modifier: Modifier = Modifier

) {

    Column(
        modifier = modifier.fillMaxWidth()
    ) {

Text(
    text = "$greeting,",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

Text(
    text = userName,
    fontSize = 34.sp,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onSurface
)
    }

}

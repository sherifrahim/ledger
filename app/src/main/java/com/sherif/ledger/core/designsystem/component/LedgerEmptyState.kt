package com.sherif.ledger.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Standardized empty state for Ledger screens.
 * Encourages the user to wait for automatic transaction detection.
 */
@Composable
fun LedgerEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
) {
    val colors = LedgerTheme.colors
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(LedgerSpacing.XLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.tertiaryLabel.copy(alpha = LedgerTheme.opacity.Muted),
            modifier = Modifier.size(MassiveIcon)
        )
        
        Spacer(Modifier.height(LedgerSpacing.Large))
        
        Text(
            text = title,
            style = LedgerTextStyles.Section.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = colors.label.copy(alpha = LedgerTheme.opacity.Emphasis),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(LedgerSpacing.Small))
        
        Text(
            text = subtitle,
            style = LedgerTextStyles.Caption.copy(
                lineHeight = 18.sp
            ),
            color = colors.secondaryLabel.copy(alpha = LedgerTheme.opacity.Overlay),
            textAlign = TextAlign.Center
        )
    }
}

private val MassiveIcon = 64.dp

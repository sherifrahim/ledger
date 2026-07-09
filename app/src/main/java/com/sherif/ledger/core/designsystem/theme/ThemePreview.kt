package com.sherif.ledger.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereGlow
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface

/**
 * Side-by-side comparison of Ledger themes.
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 1000, heightDp = 600)
@Composable
fun LedgerThemesComparisonPreview() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ThemeColumn("Classic", LedgerThemeType.Classic, modifier = Modifier.weight(1f))
        ThemeColumn("Glass", LedgerThemeType.Glass, modifier = Modifier.weight(1f))
        ThemeColumn("Midnight", LedgerThemeType.MidnightGlass, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ThemeColumn(
    name: String,
    themeType: LedgerThemeType,
    modifier: Modifier = Modifier
) {
    LedgerTheme(themeType = themeType) {
        val colors = LedgerTheme.colors
        Box(
            modifier = modifier
                .fillMaxHeight()
                .background(colors.surfaceLevel0)
        ) {
            LedgerAtmosphereGlow(Modifier.fillMaxSize())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LedgerHeader(title = name)
                
                LedgerSurface {
                    Text(
                        text = "Grouped Surface",
                        style = LedgerTextStyles.Body,
                        color = colors.label
                    )
                    Spacer(Modifier.height(8.dp))
                    LedgerAmount(amount = "AED 1,240.00", style = LedgerAmountStyle.Large)
                }
                
                LedgerButton(text = "Primary Button", onClick = {})
            }
        }
    }
}

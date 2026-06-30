package com.sherif.ledger.feature.accounts.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereGlow
import com.sherif.ledger.core.designsystem.component.LedgerAmount
import com.sherif.ledger.core.designsystem.component.LedgerAmountStyle
import com.sherif.ledger.core.designsystem.component.LedgerHairline
import com.sherif.ledger.core.designsystem.component.LedgerSectionHeader
import com.sherif.ledger.core.designsystem.component.LedgerSurface
import com.sherif.ledger.core.designsystem.component.hero.LedgerCollapsingHero
import com.sherif.ledger.core.designsystem.component.ledgerClickable
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.accounts.presentation.components.AccountRow
import com.sherif.ledger.feature.accounts.presentation.preview.AccountsPreviewData

@Composable
fun AccountsScreen(
    state: AccountsUiState = AccountsPreviewData.state,
) {
    val expandedHeight = 220.dp
    val collapsedHeight = 56.dp
    val maxOffsetPx = with(LocalDensity.current) { (expandedHeight - collapsedHeight).toPx() }
    val listState = rememberLazyListState()
    val collapseProgress by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / maxOffsetPx).coerceIn(0f, 1f)
        }
    }

    Box(Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0)) {
        LedgerAtmosphereGlow(Modifier.fillMaxSize())

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LedgerSpacing.Screen,
                end = LedgerSpacing.Screen,
                bottom = LedgerSpacing.ScreenBottom + 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section),
        ) {
            item(key = "hero_spacer") { Spacer(Modifier.height(expandedHeight)) }

            state.sections.forEach { section ->
                item(key = "section_${section.title}") {
                    Column(verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Group)) {
                        LedgerSectionHeader(title = section.title.uppercase())
                        Column(modifier = Modifier.fillMaxWidth()) {
                            section.accounts.forEachIndexed { index, account ->
                                AccountRow(
                                    account = account,
                                    onClick = { /* TODO */ }
                                )
                                if (index != section.accounts.lastIndex) {
                                    LedgerHairline(modifier = Modifier.padding(start = LedgerSpacing.AvatarIndent))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "add_account") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .ledgerSurface(level = LedgerSurfaceLevel.Level1)
                        .ledgerClickable { /* TODO */ }
                        .padding(LedgerSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = LedgerTheme.colors.success,
                        modifier = Modifier.size(LedgerTheme.iconSize.Medium)
                    )
                    Spacer(Modifier.width(LedgerSpacing.Medium))
                    Text(
                        text = "Add account",
                        style = LedgerTextStyles.Label,
                        color = LedgerTheme.colors.label
                    )
                }
            }
        }

        LedgerCollapsingHero(
            collapseProgress = collapseProgress,
            expandedHeight = expandedHeight,
            collapsedHeight = collapsedHeight,
            background = SolidColor(Color.Transparent),
            expandedContent = { progress ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .graphicsLayer { alpha = (1f - progress * 2f).coerceIn(0f, 1f) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TOTAL BALANCE",
                            style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Color.White.copy(alpha = 0.35f)
                        )
                        Spacer(Modifier.height(LedgerSpacing.XxSmall))
                        LedgerAmount(
                            amount = "AED 2,840.25",
                            style = LedgerAmountStyle.Display,
                            color = Color.White,
                        )
                    }
                }
            },
            compactContent = { progress ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .graphicsLayer { alpha = ((progress - 0.6f) * 2.5f).coerceIn(0f, 1f) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Accounts",
                        style = LedgerTextStyles.Section,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = LedgerSpacing.Screen)
                    )
                }
            }
        )
    }
}

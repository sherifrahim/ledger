package com.sherif.ledger.feature.debug.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherif.ledger.core.designsystem.atmosphere.LedgerAtmosphereGlow
import com.sherif.ledger.core.designsystem.component.*
import com.sherif.ledger.core.designsystem.component.debug.*
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

/**
 * Permanent diagnostics console for Ledger.
 */
@Composable
fun DeveloperConsoleScreen(
    onBackClick: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize().background(LedgerTheme.colors.surfaceLevel0)) {
        LedgerAtmosphereGlow(Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = LedgerSpacing.Screen,
                end = LedgerSpacing.Screen,
                top = LedgerSpacing.Large,
                bottom = LedgerSpacing.ScreenBottom
            ),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Section)
        ) {
            item("header") {
                Header(onBackClick)
            }

            item("pipeline") {
                ConsoleSection(title = "Pipeline") {
                    LedgerEmptyState(
                        title = "No diagnostic data available",
                        subtitle = "Trigger a transaction ingestion to see pipeline traces."
                    )
                }
            }

            item("benchmark") {
                ConsoleSection(title = "Benchmark") {
                    LedgerEmptyState(
                        title = "No benchmark data available",
                        subtitle = "Run the regression suite to see accuracy metrics."
                    )
                }
            }

            item("corpus") {
                ConsoleSection(title = "Corpus") {
                    LedgerEmptyState(
                        title = "No corpus data available",
                        subtitle = "Financial phrase library is currently empty or not scanned."
                    )
                }
            }

            item("merchant") {
                ConsoleSection(title = "Merchant Intelligence") {
                    LedgerEmptyState(
                        title = "No merchant data available",
                        subtitle = "Merchant registry diagnostics will appear here."
                    )
                }
            }

            item("relationship") {
                ConsoleSection(title = "Relationship Engine") {
                    LedgerEmptyState(
                        title = "No relationship data available",
                        subtitle = "Entity linking and transfer detection traces."
                    )
                }
            }

            item("system") {
                ConsoleSection(title = "System Information") {
                    // System info is easy to populate even without "business" state
                    LedgerSurface {
                        DiagnosticKeyValue(label = "Platform", value = "Android")
                        LedgerHairline()
                        DiagnosticKeyValue(label = "Build Type", value = "Debug")
                        LedgerHairline()
                        DiagnosticKeyValue(label = "Theme", value = LedgerTheme.colors.themeType.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBackClick
        )
        Spacer(Modifier.width(LedgerSpacing.Small))
        Text(
            text = "Developer Console",
            style = LedgerTheme.typography.headlineMedium,
            color = LedgerTheme.colors.label
        )
    }
}

@Composable
private fun ConsoleSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        DeveloperSectionHeader(title = title)
        content()
    }
}

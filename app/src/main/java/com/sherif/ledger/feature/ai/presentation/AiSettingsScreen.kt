package com.sherif.ledger.feature.ai.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.component.LedgerButton
import com.sherif.ledger.core.designsystem.component.LedgerTextField
import com.sherif.ledger.core.designsystem.component.ledgerSurface
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerSurfaceLevel
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.ai.audit.AiAuditEntry
import com.sherif.ledger.feature.ai.domain.AICapability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RC5 Part 8 — Provider Settings, permanently reachable (Profile → "AI Settings"),
 * not a Developer Console debug tool. Enable AI defaults OFF; nothing on
 * this screen, including "Test Connection," runs unless the user turns it
 * on first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            com.sherif.ledger.core.designsystem.component.LedgerScreenHeader(
                title = "AI Settings",
                onBackClick = onBackClick,
                modifier = Modifier.padding(horizontal = LedgerSpacing.ScreenPadding),
            )
        },
        containerColor = LedgerTheme.colors.surfaceLevel0,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = LedgerSpacing.Screen),
            verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Medium),
        ) {
            item { Spacer(Modifier.height(LedgerSpacing.Medium)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Enable AI", style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
                        Text(
                            "Off by default. No provider is called anywhere in the app until this is on.",
                            style = LedgerTextStyles.Caption,
                            color = LedgerTheme.colors.tertiaryLabel,
                        )
                    }
                    Switch(
                        checked = state.isAiEnabled,
                        onCheckedChange = viewModel::setAiEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = LedgerTheme.colors.tint),
                    )
                }
            }

            item { SectionTitle("Privacy") }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
                ) {
                    Text(
                        "Only the minimum data a task needs is sent — merchant classification never sees your account balances; " +
                            "duplicate detection never sees your full history. Local providers (Ollama, LM Studio) keep everything " +
                            "on your device or network. API keys are stored encrypted (Android Keystore) and never leave this device " +
                            "except as the Authorization header of a request you triggered.",
                        style = LedgerTextStyles.Caption,
                        color = LedgerTheme.colors.textSecondary,
                    )
                }
            }

            item { SectionTitle("Model Parameters") }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
                ) {
                    Text("Temperature: ${"%.1f".format(state.temperature)}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary)
                    Slider(
                        value = state.temperature.toFloat(),
                        onValueChange = { viewModel.setTemperature(it.toDouble()) },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(thumbColor = LedgerTheme.colors.tint, activeTrackColor = LedgerTheme.colors.tint),
                    )
                    Spacer(Modifier.height(LedgerSpacing.Small))
                    Text("Max Tokens: ${state.maxTokens}", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.textSecondary)
                    Slider(
                        value = state.maxTokens.toFloat(),
                        onValueChange = { viewModel.setMaxTokens(it.toInt()) },
                        valueRange = 16f..2048f,
                        colors = SliderDefaults.colors(thumbColor = LedgerTheme.colors.tint, activeTrackColor = LedgerTheme.colors.tint),
                    )
                }
            }

            item { SectionTitle("Capabilities") }
            items(state.capabilitySettings) { setting ->
                CapabilityRow(
                    setting = setting,
                    providers = state.providers,
                    onProviderSelected = { p -> viewModel.selectProvider(setting.capability, p) },
                    onFallbackProviderSelected = { p -> viewModel.selectFallbackProvider(setting.capability, p) },
                    onModelSelected = { m -> viewModel.selectModel(setting.capability, m) },
                    onConfidenceThresholdChanged = { t -> viewModel.setConfidenceThreshold(setting.capability, t) },
                )
            }

            item { SectionTitle("Providers") }
            items(state.providers) { provider ->
                ProviderRow(
                    provider = provider,
                    testResult = state.testResultByProvider[provider.id],
                    isTesting = state.testingProviderId == provider.id,
                    onApiKeyChange = { key -> viewModel.setApiKey(provider.id, key) },
                    onBaseUrlChange = { url -> viewModel.setBaseUrl(provider.id, url) },
                    onTestConnection = { viewModel.testConnection(provider.id) },
                )
            }

            item { SectionTitle("Cost Tracker (Today)") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CostStat("Requests", state.costSummary.requestCount.toString())
                    CostStat("Estimated Cost", "$" + "%.4f".format(state.costSummary.estimatedCostUsd))
                    CostStat("Avg Latency", "${state.costSummary.averageLatencyMs}ms")
                }
            }

            item { SectionTitle("Audit Log") }
            if (state.auditLog.isEmpty()) {
                item {
                    Text("No AI calls yet.", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
                }
            } else {
                items(state.auditLog) { entry -> AuditLogRow(entry) }
            }

            item { Spacer(Modifier.height(LedgerSpacing.XxLarge)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = LedgerTextStyles.Caption.copy(fontWeight = FontWeight.Bold),
        color = LedgerTheme.colors.tertiaryLabel,
        modifier = Modifier.padding(vertical = LedgerSpacing.Tiny),
    )
}

@Composable
private fun CostStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        Text(label, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
    }
}

@Composable
private fun CapabilityRow(
    setting: CapabilitySettingUi,
    providers: List<ProviderUi>,
    onProviderSelected: (String) -> Unit,
    onFallbackProviderSelected: (String?) -> Unit,
    onModelSelected: (String) -> Unit,
    onConfidenceThresholdChanged: (Int) -> Unit,
) {
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var fallbackMenuExpanded by remember { mutableStateOf(false) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val selectedProvider = providers.find { it.id == setting.selectedProviderId }
    val fallbackProvider = providers.find { it.id == setting.fallbackProviderId }

    Column(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
    ) {
        Text(setting.capability.displayName, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedProvider?.displayName ?: "Select provider",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .ledgerSurface(level = LedgerSurfaceLevel.Level2)
                        .clickable { providerMenuExpanded = true }
                        .padding(LedgerSpacing.Small),
                )
                DropdownMenu(expanded = providerMenuExpanded, onDismissRequest = { providerMenuExpanded = false }) {
                    providers.forEach { provider ->
                        DropdownMenuItem(text = { Text(provider.displayName) }, onClick = { onProviderSelected(provider.id); providerMenuExpanded = false })
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = setting.selectedModel ?: "Select model",
                    style = LedgerTextStyles.Caption,
                    color = LedgerTheme.colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .ledgerSurface(level = LedgerSurfaceLevel.Level2)
                        .clickable(enabled = selectedProvider != null) { modelMenuExpanded = true }
                        .padding(LedgerSpacing.Small),
                )
                DropdownMenu(expanded = modelMenuExpanded, onDismissRequest = { modelMenuExpanded = false }) {
                    selectedProvider?.knownModels?.forEach { model ->
                        DropdownMenuItem(text = { Text(model) }, onClick = { onModelSelected(model); modelMenuExpanded = false })
                    }
                }
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Small))
        Text("Fallback provider (tried only if the primary fails)", style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
        Spacer(Modifier.height(LedgerSpacing.Tiny))
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = fallbackProvider?.displayName ?: "None",
                style = LedgerTextStyles.Caption,
                color = LedgerTheme.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .ledgerSurface(level = LedgerSurfaceLevel.Level2)
                    .clickable { fallbackMenuExpanded = true }
                    .padding(LedgerSpacing.Small),
            )
            DropdownMenu(expanded = fallbackMenuExpanded, onDismissRequest = { fallbackMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("None") }, onClick = { onFallbackProviderSelected(null); fallbackMenuExpanded = false })
                providers.filter { it.id != setting.selectedProviderId }.forEach { provider ->
                    DropdownMenuItem(text = { Text(provider.displayName) }, onClick = { onFallbackProviderSelected(provider.id); fallbackMenuExpanded = false })
                }
            }
        }

        Spacer(Modifier.height(LedgerSpacing.Small))
        Text(
            "Confidence threshold: ${setting.confidenceThreshold}% (AI is only consulted below this)",
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.tertiaryLabel,
        )
        Slider(
            value = setting.confidenceThreshold.toFloat(),
            onValueChange = { onConfidenceThresholdChanged(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = LedgerTheme.colors.tint, activeTrackColor = LedgerTheme.colors.tint),
        )
    }
}

@Composable
private fun ProviderRow(
    provider: ProviderUi,
    testResult: String?,
    isTesting: Boolean,
    onApiKeyChange: (String) -> Unit,
    onBaseUrlChange: (String) -> Unit,
    onTestConnection: () -> Unit,
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Medium),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(provider.displayName, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
            Text(
                if (provider.requiresApiKey) (if (provider.hasApiKey) "Key configured" else "No key") else "No key required",
                style = LedgerTextStyles.Caption,
                color = if (provider.hasApiKey || !provider.requiresApiKey) LedgerTheme.colors.positive else LedgerTheme.colors.tertiaryLabel,
            )
        }
        Spacer(Modifier.height(LedgerSpacing.Small))

        if (provider.requiresApiKey) {
            LedgerTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it; onApiKeyChange(it) },
                placeholder = "API key",
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
        }

        if (provider.baseUrlConfigurable) {
            LedgerTextField(
                value = baseUrlInput,
                onValueChange = { baseUrlInput = it; onBaseUrlChange(it) },
                placeholder = "Server address (e.g. http://192.168.1.10:11434/v1)",
            )
            Spacer(Modifier.height(LedgerSpacing.Small))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            LedgerButton(
                text = if (isTesting) "Testing..." else "Test Connection",
                onClick = onTestConnection,
                enabled = !isTesting,
            )
            if (testResult != null) {
                Text(
                    testResult,
                    style = LedgerTextStyles.Caption,
                    color = if (testResult.startsWith("Connected")) LedgerTheme.colors.positive else LedgerTheme.colors.negative,
                )
            }
        }
    }
}

@Composable
private fun AuditLogRow(entry: AiAuditEntry) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth().ledgerSurface(level = LedgerSurfaceLevel.Level1).padding(LedgerSpacing.Small),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeFormatter.format(Date(entry.timestampMillis)), style = LedgerTextStyles.Caption, color = LedgerTheme.colors.tertiaryLabel)
            Text(
                if (entry.success) "Success" else "Failed",
                style = LedgerTextStyles.Caption,
                color = if (entry.success) LedgerTheme.colors.positive else LedgerTheme.colors.negative,
            )
        }
        Text(entry.capability.displayName, style = LedgerTextStyles.Label, color = LedgerTheme.colors.textPrimary)
        Text(
            "${entry.providerId} · ${entry.model} · ${entry.latencyMs}ms" +
                (entry.tokensUsed?.let { " · ${it} tokens" } ?: "") +
                (entry.confidencePercent?.let { " · ${it}% confidence" } ?: ""),
            style = LedgerTextStyles.Caption,
            color = LedgerTheme.colors.secondaryLabel,
        )
        entry.errorSummary?.let {
            Text(it, style = LedgerTextStyles.Caption, color = LedgerTheme.colors.negative)
        }
    }
}

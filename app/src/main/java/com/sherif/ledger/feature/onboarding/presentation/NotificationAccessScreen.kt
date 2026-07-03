package com.sherif.ledger.feature.onboarding.presentation

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sherif.ledger.core.common.util.DiagnosticUtils
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTextStyles
import com.sherif.ledger.core.designsystem.theme.LedgerTheme

@Composable
fun NotificationAccessScreen() {
    val context = LocalContext.current
    val diagnostics = remember { DiagnosticUtils.getIngestionDiagnostics(context) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Large)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(LedgerSpacing.Massive))

        Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = LedgerTheme.colors.success.copy(alpha = 0.6f),
            modifier = Modifier.size(84.dp)
        )

        Spacer(Modifier.height(LedgerSpacing.XxLarge))

        Text(
            text = "Automate your ledger",
            style = LedgerTextStyles.Section.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(LedgerSpacing.Medium))

        Text(
            text = "Ledger automatically detects transactions from your bank notifications. This requires 'Notification Access' in Android settings.",
            style = LedgerTextStyles.Caption.copy(lineHeight = 22.sp),
            color = LedgerTheme.colors.secondaryLabel,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(LedgerSpacing.XxxLarge))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LedgerTheme.colors.success,
                contentColor = LedgerTheme.colors.onTint
            ),
            shape = LedgerTheme.radius.Large
        ) {
            Text(
                text = "Enable in Settings",
                style = LedgerTextStyles.Label.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(LedgerSpacing.Massive))

        // Diagnostic Section
        DiagnosticSection(diagnostics) {
            DiagnosticUtils.requestRebind(context)
        }
        
        Spacer(Modifier.height(LedgerSpacing.Massive))
    }
}

@Composable
private fun DiagnosticSection(
    diagnostics: com.sherif.ledger.core.common.util.IngestionDiagnostics,
    onRequestRebind: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), shape = LedgerTheme.radius.Medium)
            .padding(LedgerSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(LedgerSpacing.Small)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(LedgerSpacing.Small))
            Text("PIPELINE DIAGNOSTICS", style = LedgerTextStyles.Caption, color = Color.Gray)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        DiagnosticRow("Package", diagnostics.packageName)
        DiagnosticRow("Component", diagnostics.listenerComponentName)
        DiagnosticRow("PM Resolved", if (diagnostics.canPackageManagerResolveService) "YES" else "NO", if (diagnostics.canPackageManagerResolveService) LedgerTheme.colors.success else LedgerTheme.colors.expense)
        DiagnosticRow("PM Details", diagnostics.pmResolutionDetails)
        DiagnosticRow("Access Granted", if (diagnostics.isPackageEnabled) "YES" else "NO", if (diagnostics.isPackageEnabled) LedgerTheme.colors.success else LedgerTheme.colors.expense)
        
        Text(
            "Enabled Packages: ${diagnostics.enabledListeners.joinToString(", ").ifEmpty { "NONE" }}",
            style = LedgerTextStyles.Caption.copy(fontSize = 10.sp),
            color = Color.White.copy(alpha = 0.3f)
        )

        Spacer(Modifier.height(LedgerSpacing.Small))

        Button(
            onClick = onRequestRebind,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = LedgerTheme.radius.Small
        ) {
            Text("Request Rebind", style = LedgerTextStyles.Caption)
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color = Color.White.copy(alpha = 0.7f)) {
    Column {
        Text(label, style = LedgerTextStyles.Caption.copy(fontSize = 10.sp), color = Color.Gray)
        Text(value, style = LedgerTextStyles.Caption, color = valueColor)
    }
}

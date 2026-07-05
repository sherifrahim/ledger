package com.sherif.ledger.feature.onboarding.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sherif.ledger.core.designsystem.theme.LedgerSpacing
import com.sherif.ledger.core.designsystem.theme.LedgerTheme
import com.sherif.ledger.feature.onboarding.presentation.viewmodel.SmsOnboardingViewModel

@Composable
fun SmsOnboardingScreen(
    onComplete: () -> Unit,
    viewModel: SmsOnboardingViewModel = hiltViewModel()
) {
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startImport()
        } else {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LedgerTheme.colors.surfaceLevel0)
            .padding(LedgerSpacing.Screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Sms,
            contentDescription = null,
            tint = LedgerTheme.colors.tint,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(Modifier.height(LedgerSpacing.Large))
        
        Text(
            text = "Import Transaction History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = LedgerTheme.colors.label,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(LedgerSpacing.Medium))
        
        Text(
            text = "Ledger can scan your existing bank SMS alerts to build your financial history instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = LedgerTheme.colors.secondaryLabel,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(LedgerSpacing.Massive))

        when {
            isImporting -> {
                CircularProgressIndicator(color = LedgerTheme.colors.tint)
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Text("Scanning inbox...", color = LedgerTheme.colors.secondaryLabel)
            }
            importResult != null -> {
                val message = when (importResult) {
                    -1 -> "Import failed. You can try again later from settings."
                    0 -> "No transaction SMS found in your inbox."
                    else -> "Imported $importResult message(s) from your inbox."
                }
                Text(
                    text = message,
                    color = LedgerTheme.colors.label,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(LedgerSpacing.Medium))
                Button(
                    onClick = { onComplete() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.tint)
                ) {
                    Text("Continue")
                }
            }
            else -> {
                Button(
                    onClick = { launcher.launch(android.Manifest.permission.READ_SMS) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LedgerTheme.colors.tint)
                ) {
                    Text("Scan Inbox")
                }

                TextButton(
                    onClick = { viewModel.skipImport(onComplete) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now", color = LedgerTheme.colors.tertiaryLabel)
                }
            }
        }
    }
}

@Composable
fun rememberCoroutineOf() = rememberCoroutineScope()

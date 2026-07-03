package com.sherif.ledger.core.common.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import com.sherif.ledger.feature.capture.notification.LedgerNotificationListener

data class IngestionDiagnostics(
    val packageName: String,
    val listenerComponentName: String,
    val enabledListeners: List<String>,
    val isPackageEnabled: Boolean,
    val canPackageManagerResolveService: Boolean,
    val pmResolutionDetails: String,
    val lastResolutionError: String? = null,
)

object DiagnosticUtils {
    
    fun getIngestionDiagnostics(context: Context): IngestionDiagnostics {
        val packageName = context.packageName
        val componentName = ComponentName(context, LedgerNotificationListener::class.java)
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context).toList()
        
        val intent = Intent(NotificationListenerService.SERVICE_INTERFACE).apply {
            component = componentName
        }
        
        val resolveInfo = context.packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
        val canResolve = resolveInfo.isNotEmpty()
        
        val resolutionDetails = if (canResolve) {
            "Resolved ${resolveInfo.size} service(s). First: ${resolveInfo[0].serviceInfo.name}"
        } else {
            "PackageManager returned 0 results for the intent."
        }

        return IngestionDiagnostics(
            packageName = packageName,
            listenerComponentName = componentName.flattenToString(),
            enabledListeners = enabledListeners,
            isPackageEnabled = enabledListeners.contains(packageName),
            canPackageManagerResolveService = canResolve,
            pmResolutionDetails = resolutionDetails
        )
    }

    fun requestRebind(context: Context) {
        val componentName = ComponentName(context, LedgerNotificationListener::class.java)
        NotificationListenerService.requestRebind(componentName)
    }
}

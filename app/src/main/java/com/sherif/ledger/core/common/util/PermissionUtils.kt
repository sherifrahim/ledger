package com.sherif.ledger.core.common.util

import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object PermissionUtils {
    /**
     * Checks if the Notification Listener Service is enabled for this app.
     */
    fun isNotificationServiceEnabled(context: Context): Boolean {
        val packageNames = NotificationManagerCompat.getEnabledListenerPackages(context)
        return packageNames.contains(context.packageName)
    }
}

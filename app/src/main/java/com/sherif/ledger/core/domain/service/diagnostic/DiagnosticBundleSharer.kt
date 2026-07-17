package com.sherif.ledger.core.domain.service.diagnostic

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/** Builds the share-sheet intent for a generated bundle — kept separate from
 *  [DiagnosticBundleGenerator] so bundle *generation* (pure file I/O, testable
 *  without Android's Intent system) stays independent of bundle *sharing*
 *  (inherently Android-framework-specific). */
class DiagnosticBundleSharer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun buildShareIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.diagnosticfileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share Ledger diagnostic bundle").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}




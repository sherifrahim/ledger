package com.sherif.ledger.core.domain.service.diagnostic

import android.content.Context
import com.sherif.ledger.core.common.logging.LedgerLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

/**
 * Runs every bound [DiagnosticCollector] and packages the results into one
 * zip — `ledger_diagnostic_YYYYMMDD_HHMM.zip`, per RC4's spec exactly. Adding
 * a new diagnostic later means adding one new collector to the Hilt
 * multibinding set (see DiagnosticCollectorModule); this class never changes.
 *
 * One collector failing does not fail the whole bundle — its section is
 * replaced with a small error note and every other collector still runs.
 * A diagnostic tool that can be taken down by the exact kind of bug it
 * exists to find would defeat its own purpose.
 */
class DiagnosticBundleGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collectors: Set<@JvmSuppressWildcards DiagnosticCollector>,
) {
    companion object {
        private val FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
    }

    suspend fun generateBundle(): File {
        val timestamp = FILENAME_FORMATTER.format(Instant.now().atZone(ZoneId.systemDefault()))
        val outputDir = File(context.cacheDir, "diagnostics").apply { mkdirs() }
        val zipFile = File(outputDir, "ledger_diagnostic_$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            collectors.forEach { collector ->
                val section = try {
                    collector.collect()
                } catch (e: Exception) {
                    LedgerLogger.e("DiagnosticBundleGenerator: collector '${collector.id}' failed", e)
                    DiagnosticSection.Json(collector.id, """{"error":"${e.javaClass.simpleName}: ${e.message}"}""")
                }
                val (entryName, content) = when (section) {
                    is DiagnosticSection.Json -> "${section.id}.json" to section.json
                    is DiagnosticSection.LogText -> "${section.id}.log" to section.logText
                }
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        LedgerLogger.d("DiagnosticBundleGenerator: wrote ${zipFile.name} (${collectors.size} sections)")
        return zipFile
    }
}




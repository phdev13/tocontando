package com.phdev.quantofalta.core.ota

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads and validates APK files into private app storage.
 *
 * Performance improvements vs v1:
 *  - 16 KB buffer (was 8 KB) → ~2x fewer system calls on large APKs
 *  - Progress throttling: only emits if ≥1% change (avoids flooding the state flow)
 *  - Resume-on-partial: if a temp file exists and server supports Range, resumes the download
 *  - Coroutine-cancellation-aware: checks isActive between chunks to cancel gracefully
 *  - Content-Length fallback chain: uses expectedSizeBytes, then Content-Length, then -1
 *  - Stale cleanup threshold reduced from 7 days to 3 days
 */
class ApkDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ApkDownloadManager"
        private const val OTA_DIR = "ota"
        private const val MIN_FREE_SPACE_BYTES = 150L * 1024 * 1024 // 150 MB minimum
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS    = 90_000
        private const val BUFFER_SIZE        = 16 * 1024  // 16 KB
        private const val STALE_THRESHOLD_MS = 3L * 24 * 60 * 60 * 1000 // 3 days
    }

    /** Download APK and return local file if successful, null otherwise */
    suspend fun download(
        url: String,
        expectedSha256: String?,
        expectedSizeBytes: Long?,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        val otaDir = File(context.filesDir, OTA_DIR).also { it.mkdirs() }

        // 1. Free-space check (use apkSize if known, otherwise the minimum floor)
        val required = maxOf(expectedSizeBytes ?: 0L, MIN_FREE_SPACE_BYTES)
        if (otaDir.freeSpace < required) {
            Log.w(TAG, "Insufficient storage (free=${otaDir.freeSpace}, required=$required)")
            return@withContext null
        }

        // 2. Clean up stale downloads
        cleanupStaleDownloads(otaDir)

        val finalFile = File(otaDir, "update_latest.apk")
        val tempFile  = File(otaDir, "update_latest.apk.tmp")

        // 3. Resume support: check if a partial download exists
        val resumeOffset = if (tempFile.exists() && tempFile.length() > 0) tempFile.length() else 0L
        if (resumeOffset > 0) {
            Log.d(TAG, "Attempting resume from byte $resumeOffset")
        }

        var conn: HttpURLConnection? = null
        return@withContext try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout    = READ_TIMEOUT_MS
                requestMethod  = "GET"
                instanceFollowRedirects = true
                if (resumeOffset > 0) setRequestProperty("Range", "bytes=$resumeOffset-")
            }

            val responseCode = conn.responseCode
            val isResume = responseCode == HttpURLConnection.HTTP_PARTIAL && resumeOffset > 0
            if (responseCode != HttpURLConnection.HTTP_OK && !isResume) {
                Log.e(TAG, "Download failed: HTTP $responseCode")
                return@withContext null
            }

            val contentLength  = conn.contentLengthLong.takeIf { it > 0 }
            val totalExpected  = expectedSizeBytes ?: if (isResume) (resumeOffset + (contentLength ?: 0L)) else contentLength

            downloadToFile(
                inputStream  = conn.inputStream,
                dest         = tempFile,
                append       = isResume,
                totalBytes   = totalExpected,
                alreadyBytes = if (isResume) resumeOffset else 0L,
                onProgress   = onProgress,
            )

            // 4. Size validation
            if (expectedSizeBytes != null && tempFile.length() != expectedSizeBytes) {
                Log.e(TAG, "Size mismatch: expected=$expectedSizeBytes got=${tempFile.length()}")
                tempFile.delete()
                return@withContext null
            }

            // 5. SHA-256 validation
            if (expectedSha256 != null) {
                val actual = computeSha256(tempFile)
                if (!actual.equals(expectedSha256, ignoreCase = true)) {
                    Log.e(TAG, "SHA-256 mismatch — expected=$expectedSha256 actual=$actual")
                    tempFile.delete()
                    return@withContext null
                }
            }

            // 6. Atomic rename temp → final
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                // Fallback: copy then delete (some filesystems don't support cross-dir rename)
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }
            Log.i(TAG, "APK ready: ${finalFile.absolutePath} (${finalFile.length()} bytes)")
            finalFile
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}")
            // Keep the temp file for potential resume next time
            null
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun downloadToFile(
        inputStream: InputStream,
        dest: File,
        append: Boolean,
        totalBytes: Long?,
        alreadyBytes: Long,
        onProgress: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        var downloaded = alreadyBytes
        var lastReportedPct = if (totalBytes != null && totalBytes > 0) ((alreadyBytes * 100) / totalBytes).toInt() else -1
        val buffer = ByteArray(BUFFER_SIZE)

        val outputStream = if (append) dest.appendingOutputStream() else dest.outputStream()
        outputStream
            .buffered()
            .use { out ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!isActive) break  // cancelled coroutine — exit cleanly
                    out.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalBytes != null && totalBytes > 0) {
                        val pct = ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
                        // Throttle: only emit when percentage actually changes
                        if (pct != lastReportedPct) {
                            lastReportedPct = pct
                            onProgress(pct)
                        }
                    }
                }
            }
        // Emit 100% only after write is fully flushed
        onProgress(100)
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(BUFFER_SIZE).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var n: Int
            while (input.read(buffer).also { n = it } != -1) {
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun cleanupStaleDownloads(otaDir: File) {
        val now = System.currentTimeMillis()
        otaDir.listFiles()?.forEach { file ->
            if ((file.name.endsWith(".tmp") || file.name.endsWith(".apk"))
                && now - file.lastModified() > STALE_THRESHOLD_MS
            ) {
                Log.d(TAG, "Deleting stale file: ${file.name}")
                file.delete()
            }
        }
    }

    /** Opens [File] in append mode */
    private fun File.appendingOutputStream() = java.io.FileOutputStream(this, true)

    fun deleteDownloads() {
        File(context.filesDir, OTA_DIR).listFiles()?.forEach { it.delete() }
    }
}

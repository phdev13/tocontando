package com.phdev.quantofalta.core.ota

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads and validates APK files into private app storage.
 * Never saves to external/shared storage.
 * Validates SHA-256 before accepting the file.
 */
class ApkDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "ApkDownloadManager"
        private const val OTA_DIR = "ota"
        private const val MIN_FREE_SPACE_BYTES = 200L * 1024 * 1024 // 200MB minimum
    }

    /** Download APK and return local file path if successful, null otherwise */
    suspend fun download(
        url: String,
        expectedSha256: String?,
        expectedSizeBytes: Long?,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        // 1. Check free space
        val otaDir = File(context.filesDir, OTA_DIR).also { it.mkdirs() }
        if (otaDir.freeSpace < MIN_FREE_SPACE_BYTES) {
            Log.w(TAG, "Insufficient storage for OTA download")
            return@withContext null
        }

        // 2. Clean up stale downloads first
        cleanupStaleDownloads(otaDir)

        val tempFile = File(otaDir, "update_${System.currentTimeMillis()}.apk.tmp")
        val finalFile = File(otaDir, "update_latest.apk")

        var conn: HttpURLConnection? = null
        return@withContext try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 60_000
                requestMethod = "GET"
                instanceFollowRedirects = true
            }

            if (conn.responseCode != 200) {
                Log.e(TAG, "Download failed: HTTP ${conn.responseCode}")
                return@withContext null
            }

            val contentLength = expectedSizeBytes ?: conn.contentLengthLong.takeIf { it > 0 }
            downloadToFile(conn.inputStream, tempFile, contentLength, onProgress)

            // 3. Validate size
            if (expectedSizeBytes != null && tempFile.length() != expectedSizeBytes) {
                Log.e(TAG, "Size mismatch: expected $expectedSizeBytes, got ${tempFile.length()}")
                tempFile.delete()
                return@withContext null
            }

            // 4. Validate SHA-256
            if (expectedSha256 != null) {
                val actualHash = computeSha256(tempFile)
                if (!actualHash.equals(expectedSha256, ignoreCase = true)) {
                    Log.e(TAG, "SHA-256 mismatch — rejecting file")
                    tempFile.delete()
                    return@withContext null
                }
            }

            // 5. Move temp → final
            if (finalFile.exists()) finalFile.delete()
            tempFile.renameTo(finalFile)
            Log.i(TAG, "APK downloaded and validated: ${finalFile.absolutePath}")
            finalFile
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}")
            tempFile.delete()
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun downloadToFile(
        inputStream: InputStream,
        dest: File,
        totalBytes: Long?,
        onProgress: (Int) -> Unit,
    ) {
        var downloaded = 0L
        val buffer = ByteArray(8192)
        dest.outputStream().buffered().use { out ->
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
                downloaded += bytesRead
                if (totalBytes != null && totalBytes > 0) {
                    onProgress(((downloaded * 100) / totalBytes).toInt().coerceIn(0, 100))
                }
            }
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun cleanupStaleDownloads(otaDir: File) {
        otaDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".tmp") || file.name.endsWith(".apk")) {
                if (System.currentTimeMillis() - file.lastModified() > 7 * 24 * 60 * 60 * 1000L) {
                    file.delete()
                }
            }
        }
    }

    fun deleteDownloads() {
        File(context.filesDir, OTA_DIR).listFiles()?.forEach { it.delete() }
    }
}

package com.phdev.quantofalta.core.ota

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Lightweight OTA API client using only java.net (no OkHttp dependency required).
 * Validates responses before trusting them.
 */
class OtaApiClient(private val context: Context) {

    companion object {
        // Base URL injected at build time via BuildConfig (set via .env / CI)
        // Default points to the Cloudflare Worker
        private val BASE_URL: String
            get() = try {
                BuildConfig::class.java.getField("API_BASE_URL").get(null) as String
            } catch (_: Exception) {
                BuildConfig.API_BASE_URL
            }

        private const val TIMEOUT_MS = 10_000
        private const val TAG = "OtaApiClient"
    }

    suspend fun checkForUpdate(
        installationId: String,
        releaseChannel: String = "stable",
    ): OtaUpdateInfo? = withContext(Dispatchers.IO) {
        if (com.phdev.quantofalta.BuildConfig.FLAVOR == "playStore") return@withContext null

        val versionCode = BuildConfig.VERSION_CODE
        val versionName = BuildConfig.VERSION_NAME
        val packageName = context.packageName
        val androidVersion = android.os.Build.VERSION.RELEASE
        val arch = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        val params = buildString {
            append("versionCode=").append(versionCode)
            append("&versionName=").append(URLEncoder.encode(versionName, "UTF-8"))
            append("&packageName=").append(URLEncoder.encode(packageName, "UTF-8"))
            append("&releaseChannel=").append(URLEncoder.encode(releaseChannel, "UTF-8"))
            append("&androidVersion=").append(URLEncoder.encode(androidVersion, "UTF-8"))
            append("&architecture=").append(URLEncoder.encode(arch, "UTF-8"))
            append("&installationId=").append(URLEncoder.encode(installationId, "UTF-8"))
        }

        val url = URL("$BASE_URL/api/v1/app/ota/check?$params")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != 200) {
                Log.w(TAG, "OTA check returned ${conn.responseCode}")
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().readText()
            parseUpdateResponse(body)
        } catch (e: Exception) {
            Log.w(TAG, "OTA check failed: ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun parseUpdateResponse(json: String): OtaUpdateInfo? {
        return try {
            val root = JSONObject(json)
            val obj = root.optJSONObject("data") ?: root
            if (!obj.optBoolean("updateAvailable", false)) return null

            val changelogArr = obj.optJSONArray("changelog")
            val changelog = buildList {
                if (changelogArr != null) {
                    for (i in 0 until changelogArr.length()) add(changelogArr.getString(i))
                }
            }

            val rawApkUrl = obj.optString("apkUrl", "")
            val apkUrl = if (rawApkUrl.startsWith("/")) "$BASE_URL$rawApkUrl" else rawApkUrl

            OtaUpdateInfo(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.getString("versionName"),
                title = obj.optString("title", "Nova atualização disponível"),
                summary = obj.optString("summary", ""),
                changelog = changelog,
                apkUrl = apkUrl,
                apkSizeBytes = if (obj.has("apkSize")) obj.getLong("apkSize") else null,
                sha256 = obj.optString("sha256").takeIf { it.isNotBlank() },
                mandatory = obj.optBoolean("mandatory", false),
                rolloutPercentage = obj.optInt("rolloutPercentage", 100),
                releaseChannel = obj.optString("releaseChannel", "stable"),
                publishedAt = obj.optString("publishedAt").takeIf { it.isNotBlank() },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OTA response: ${e.message}")
            null
        }
    }
}

package com.phdev.quantofalta.core.ota

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Cliente simplificado para busca de atualizações OTA via JSON estático.
 */
class OtaApiClient(private val context: Context) {

    companion object {
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
        releaseChannel: String = BuildConfig.OTA_RELEASE_CHANNEL,
    ): OtaUpdateInfo? = withContext(Dispatchers.IO) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        
        val url = URL(
            "$BASE_URL/api/v1/app/ota/check" +
                "?versionCode=$currentVersionCode" +
                "&versionName=${BuildConfig.VERSION_NAME.urlEncoded()}" +
                "&packageName=${BuildConfig.APPLICATION_ID.urlEncoded()}" +
                "&releaseChannel=${releaseChannel.urlEncoded()}" +
                "&androidVersion=${android.os.Build.VERSION.RELEASE.urlEncoded()}" +
                "&architecture=${android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty().urlEncoded()}" +
                "&installationId=${installationId.urlEncoded()}"
        )
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")

            if (conn.responseCode != 200) {
                Log.w(TAG, "OTA config not found at $url")
                return@withContext null
            }

            val body = conn.inputStream.bufferedReader().readText()
            val updateInfo = parseUpdateResponse(body)
            
            // Só ativa se o versionCode do servidor for estritamente maior
            if (updateInfo != null && updateInfo.versionCode > currentVersionCode) {
                Log.i(TAG, "New version found: ${updateInfo.versionCode} (Current: $currentVersionCode)")
                updateInfo
            } else {
                null
            }
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

            val rawApkUrl = obj.optString("apkUrl", "")
            val apkUrl = if (rawApkUrl.startsWith("/")) "$BASE_URL$rawApkUrl" else rawApkUrl

            OtaUpdateInfo(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.getString("versionName"),
                title = obj.optString("title", "Nova atualização disponível"),
                summary = obj.optString("summary", ""),
                changelog = parseChangelog(obj),
                apkUrl = apkUrl,
                apkSizeBytes = obj.optLong("apkSizeBytes", 0L).takeIf { it > 0 },
                sha256 = obj.optString("sha256").takeIf { it.isNotBlank() && it != "null" },
                mandatory = obj.optBoolean("mandatory", false),
                rolloutPercentage = obj.optInt("rolloutPercentage", 100),
                releaseChannel = obj.optString("releaseChannel", "stable"),
                publishedAt = obj.optString("publishedAt").takeIf { it.isNotBlank() && it != "null" },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse OTA response: ${e.message}")
            null
        }
    }

    private fun parseChangelog(obj: JSONObject): List<String> {
        val changelogArr = obj.optJSONArray("changelog")
        if (changelogArr != null) {
            return buildList {
                for (i in 0 until changelogArr.length()) {
                    val line = changelogArr.optString(i).trim()
                    if (line.isNotBlank()) add(line)
                }
            }
        }
        return emptyList()
    }

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}

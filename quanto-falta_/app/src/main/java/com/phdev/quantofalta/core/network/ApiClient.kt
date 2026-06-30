package com.phdev.quantofalta.core.network

import android.util.Log
import com.phdev.quantofalta.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Centralised HTTP client for all API calls.
 * - Manages the base URL in one place
 * - Enforces connection + read timeouts so no call hangs forever
 * - Safely reads both success and error streams
 */
object ApiClient {

    private const val TAG = "ApiClient"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS    = 15_000

    val baseUrl: String
        get() = try {
            BuildConfig::class.java.getField("API_BASE_URL").get(null) as String
        } catch (_: Exception) {
            BuildConfig.API_BASE_URL
        }

    data class Response(val statusCode: Int, val body: String) {
        /** Convenience to check if we got a 2xx back. */
        fun isSuccess() = statusCode in 200..299
    }
    /**
     * POST a JSON payload and return the raw HTTP response.
     * Always returns a [Response]; throws [IOException] only for network failures.
     */
    suspend fun post(path: String, payload: JSONObject, headers: Map<String, String> = emptyMap()): Response = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout    = READ_TIMEOUT_MS
            conn.requestMethod  = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) ->
                conn.setRequestProperty(key, value)
            }
            conn.doOutput = true

            val bytes = payload.toString().toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(bytes) }

            val status = conn.responseCode
            // errorStream can be null on some Android versions / network configs
            val stream = if (status >= 400) (conn.errorStream ?: conn.inputStream) else conn.inputStream
            val body   = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            Log.d(TAG, "POST $path -> $status")
            Response(status, body)
        } catch (e: IOException) {
            Log.w(TAG, "POST $path failed: ${e.message}")
            throw e
        } finally {
            conn.disconnect()
        }
    }

    /**
     * GET from a path and return the raw HTTP response.
     */
    suspend fun get(path: String, headers: Map<String, String> = emptyMap()): Response = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout    = READ_TIMEOUT_MS
            conn.requestMethod  = "GET"
            conn.setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) ->
                conn.setRequestProperty(key, value)
            }

            val status = conn.responseCode
            val stream = if (status >= 400) (conn.errorStream ?: conn.inputStream) else conn.inputStream
            val body   = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }

            Log.d(TAG, "GET $path -> $status")
            Response(status, body)
        } catch (e: IOException) {
            Log.w(TAG, "GET $path failed: ${e.message}")
            throw e
        } finally {
            conn.disconnect()
        }
    }


    fun unwrapDataObject(body: String): JSONObject {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return JSONObject()
        return root.optJSONObject("data") ?: root
    }

    fun errorMessage(body: String, fallback: String): String {
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return fallback
        val error = root.optJSONObject("error")
        return error?.optString("message")?.takeIf { it.isNotBlank() }
            ?: root.optString("error").takeIf { it.isNotBlank() }
            ?: fallback
    }
}

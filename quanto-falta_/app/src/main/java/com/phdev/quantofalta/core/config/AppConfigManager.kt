package com.phdev.quantofalta.core.config

import android.content.Context
import android.util.Log
import com.phdev.quantofalta.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AppConfigManager {
    private const val TAG = "AppConfigManager"
    private const val PREFS_NAME = "AppConfigPrefs"
    private const val KEY_PREMIUM_CARDS = "premium_event_cards_enabled"

    fun isPremiumCardsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PREMIUM_CARDS, true) // Default is true!
    }

    suspend fun syncWithServer(context: Context) = withContext(Dispatchers.IO) {
        try {
            val url = URL("${BuildConfig.API_BASE_URL}/api/v1/public/app-config")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().readText()
                val root = JSONObject(json)
                val obj = root.optJSONObject("data") ?: root
                
                val premiumCardsEnabled = obj.optBoolean("premiumEventCardsEnabled", true)
                
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_PREMIUM_CARDS, premiumCardsEnabled).apply()
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync app config: ${e.message}")
        }
    }
}

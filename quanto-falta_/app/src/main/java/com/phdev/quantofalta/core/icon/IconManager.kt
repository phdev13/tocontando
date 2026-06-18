package com.phdev.quantofalta.core.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.phdev.quantofalta.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object IconManager {
    private const val TAG = "IconManager"

    // Component names match the aliases in AndroidManifest.xml
    private const val ALIAS_COPA = "com.phdev.quantofalta.MainActivityAliasCopa"
    private const val ALIAS_PADRAO = "com.phdev.quantofalta.MainActivityAliasPadrao"

    suspend fun syncIconWithServer(context: Context) = withContext(Dispatchers.IO) {
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
                val mode = obj.optString("activeIconMode", "auto")
                applyIconMode(context, mode)
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync icon config: ${e.message}")
            // Fallback to auto if network fails
            applyIconMode(context, "auto")
        }
    }

    private fun applyIconMode(context: Context, mode: String) {
        // User requested to always show standard logo
        val isCopa = false

        val pm = context.packageManager
        val componentCopa = ComponentName(context, ALIAS_COPA)
        val componentPadrao = ComponentName(context, ALIAS_PADRAO)

        val currentCopaState = pm.getComponentEnabledSetting(componentCopa)
        val currentPadraoState = pm.getComponentEnabledSetting(componentPadrao)

        val targetCopaState = if (isCopa) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val targetPadraoState = if (!isCopa) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

        // Only change if needed to avoid restarting the launcher unnecessarily
        if (currentCopaState != targetCopaState || currentPadraoState != targetPadraoState) {
            Log.i(TAG, "Changing app icon to: ${if (isCopa) "Copa" else "Padrao"}")

            // Enable the new one first, then disable the old one (so there is always one enabled)
            if (isCopa) {
                pm.setComponentEnabledSetting(componentCopa, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(componentPadrao, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            } else {
                pm.setComponentEnabledSetting(componentPadrao, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(componentCopa, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
        }
    }
}

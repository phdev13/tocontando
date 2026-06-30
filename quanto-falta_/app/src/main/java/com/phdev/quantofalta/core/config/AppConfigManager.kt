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
    private const val KEY_EARLY_ACCESS = "early_access_config"
    private const val KEY_OTA_ENABLED = "ota_enabled"

    fun isPremiumCardsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PREMIUM_CARDS, true) // Default is true!
    }

    fun isOtaEnabled(context: Context): Boolean {
        if (!BuildConfig.OTA_UPDATES_SUPPORTED) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_OTA_ENABLED, true)
    }

    fun getEarlyAccessConfig(context: Context): EarlyAccessConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_EARLY_ACCESS, null) ?: return EarlyAccessConfig()
        return try {
            val obj = JSONObject(jsonStr)
            val featuresArray = obj.optJSONArray("features")
            val featuresList = mutableListOf<String>()
            if (featuresArray != null) {
                for (i in 0 until featuresArray.length()) {
                    featuresList.add(featuresArray.getString(i))
                }
            } else {
                featuresList.addAll(EarlyAccessConfig().features)
            }
            
            EarlyAccessConfig(
                active = obj.optBoolean("active", true),
                title = obj.optString("title", "Acesso Antecipado"),
                subtitle = obj.optString("subtitle", "Garanta recursos exclusivos antes do lançamento oficial na Play Store."),
                price = obj.optString("price", "R\$ 19,90"),
                description = obj.optString("description", "Ao participar do acesso antecipado, você ganha acesso vitalício a todos os recursos Premium."),
                features = featuresList,
                buttonText = obj.optString("buttonText", "Solicitar Atendimento"),
                instructions = obj.optString("instructions", "Abra um ticket para conversar com o desenvolvedor, enviar o comprovante de pagamento e receber seu token de ativação."),
                allowTicket = obj.optBoolean("allowTicket", true)
            )
        } catch (e: Exception) {
            EarlyAccessConfig()
        }
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
                val otaEnabled = when {
                    obj.has("otaEnabled") -> obj.optBoolean("otaEnabled", BuildConfig.OTA_UPDATES_SUPPORTED)
                    obj.optJSONObject("ota") != null -> {
                        obj.optJSONObject("ota")?.optBoolean("enabled", BuildConfig.OTA_UPDATES_SUPPORTED)
                            ?: BuildConfig.OTA_UPDATES_SUPPORTED
                    }
                    else -> BuildConfig.OTA_UPDATES_SUPPORTED
                }
                val earlyAccessObj = obj.optJSONObject("earlyAccess")
                
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean(KEY_PREMIUM_CARDS, premiumCardsEnabled)
                    .putBoolean(KEY_OTA_ENABLED, BuildConfig.OTA_UPDATES_SUPPORTED && otaEnabled)
                
                if (earlyAccessObj != null) {
                    editor.putString(KEY_EARLY_ACCESS, earlyAccessObj.toString())
                }
                
                editor.apply()
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync app config: ${e.message}")
        }
    }
}

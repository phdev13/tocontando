package com.phdev.quantofalta.billing

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import android.provider.Settings
import com.phdev.quantofalta.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
/**
 * Manages premium entitlements securely using EncryptedSharedPreferences.
 */
class EntitlementManager(context: Context) {

    private val ENTITLEMENTS_KEY = "entitlements_json"
    private val TAG = "EntitlementManager"

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secret_entitlements_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences", e)
            // Fallback to normal shared prefs if Keystore fails (e.g. broken keystore on some devices)
            context.getSharedPreferences("fallback_entitlements_prefs", Context.MODE_PRIVATE)
        }
    }

    // Since SharedPreferences doesn't have native Flow support like DataStore,
    // we use a MutableStateFlow to back it up and emit updates.
    private val _entitlementsFlow = MutableStateFlow<List<Entitlement>>(emptyList())

    init {
        // Load initial state
        _entitlementsFlow.value = loadFromPrefs()
    }

    // ── Public flows ───────────────────────────────────────────────────────

    /** All entitlements stored locally (may include expired ones). */
    val allEntitlements: Flow<List<Entitlement>> = _entitlementsFlow

    /**
     * Only entitlements that are currently valid (not expired).
     * Lifetime entitlements (expiresAt == null) are always active.
     */
    val activeEntitlements: Flow<List<Entitlement>> = _entitlementsFlow.map { list ->
        val nowSec = System.currentTimeMillis() / 1000
        list.filter { it.isActive(nowSec) }
    }

    /** True when there is at least one valid entitlement or active Google Play purchase. */
    val hasActivePremium: Flow<Boolean> = activeEntitlements.map { it.isNotEmpty() }

    /** The best active plan (VITALICIO > ANUAL > MENSAL > PERSONALIZADO > NONE). */
    val activePlan: Flow<PremiumPlan> = activeEntitlements.map { list ->
        if (list.isEmpty()) return@map PremiumPlan.NONE
        val plans = list.mapNotNull { PremiumPlan.fromString(it.planType).takeIf { p -> p != PremiumPlan.NONE } }
        when {
            PremiumPlan.VITALICIO  in plans -> PremiumPlan.VITALICIO
            PremiumPlan.ANUAL      in plans -> PremiumPlan.ANUAL
            PremiumPlan.MENSAL     in plans -> PremiumPlan.MENSAL
            PremiumPlan.PERSONALIZADO in plans -> PremiumPlan.PERSONALIZADO
            else -> PremiumPlan.NONE
        }
    }

    // ── Mutations ──────────────────────────────────────────────────────────

    suspend fun saveEntitlements(entitlements: List<Entitlement>) {
        saveToPrefs(entitlements)
    }

    /** Add a single entitlement, skipping if already present (same id). */
    suspend fun addEntitlement(entitlement: Entitlement) {
        val current = _entitlementsFlow.value.toMutableList()
        if (current.none { it.id == entitlement.id }) {
            current.add(entitlement)
            saveToPrefs(current)
        }
    }

    /** Remove all expired entitlements from local storage. */
    suspend fun pruneExpired() {
        val nowSec = System.currentTimeMillis() / 1000
        val valid = _entitlementsFlow.value.filter { it.isActive(nowSec) }
        saveToPrefs(valid)
    }

    /**
     * Immediately revokes all non-Google-Play entitlements from local storage.
     *
     * Should be called on:
     *  - User logout (to prevent premium state from leaking after session ends)
     *  - Account switch (before setting the new email + syncing with server)
     *
     * Google Play purchases (id starts with "play_store_") are preserved because
     * they are tied to the device, not the server account.
     */
    fun revokeNonPlayStoreEntitlements() {
        val current = _entitlementsFlow.value.toMutableList()
        val removed = current.removeAll { !it.id.startsWith("play_store_") }
        if (removed) {
            saveToPrefs(current)
            Log.d(TAG, "revokeNonPlayStoreEntitlements: local entitlements cleared")
        }
    }

    // ── Prefs Management ──────────────────────────────────────────────
    
    fun getSavedEmail(): String? {
        return sharedPreferences.getString("user_email", null)
    }

    fun setSavedEmail(email: String) {
        sharedPreferences.edit().putString("user_email", email).apply()
    }

    suspend fun syncWithServer(context: Context) = withContext(Dispatchers.IO) {
        try {
            val installationId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: return@withContext
            
            val email = getSavedEmail()
            
            val response = PremiumApi.getStatus(installationId, email)
            
            val nowSec = System.currentTimeMillis() / 1000
            
            if (response.success) {
                // Server successfully responded. It is the absolute source of truth.
                sharedPreferences.edit().putLong("last_successful_sync", nowSec).apply()
                
                if (response.isPremium) {
                    val entitlementId = response.id ?: "server_sync_$installationId"
                    
                    val entitlement = Entitlement(
                        id = entitlementId,
                        planType = response.planType,
                        features = response.features,
                        expiresAt = response.expiresAt
                    )
                    
                    addEntitlement(entitlement)
                } else {
                    // Revoke non-Google Play local entitlements if server actively says NO premium
                    val current = _entitlementsFlow.value.toMutableList()
                    val removed = current.removeAll { !it.id.startsWith("play_store_") }
                    if (removed) {
                        saveToPrefs(current)
                    }
                }
            } else {
                // Network error or server error.
                // Check TTL: if last successful sync is older than 7 days, revoke non-PlayStore cache.
                val lastSync = sharedPreferences.getLong("last_successful_sync", nowSec)
                val daysSinceSync = (nowSec - lastSync) / 86400
                if (daysSinceSync > 7) {
                    Log.w(TAG, "Offline TTL expired (> 7 days). Revoking unverified entitlements.")
                    val current = _entitlementsFlow.value.toMutableList()
                    val removed = current.removeAll { !it.id.startsWith("play_store_") }
                    if (removed) {
                        saveToPrefs(current)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync premium status: ${e.message}")
        }
    }

    private fun loadFromPrefs(): List<Entitlement> {
        val jsonString = sharedPreferences.getString(ENTITLEMENTS_KEY, "[]") ?: "[]"
        return parseEntitlements(jsonString)
    }

    private fun saveToPrefs(list: List<Entitlement>) {
        val jsonString = serialize(list)
        sharedPreferences.edit().putString(ENTITLEMENTS_KEY, jsonString).apply()
        _entitlementsFlow.value = list // Notify flow
    }

    // ── Serialisation helpers ──────────────────────────────────────────────

    private fun serialize(list: List<Entitlement>): String {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().apply {
                put("id",        e.id)
                put("planType",  e.planType ?: JSONObject.NULL)
                put("features",  e.features ?: JSONObject.NULL)
                put("expiresAt", e.expiresAt ?: -1L)
            })
        }
        return arr.toString()
    }

    private fun parseEntitlements(jsonString: String): List<Entitlement> {
        val list = mutableListOf<Entitlement>()
        return try {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val expiresAt = obj.optLong("expiresAt", -1L)
                list.add(
                    Entitlement(
                        id        = obj.getString("id"),
                        planType  = obj.optString("planType").takeIf { it.isNotBlank() && it != "null" },
                        features  = obj.optString("features").takeIf { it.isNotBlank() && it != "null" },
                        expiresAt = if (expiresAt == -1L) null else expiresAt,
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}

// ── Model ──────────────────────────────────────────────────────────────────

data class Entitlement(
    val id: String,
    val planType: String?,   // "MENSAL" | "ANUAL" | "VITALICIO" | "PERSONALIZADO"
    val features: String?,
    val expiresAt: Long?,    // Unix seconds; null = vitalício
) {
    /** Returns true if this entitlement hasn't expired yet. */
    fun isActive(nowSec: Long = System.currentTimeMillis() / 1000): Boolean =
        expiresAt == null || expiresAt > nowSec
}

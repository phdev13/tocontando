package com.phdev.quantofalta.core.analytics

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

private val Context.installIdStore: DataStore<Preferences> by preferencesDataStore(name = "installation_id")

/**
 * Manages a stable, anonymous installation ID.
 * Uses UUID.randomUUID() — no device identifiers.
 * Regenerated automatically if app data is cleared.
 */
object InstallationManager {

    private const val TAG = "InstallationManager"
    private val KEY_INSTALL_ID = stringPreferencesKey("installation_uuid")

    @Volatile private var cachedId: String? = null

    suspend fun getOrCreateId(context: Context): String {
        cachedId?.let { return it }
        val prefs = context.installIdStore.data.first()
        val existing = prefs[KEY_INSTALL_ID]
        if (existing != null) {
            cachedId = existing
            return existing
        }
        val newId = UUID.randomUUID().toString()
        context.installIdStore.edit { it[KEY_INSTALL_ID] = newId }
        cachedId = newId
        Log.d(TAG, "Installation ID created")
        return newId
    }

    /** Returns cached ID synchronously — may be null before first async call */
    fun getCachedIdOrNull(): String? = cachedId
}

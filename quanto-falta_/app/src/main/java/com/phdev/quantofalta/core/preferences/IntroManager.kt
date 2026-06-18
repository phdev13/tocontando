package com.phdev.quantofalta.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quanto_falta_settings")

class IntroManager(private val context: Context) {
    companion object {
        val INTRO_COMPLETED_KEY = booleanPreferencesKey("intro_completed")
        val SPONSOR_SEEN_KEY = booleanPreferencesKey("sponsor_seen")
    }

    val isIntroCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[INTRO_COMPLETED_KEY] ?: false
    }

    val isSponsorSeen: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SPONSOR_SEEN_KEY] ?: false
    }

    suspend fun setIntroCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INTRO_COMPLETED_KEY] = completed
        }
    }

    suspend fun setSponsorSeen(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SPONSOR_SEEN_KEY] = seen
        }
    }
}

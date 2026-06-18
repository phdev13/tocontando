package com.phdev.quantofalta.core.feedback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.smartFeedbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_feedback_prefs")

class SmartFeedbackManager(private val context: Context) {

    companion object {
        private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        private val EVENT_CREATED_COUNT = intPreferencesKey("event_created_count")
        private val HAS_SUBMITTED_FEEDBACK = booleanPreferencesKey("has_submitted_feedback")
        private val LAST_DISMISSED_TIME = longPreferencesKey("last_dismissed_time")

        // Regras de UX
        private const val SEVEN_DAYS_MILLIS = 7L * 24 * 60 * 60 * 1000
    }

    val shouldShowPrompt: Flow<Boolean> = context.smartFeedbackDataStore.data.map { prefs ->
        val hasSubmitted = prefs[HAS_SUBMITTED_FEEDBACK] ?: false
        if (hasSubmitted) return@map false

        val lastDismissed = prefs[LAST_DISMISSED_TIME] ?: 0L
        val now = System.currentTimeMillis()
        if (now - lastDismissed < SEVEN_DAYS_MILLIS) return@map false

        val openCount = prefs[APP_OPEN_COUNT] ?: 0
        val eventCount = prefs[EVENT_CREATED_COUNT] ?: 0

        // Dispara quando atinge múltiplos de 5 aberturas (a partir da 5ª) ou múltiplos de 2 eventos (a partir do 2º)
        val triggeredByOpens = openCount > 0 && openCount % 5 == 0
        val triggeredByEvents = eventCount > 0 && eventCount % 2 == 0

        triggeredByOpens || triggeredByEvents
    }

    suspend fun recordAppOpen() {
        context.smartFeedbackDataStore.edit { prefs ->
            val current = prefs[APP_OPEN_COUNT] ?: 0
            prefs[APP_OPEN_COUNT] = current + 1
        }
    }

    suspend fun recordEventCreated() {
        context.smartFeedbackDataStore.edit { prefs ->
            val current = prefs[EVENT_CREATED_COUNT] ?: 0
            prefs[EVENT_CREATED_COUNT] = current + 1
        }
    }

    suspend fun recordDismissed() {
        context.smartFeedbackDataStore.edit { prefs ->
            prefs[LAST_DISMISSED_TIME] = System.currentTimeMillis()
            // Incrementa os contadores ligeiramente para não ficar travado no múltiplo e disparar de novo logo após 7 dias se a pessoa não fizer mais nada
            prefs[APP_OPEN_COUNT] = (prefs[APP_OPEN_COUNT] ?: 0) + 1
        }
    }

    suspend fun recordSubmitted() {
        context.smartFeedbackDataStore.edit { prefs ->
            prefs[HAS_SUBMITTED_FEEDBACK] = true
        }
    }
}

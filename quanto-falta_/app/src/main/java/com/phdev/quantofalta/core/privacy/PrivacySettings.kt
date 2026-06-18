package com.phdev.quantofalta.core.privacy

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.privacyDataStore: DataStore<Preferences> by preferencesDataStore(name = "privacy_settings")

/**
 * User privacy preferences.
 * All analytics gates check these flows before sending.
 */
class PrivacySettings(private val context: Context) {

    companion object {
        private val KEY_USAGE_DATA = booleanPreferencesKey("share_usage_data")
        private val KEY_PERFORMANCE_DATA = booleanPreferencesKey("share_performance_data")
        private val KEY_ERROR_REPORTS = booleanPreferencesKey("share_error_reports")
    }

    /** True by default — user can disable in Settings */
    val shareUsageData: Flow<Boolean> = context.privacyDataStore.data.map { it[KEY_USAGE_DATA] ?: true }
    val sharePerformanceData: Flow<Boolean> = context.privacyDataStore.data.map { it[KEY_PERFORMANCE_DATA] ?: true }
    val shareErrorReports: Flow<Boolean> = context.privacyDataStore.data.map { it[KEY_ERROR_REPORTS] ?: true }

    suspend fun setShareUsageData(enabled: Boolean) {
        context.privacyDataStore.edit { it[KEY_USAGE_DATA] = enabled }
    }

    suspend fun setSharePerformanceData(enabled: Boolean) {
        context.privacyDataStore.edit { it[KEY_PERFORMANCE_DATA] = enabled }
    }

    suspend fun setShareErrorReports(enabled: Boolean) {
        context.privacyDataStore.edit { it[KEY_ERROR_REPORTS] = enabled }
    }
}

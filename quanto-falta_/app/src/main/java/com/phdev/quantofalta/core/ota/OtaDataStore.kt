package com.phdev.quantofalta.core.ota

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.otaDataStore: DataStore<Preferences> by preferencesDataStore(name = "ota_state")

/** Persists OTA-related state using DataStore. No personal data stored. */
class OtaDataStore(private val context: Context) {

    companion object {
        private val KEY_LAST_CHECK = longPreferencesKey("ota_last_check_ts")
        private val KEY_LAST_SHOWN = longPreferencesKey("ota_last_shown_ts")
        private val KEY_SHOW_COUNT = intPreferencesKey("ota_show_count")
        private val KEY_DEFER_COUNT = intPreferencesKey("ota_defer_count")
        private val KEY_INSTALLED_VERSION = intPreferencesKey("ota_installed_version_code")
        private val KEY_DOWNLOADED_VERSION = intPreferencesKey("ota_downloaded_version_code")
        private val KEY_DOWNLOADED_APK_PATH = stringPreferencesKey("ota_downloaded_apk_path")
        private val KEY_PENDING_VERSION_CODE = intPreferencesKey("ota_pending_version_code")
    }

    val lastCheckTimestamp: Flow<Long> = context.otaDataStore.data.map { it[KEY_LAST_CHECK] ?: 0L }
    val lastShownTimestamp: Flow<Long> = context.otaDataStore.data.map { it[KEY_LAST_SHOWN] ?: 0L }
    val showCount: Flow<Int> = context.otaDataStore.data.map { it[KEY_SHOW_COUNT] ?: 0 }
    val deferCount: Flow<Int> = context.otaDataStore.data.map { it[KEY_DEFER_COUNT] ?: 0 }
    val installedVersionCode: Flow<Int> = context.otaDataStore.data.map { it[KEY_INSTALLED_VERSION] ?: 0 }
    val downloadedVersionCode: Flow<Int> = context.otaDataStore.data.map { it[KEY_DOWNLOADED_VERSION] ?: 0 }
    val downloadedApkPath: Flow<String?> = context.otaDataStore.data.map { it[KEY_DOWNLOADED_APK_PATH] }
    val pendingVersionCode: Flow<Int> = context.otaDataStore.data.map { it[KEY_PENDING_VERSION_CODE] ?: 0 }

    suspend fun recordCheck() {
        context.otaDataStore.edit { it[KEY_LAST_CHECK] = System.currentTimeMillis() }
    }

    suspend fun recordModalShown() {
        context.otaDataStore.edit {
            it[KEY_LAST_SHOWN] = System.currentTimeMillis()
            it[KEY_SHOW_COUNT] = (it[KEY_SHOW_COUNT] ?: 0) + 1
        }
    }

    suspend fun recordDeferred() {
        context.otaDataStore.edit { it[KEY_DEFER_COUNT] = (it[KEY_DEFER_COUNT] ?: 0) + 1 }
    }

    suspend fun recordDownloaded(versionCode: Int, apkPath: String) {
        context.otaDataStore.edit {
            it[KEY_DOWNLOADED_VERSION] = versionCode
            it[KEY_DOWNLOADED_APK_PATH] = apkPath
            it[KEY_PENDING_VERSION_CODE] = versionCode
        }
    }

    suspend fun recordInstalled(versionCode: Int) {
        context.otaDataStore.edit {
            it[KEY_INSTALLED_VERSION] = versionCode
            it[KEY_DOWNLOADED_VERSION] = 0
            it.remove(KEY_DOWNLOADED_APK_PATH)
            it[KEY_PENDING_VERSION_CODE] = 0
            it[KEY_SHOW_COUNT] = 0
            it[KEY_DEFER_COUNT] = 0
        }
    }

    suspend fun clearDownload() {
        context.otaDataStore.edit {
            it[KEY_DOWNLOADED_VERSION] = 0
            it.remove(KEY_DOWNLOADED_APK_PATH)
            it[KEY_PENDING_VERSION_CODE] = 0
        }
    }

    suspend fun updateInstalledVersion(versionCode: Int) {
        context.otaDataStore.edit { it[KEY_INSTALLED_VERSION] = versionCode }
    }
}

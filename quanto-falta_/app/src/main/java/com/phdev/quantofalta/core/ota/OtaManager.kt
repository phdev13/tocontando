package com.phdev.quantofalta.core.ota

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.phdev.quantofalta.core.analytics.InstallationManager
import java.io.File

/**
 * Central OTA coordinator. Singleton per process.
 * Exposes [otaState] flow that UI observes to show the modal.
 */
class OtaManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "OtaManager"

        @Volatile private var INSTANCE: OtaManager? = null

        fun getInstance(context: Context): OtaManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OtaManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore = OtaDataStore(context)
    private val apiClient = OtaApiClient(context)
    private val downloader = ApkDownloadManager(context)
    private val validator = ApkValidator(context)

    private val _otaState = MutableStateFlow<OtaState>(OtaState.Idle)
    val otaState: StateFlow<OtaState> = _otaState.asStateFlow()

    /** Called by the WorkManager worker and optionally on foreground resume */
    suspend fun checkForUpdates(): Boolean {
        if (_otaState.value is OtaState.Downloading || _otaState.value is OtaState.ReadyToInstall) {
            return true // Don't interrupt ongoing OTA
        }

        _otaState.value = OtaState.Checking
        Log.d(TAG, "Checking for updates…")

        try {
            val installer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            if (installer == "com.android.vending") {
                Log.d(TAG, "Play Store installation detected, disabling OTA.")
                _otaState.value = OtaState.Idle
                return false
            }

            val installationId = InstallationManager.getOrCreateId(context)
            val updateInfo = apiClient.checkForUpdate(installationId) ?: run {
                _otaState.value = OtaState.Idle
                return false
            }

            Log.i(TAG, "Update available: v${updateInfo.versionCode}")
            _otaState.value = OtaState.UpdateAvailable(updateInfo)

            // Auto-download if non-mandatory (let user decide via modal)
            // For mandatory updates, also start download immediately
            if (updateInfo.apkUrl.isNotBlank()) {
                downloadInBackground(updateInfo)
            }
            return true
        } catch (e: Exception) {
            Log.w(TAG, "OTA check error: ${e.message}")
            _otaState.value = OtaState.Idle
            return false
        }
    }

    /** Resume from previously downloaded APK (after app restart) */
    fun checkForPendingInstallation() {
        scope.launch {
            val path = dataStore.downloadedApkPath.first() ?: return@launch
            val version = dataStore.downloadedVersionCode.first()
            if (version <= 0) return@launch

            val file = File(path)
            if (file.exists() && validator.validate(file)) {
                // We don't have the info anymore — show a minimal UpdateAvailable to trigger UI
                // In a full implementation, we'd cache the info too
                Log.d(TAG, "Pending installation found at: $path")
                // The UI will check this state on resume
            } else {
                dataStore.clearDownload()
            }
        }
    }

    private fun downloadInBackground(updateInfo: OtaUpdateInfo) {
        scope.launch {
            _otaState.value = OtaState.Downloading(updateInfo.versionCode, 0)

            val apkFile = downloader.download(
                url = updateInfo.apkUrl,
                expectedSha256 = updateInfo.sha256,
                expectedSizeBytes = updateInfo.apkSizeBytes,
                onProgress = { pct ->
                    _otaState.value = OtaState.Downloading(updateInfo.versionCode, pct)
                }
            )

            if (apkFile == null) {
                Log.w(TAG, "Download failed")
                _otaState.value = OtaState.Error(OtaError.DOWNLOAD_FAILED)
                return@launch
            }

            if (!validator.validate(apkFile)) {
                Log.e(TAG, "APK validation failed")
                apkFile.delete()
                dataStore.clearDownload()
                _otaState.value = OtaState.Error(OtaError.INVALID_APK)
                return@launch
            }

            dataStore.recordDownloaded(updateInfo.versionCode, apkFile.absolutePath)
            Log.i(TAG, "APK ready to install: ${apkFile.absolutePath}")
            _otaState.value = OtaState.ReadyToInstall(updateInfo, apkFile.absolutePath)
        }
    }

    suspend fun recordDeferred() {
        dataStore.recordDeferred()
        _otaState.value = OtaState.Idle
    }

    suspend fun recordModalShown() = dataStore.recordModalShown()

    /** Cleans up downloaded APKs after successful installation or user rejection */
    fun cleanup() {
        scope.launch {
            downloader.deleteDownloads()
            dataStore.clearDownload()
        }
    }
}

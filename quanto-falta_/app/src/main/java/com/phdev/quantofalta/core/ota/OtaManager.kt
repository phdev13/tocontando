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
import com.phdev.quantofalta.core.analytics.AnalyticsEvent
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import com.phdev.quantofalta.core.analytics.InstallationManager
import com.phdev.quantofalta.core.config.AppConfigManager
import java.io.File

/**
 * Central OTA coordinator. Singleton per process.
 *
 * Flow:
 *  1. checkForUpdates() → UpdateAvailable
 *  2. downloadInBackground() → progress shown via notification (state = DownloadingBackground)
 *     OR startDownload() (user-initiated from Settings) → state = Downloading (modal-visible)
 *  3. Download completes → ReadyToInstall + "Tap to install" notification
 *  4. User taps notification or modal button → installApk() called externally
 *  5. After returning from installer → markInstalled() clears state + APK
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

    private val scope      = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dataStore  = OtaDataStore(context)
    private val apiClient  = OtaApiClient(context)
    private val downloader = ApkDownloadManager(context)
    private val validator  = ApkValidator(context)
    private val analytics  = AnalyticsManager(context)

    private val _otaState = MutableStateFlow<OtaState>(OtaState.Idle)
    val otaState: StateFlow<OtaState> = _otaState.asStateFlow()

    private fun isOtaEnabled(): Boolean = AppConfigManager.isOtaEnabled(context)

    private suspend fun disableOta() {
        downloader.deleteDownloads()
        dataStore.clearDownload()
        OtaNotificationHelper.cancelAll(context)
        _otaState.value = OtaState.Idle
    }

    // ─────────────────────── Check & detect ───────────────────────────────

    /**
     * Called by WorkManager worker and optionally on foreground resume.
     * Returns true if an update was found.
     */
    suspend fun checkForUpdates(): Boolean {
        if (!isOtaEnabled()) {
            disableOta()
            return false
        }

        // Don't interrupt an ongoing download or ready state
        val current = _otaState.value
        if (current is OtaState.DownloadingBackground ||
            current is OtaState.Downloading ||
            current is OtaState.ReadyToInstall ||
            current is OtaState.Installing) {
            return true
        }

        _otaState.value = OtaState.Checking
        Log.d(TAG, "Checking for updates…")

        return try {
            val installationId = InstallationManager.getOrCreateId(context)
            val updateInfo = apiClient.checkForUpdate(installationId) ?: run {
                analytics.track(AnalyticsEvent.OtaCheckCompleted)
                _otaState.value = OtaState.Idle
                return false
            }

            Log.i(TAG, "Update available: v${updateInfo.versionCode}")
            analytics.track(AnalyticsEvent.OtaCheckCompleted)
            analytics.track(AnalyticsEvent.OtaUpdateAvailable(updateInfo.versionCode, updateInfo.mandatory))
            _otaState.value = OtaState.UpdateAvailable(updateInfo)

            // Auto-download in background for all updates that have an APK URL
            if (updateInfo.apkUrl.isNotBlank()) {
                downloadInBackground(updateInfo, silent = true)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "OTA check error: ${e.message}")
            _otaState.value = OtaState.Idle
            false
        }
    }

    // ─────────────────────── Resume after restart ─────────────────────────

    /**
     * Called in MainActivity.onCreate / onResume.
     * If the APK was already downloaded in a previous session, surfaces the ReadyToInstall state
     * and re-posts the notification so the user can still install.
     */
    fun checkForPendingInstallation() {
        scope.launch {
            if (!isOtaEnabled()) {
                disableOta()
                return@launch
            }

            val path = dataStore.downloadedApkPath.first() ?: return@launch
            val version = dataStore.downloadedVersionCode.first()
            if (version <= 0) return@launch

            // Guard: if we just came back from the installer (version matches installed),
            // clean up instead of re-surfacing the modal.
            val installedVersion = dataStore.installedVersionCode.first()
            if (installedVersion >= version) {
                Log.d(TAG, "Version $version already recorded as installed — cleaning up")
                cleanup()
                return@launch
            }

            val file = File(path)
            if (!file.exists() || !validator.validate(file)) {
                Log.w(TAG, "Stale or invalid pending APK — discarding")
                dataStore.clearDownload()
                return@launch
            }

            Log.d(TAG, "Pending installation found: v$version at $path")
            val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            val info = OtaUpdateInfo(
                versionCode    = version,
                versionName    = packageInfo?.versionName ?: version.toString(),
                title          = "Atualização pronta para instalar",
                summary        = "O download foi concluído. Toque para instalar.",
                changelog      = emptyList(),
                apkUrl         = "",
                apkSizeBytes   = file.length(),
                sha256         = null,
                mandatory      = false,
                rolloutPercentage = 100,
                releaseChannel = "stable",
                publishedAt    = null,
            )
            _otaState.value = OtaState.ReadyToInstall(info, path)
            // Re-post notification in case it was dismissed
            OtaNotificationHelper.showReadyToInstall(context, info.versionName)
        }
    }

    // ─────────────────────── Download ─────────────────────────────────────

    /**
     * User-initiated download (from Settings screen).
     * Uses the Downloading state (modal-visible progress bar).
     */
    fun startDownload(updateInfo: OtaUpdateInfo) {
        if (!isOtaEnabled()) {
            scope.launch { disableOta() }
            return
        }

        if (updateInfo.apkUrl.isBlank()) {
            _otaState.value = OtaState.Error(OtaError.LINK_EXPIRED)
            return
        }
        downloadInBackground(updateInfo, silent = false)
    }

    /**
     * Internal background download.
     * [silent] = true → uses DownloadingBackground state (no modal) + notification progress.
     * [silent] = false → uses Downloading state (modal with progress bar visible).
     */
    private fun downloadInBackground(updateInfo: OtaUpdateInfo, silent: Boolean) {
        scope.launch {
            if (!isOtaEnabled()) {
                disableOta()
                return@launch
            }

            if (silent) {
                _otaState.value = OtaState.DownloadingBackground(updateInfo.versionCode, 0)
                OtaNotificationHelper.showDownloadProgress(context, updateInfo.versionName, 0)
            } else {
                _otaState.value = OtaState.Downloading(updateInfo.versionCode, 0)
            }
            analytics.track(AnalyticsEvent.OtaDownloadStarted(updateInfo.versionCode))

            val apkFile = downloader.download(
                url = updateInfo.apkUrl,
                expectedSha256 = updateInfo.sha256,
                expectedSizeBytes = updateInfo.apkSizeBytes,
                onProgress = { pct ->
                    if (silent) {
                        _otaState.value = OtaState.DownloadingBackground(updateInfo.versionCode, pct)
                        OtaNotificationHelper.showDownloadProgress(context, updateInfo.versionName, pct)
                    } else {
                        _otaState.value = OtaState.Downloading(updateInfo.versionCode, pct)
                    }
                }
            )

            if (apkFile == null) {
                Log.w(TAG, "Download failed")
                _otaState.value = OtaState.Error(OtaError.DOWNLOAD_FAILED)
                if (silent) OtaNotificationHelper.showDownloadError(context)
                return@launch
            }

            if (!validator.validate(apkFile)) {
                Log.e(TAG, "APK validation failed")
                apkFile.delete()
                dataStore.clearDownload()
                _otaState.value = OtaState.Error(OtaError.INVALID_APK)
                if (silent) OtaNotificationHelper.showDownloadError(context)
                return@launch
            }

            dataStore.recordDownloaded(updateInfo.versionCode, apkFile.absolutePath)
            analytics.track(AnalyticsEvent.OtaDownloadCompleted(updateInfo.versionCode))
            Log.i(TAG, "APK ready to install: ${apkFile.absolutePath}")
            _otaState.value = OtaState.ReadyToInstall(updateInfo, apkFile.absolutePath)

            // Always show the notification — even for user-initiated downloads,
            // so the user has a persistent CTA if they navigated away from Settings.
            OtaNotificationHelper.showReadyToInstall(context, updateInfo.versionName)
        }
    }

    // ─────────────────────── Install lifecycle ────────────────────────────

    /**
     * Called just before launching the system installer.
     * Transitions to Installing state so the modal keeps showing
     * until we know the result.
     */
    fun onInstallLaunched(info: OtaUpdateInfo, apkPath: String) {
        if (!isOtaEnabled()) return

        _otaState.value = OtaState.Installing(info, apkPath)
        analytics.track(AnalyticsEvent.OtaInstallationStarted(info.versionCode))
        OtaNotificationHelper.cancelReady(context)
    }

    /**
     * Called when the app resumes after returning from the system installer.
     * Checks if the installed version matches the pending version and cleans up.
     * 
     * This is the KEY fix for "pede instalar de novo" — we detect that the install
     * succeeded by comparing the running versionCode to the pending versionCode.
     */
    fun onResumeAfterInstaller() {
        scope.launch {
            if (!isOtaEnabled()) {
                disableOta()
                return@launch
            }

            val pendingVersion = dataStore.downloadedVersionCode.first()
            if (pendingVersion <= 0) return@launch // nothing pending

            // Check if the currently running version is >= the pending version
            val runningVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo).toInt()
            } catch (_: Exception) { 0 }

            if (runningVersion >= pendingVersion) {
                Log.i(TAG, "Install confirmed: running v$runningVersion >= pending v$pendingVersion")
                markInstalled(pendingVersion)
            } else {
                // Installer was cancelled or failed — go back to ReadyToInstall
                Log.d(TAG, "Install not completed (running=$runningVersion, pending=$pendingVersion)")
                val path = dataStore.downloadedApkPath.first()
                if (path != null && File(path).exists()) {
                    val state = _otaState.value
                    if (state is OtaState.Installing) {
                        _otaState.value = OtaState.ReadyToInstall(state.info, state.apkPath)
                    }
                } else {
                    _otaState.value = OtaState.Idle
                }
            }
        }
    }

    /** Record successful install — clears state, APK, and notifications */
    private suspend fun markInstalled(versionCode: Int) {
        dataStore.recordInstalled(versionCode)
        downloader.deleteDownloads()
        OtaNotificationHelper.cancelAll(context)
        _otaState.value = OtaState.Idle
        Log.i(TAG, "OTA complete — v$versionCode installed and cleaned up")
    }

    // ─────────────────────── Deferred / dismiss ───────────────────────────

    suspend fun recordDeferred() {
        dataStore.recordDeferred()
        _otaState.value = OtaState.Idle
    }

    suspend fun recordModalShown() = dataStore.recordModalShown()

    // ─────────────────────── Cleanup ──────────────────────────────────────

    /**
     * Hard cleanup: deletes APK files and clears DataStore.
     * Only call this when you're sure the APK is no longer needed
     * (e.g., after confirmed install or user explicit dismissal).
     */
    fun cleanup() {
        scope.launch {
            downloader.deleteDownloads()
            dataStore.clearDownload()
            OtaNotificationHelper.cancelAll(context)
            _otaState.value = OtaState.Idle
        }
    }
}

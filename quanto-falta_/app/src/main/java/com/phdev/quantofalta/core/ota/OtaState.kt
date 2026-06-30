package com.phdev.quantofalta.core.ota

/** Sealed state machine for the OTA update flow */
sealed class OtaState {
    /** No update activity — idle state */
    object Idle : OtaState()

    /** Background check in progress */
    object Checking : OtaState()

    /** Update is available but not yet downloaded */
    data class UpdateAvailable(val info: OtaUpdateInfo) : OtaState()

    /**
     * Download is happening silently in background (no modal shown).
     * Progress is shown via system notification instead.
     */
    data class DownloadingBackground(val versionCode: Int, val progressPercent: Int) : OtaState()

    /**
     * User-initiated download (from settings screen) — shows in-modal progress.
     * Not used for auto-downloads.
     */
    data class Downloading(val versionCode: Int, val progressPercent: Int) : OtaState()

    /** APK downloaded, validated, and ready — awaiting user tap on notification or modal */
    data class ReadyToInstall(val info: OtaUpdateInfo, val apkPath: String) : OtaState()

    /** User tapped "Install" — system installer launched, waiting for result */
    data class Installing(val info: OtaUpdateInfo, val apkPath: String) : OtaState()

    /** An error occurred */
    data class Error(val reason: OtaError) : OtaState()
}

data class OtaUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val title: String,
    val summary: String,
    val changelog: List<String>,
    val apkUrl: String,
    val apkSizeBytes: Long?,
    val sha256: String?,
    val mandatory: Boolean,
    val rolloutPercentage: Int,
    val releaseChannel: String,
    val publishedAt: String?,
)

enum class OtaError {
    NETWORK_UNAVAILABLE,
    SERVER_ERROR,
    DOWNLOAD_FAILED,
    VALIDATION_FAILED,  // SHA-256 mismatch
    INSUFFICIENT_STORAGE,
    INVALID_APK,        // package name mismatch
    LINK_EXPIRED,
}

package com.phdev.quantofalta.core.analytics

/**
 * Centralized, versioned analytics event contract.
 * Only approved events with typed properties are allowed.
 * NEVER include: user event titles, descriptions, personal dates, typed text.
 */
sealed class AnalyticsEvent(val name: String) {

    // ── App lifecycle ──────────────────────────────────────
    object AppOpened : AnalyticsEvent("app_opened")

    data class SessionStarted(val sessionId: String) : AnalyticsEvent("session_started")
    data class SessionEnded(val sessionId: String, val durationMs: Long) : AnalyticsEvent("session_ended")

    data class ScreenViewed(val screen: String, val previousScreen: String? = null) : AnalyticsEvent("screen_viewed")

    // ── Onboarding ─────────────────────────────────────────
    object OnboardingStarted : AnalyticsEvent("onboarding_started")
    object OnboardingCompleted : AnalyticsEvent("onboarding_completed")
    object OnboardingSkipped : AnalyticsEvent("onboarding_skipped")

    // ── Events (NO titles/dates/descriptions) ──────────────
    object EventCreationStarted : AnalyticsEvent("event_creation_started")
    object EventCreated : AnalyticsEvent("event_created")
    object EventCreationAbandoned : AnalyticsEvent("event_creation_abandoned")
    object EventEdited : AnalyticsEvent("event_edited")
    object EventArchived : AnalyticsEvent("event_archived")
    object EventRestored : AnalyticsEvent("event_restored")
    object EventDeleted : AnalyticsEvent("event_deleted")

    // ── Features ───────────────────────────────────────────
    object HighlightOpened : AnalyticsEvent("highlight_opened")
    data class NotificationPermissionResult(val granted: Boolean) : AnalyticsEvent("notification_permission_result")
    object NotificationOpened : AnalyticsEvent("notification_opened")

    // ── Feedback ───────────────────────────────────────────
    object FeedbackOpened : AnalyticsEvent("feedback_opened")
    object FeedbackSubmitted : AnalyticsEvent("feedback_submitted")

    // ── OTA ────────────────────────────────────────────────
    object OtaCheckCompleted : AnalyticsEvent("ota_check_completed")
    data class OtaUpdateAvailable(val otaVersionCode: Int, val mandatory: Boolean) : AnalyticsEvent("ota_update_available")
    data class OtaDownloadStarted(val otaVersionCode: Int) : AnalyticsEvent("ota_download_started")
    data class OtaDownloadCompleted(val otaVersionCode: Int) : AnalyticsEvent("ota_download_completed")
    data class OtaModalShown(val otaVersionCode: Int) : AnalyticsEvent("ota_modal_shown")
    data class OtaUpdateDeferred(val otaVersionCode: Int) : AnalyticsEvent("ota_update_deferred")
    data class OtaInstallationStarted(val otaVersionCode: Int) : AnalyticsEvent("ota_installation_started")
    data class AppVersionChanged(val fromVersionCode: Int, val toVersionCode: Int) : AnalyticsEvent("app_version_changed")

    /** Extracts only approved properties — no free-form strings from user content */
    fun toProperties(): Map<String, Any> = when (this) {
        is SessionStarted -> mapOf("session_id" to sessionId)
        is SessionEnded -> mapOf("session_id" to sessionId, "duration_ms" to durationMs)
        is ScreenViewed -> buildMap {
            put("screen", screen)
            previousScreen?.let { put("previous_screen", it) }
        }
        is NotificationPermissionResult -> mapOf("notification_granted" to granted)
        is OtaUpdateAvailable -> mapOf("ota_version_code" to otaVersionCode, "ota_mandatory" to mandatory)
        is OtaDownloadStarted -> mapOf("ota_version_code" to otaVersionCode)
        is OtaDownloadCompleted -> mapOf("ota_version_code" to otaVersionCode)
        is OtaModalShown -> mapOf("ota_version_code" to otaVersionCode)
        is OtaUpdateDeferred -> mapOf("ota_version_code" to otaVersionCode)
        is OtaInstallationStarted -> mapOf("ota_version_code" to otaVersionCode)
        is AppVersionChanged -> mapOf("from_version_code" to fromVersionCode, "to_version_code" to toVersionCode)
        else -> emptyMap()
    }
}

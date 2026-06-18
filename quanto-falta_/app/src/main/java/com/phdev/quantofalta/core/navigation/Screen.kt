package com.phdev.quantofalta.core.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateEvent : Screen("create_event?eventId={eventId}&prefillTitle={prefillTitle}&prefillColor={prefillColor}&prefillIconName={prefillIconName}&prefillDaysLeft={prefillDaysLeft}") {
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) "create_event?eventId=$eventId" else "create_event"
        }
        fun createPrefillRoute(
            prefillTitle: String,
            prefillColorHex: String,
            prefillIconName: String,
            prefillDaysLeft: Int
        ): String {
            return "create_event?prefillTitle=${android.net.Uri.encode(prefillTitle)}&prefillColor=${android.net.Uri.encode(prefillColorHex)}&prefillIconName=${android.net.Uri.encode(prefillIconName)}&prefillDaysLeft=$prefillDaysLeft"
        }
    }
    object EventDetails : Screen("event_details/{eventId}") {
        fun createRoute(eventId: String) = "event_details/$eventId"
    }
    object Completed : Screen("completed")
    object Highlight : Screen("highlight/{eventId}") {
        fun createRoute(eventId: String) = "highlight/$eventId"
    }
    object More : Screen("more")
    object Testers : Screen("testers")
    object Intro : Screen("intro")
    object Premium : Screen("premium")
    object RedeemCode : Screen("redeem_code")
    object Sponsor : Screen("sponsor")
    object Diagnostics : Screen("diagnostics")
    
    // Novas subtelas de ajustes
    object SettingsAppearance : Screen("settings_appearance")
    object SettingsNotifications : Screen("settings_notifications")
    object SettingsBackup : Screen("settings_backup")
    object SettingsClearData : Screen("settings_clear_data")
    object SettingsSupport : Screen("settings_support")
    object SettingsUpdates : Screen("settings_updates")
    object SettingsPrivacy : Screen("settings_privacy")
    object SettingsAbout : Screen("settings_about")
    object SettingsSync : Screen("settings_sync")
}

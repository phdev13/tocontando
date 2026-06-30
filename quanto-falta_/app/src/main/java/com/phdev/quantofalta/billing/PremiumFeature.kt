package com.phdev.quantofalta.billing

/**
 * Granular feature flags for the Premium system.
 *
 * A user with any active entitlement gets ALL features automatically.
 * Individual flags exist so features can be independently toggled in the future
 * (e.g. beta tests, promotional campaigns, or temporary feature disables).
 *
 * Usage:
 *   val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
 *   isPremium.allows(PremiumFeature.ALL_ICONS)
 */
enum class PremiumFeature {
    /** Create more than FREE_EVENT_LIMIT active events. */
    UNLIMITED_EVENTS,

    /** Access icons beyond the first FREE_ICON_COUNT in the icon list. */
    ALL_ICONS,

    /** Protect individual events with biometric authentication. */
    BIOMETRIC_PROTECTION,

    /** Advanced share image exports (story, wallpaper formats). */
    ADVANCED_EXPORT,

    /** Additional widget styles and multi-event widgets. */
    PREMIUM_WIDGETS,

    /** Full event timeline/history. */
    FULL_STATISTICS,

    /** Multiple scheduled reminders per event, progress-based alerts. */
    ADVANCED_NOTIFICATIONS,

    /** Sub-unit counters: hours, minutes, seconds display modes. */
    ADVANCED_COUNTERS,
    
    /** Add a custom cover photo to the event card. */
    COVER_PHOTO,
    
    /** Use the new premium visual for event cards. */
    PREMIUM_CARDS;

    companion object {
        /** Events a Free user can have active at the same time. */
        const val FREE_EVENT_LIMIT = PremiumPolicy.FREE_EVENT_LIMIT

        /** Number of icons freely accessible (index 0..FREE_ICON_COUNT-1). */
        const val FREE_ICON_COUNT = PremiumPolicy.FREE_ICON_COUNT
    }
}

/**
 * Returns whether a premium [Boolean] grants access to the given [feature].
 * When [this] is true (premium active), all features are allowed.
 */
fun Boolean.allows(feature: PremiumFeature): Boolean = this

/**
 * Convenience — returns true when this premium flag blocks the given feature.
 */
fun Boolean.blocks(feature: PremiumFeature): Boolean = !allows(feature)

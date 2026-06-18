package com.phdev.quantofalta.core

import android.content.Context
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.designsystem.theme.ThemeManager
import com.phdev.quantofalta.core.feedback.FeedbackManager
import com.phdev.quantofalta.core.feedback.SmartFeedbackManager
import com.phdev.quantofalta.core.ota.OtaManager
import com.phdev.quantofalta.core.preferences.IntroManager
import com.phdev.quantofalta.core.privacy.PrivacySettings
import com.phdev.quantofalta.core.testers.TestersManager
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.billing.BillingClientWrapper
import com.phdev.quantofalta.billing.EntitlementManager

class AppContainer(private val context: Context) {
    val database by lazy { AppDatabase.getDatabase(context) }
    val performanceDao by lazy { database.performanceDao() }

    // Infrastructure singletons — created lazily to avoid blocking Application.onCreate()
    val privacySettings by lazy { PrivacySettings(context) }
    val analyticsManager by lazy { AnalyticsManager(context) }
    val authManager by lazy { com.phdev.quantofalta.core.auth.AuthManager(context) }
    val feedbackManager by lazy { FeedbackManager(context) }
    val smartFeedbackManager by lazy { SmartFeedbackManager(context) }
    val themeManager by lazy { ThemeManager(context) }
    val introManager by lazy { IntroManager(context) }
    val testersManager by lazy { TestersManager(context, analyticsManager) }
    val otaManager: OtaManager get() = OtaManager.getInstance(context)
    val entitlementManager by lazy { EntitlementManager(context) }
    val billingClientWrapper by lazy { BillingClientWrapper(context, entitlementManager) }

    // Repository: receives all DAO dependencies and the shared SmartFeedbackManager instance
    val eventRepository by lazy {
        EventRepository(
            context = context,
            eventDao = database.eventDao(),
            eventReminderDao = database.eventReminderDao(),
            eventTimelineDao = database.eventTimelineDao(),
            outboxDao = database.outboxDao(),
            smartFeedbackManager = smartFeedbackManager,
            analyticsManager = analyticsManager
        )
    }
}

package com.phdev.quantofalta

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.core.designsystem.theme.AppTheme
import com.phdev.quantofalta.core.navigation.AppNavigation
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MainActivity : FragmentActivity() {
    private var jankStatsAggregator: com.phdev.quantofalta.core.performance.JankStatsAggregator? = null

    override fun onResume() {
        super.onResume()
        jankStatsAggregator?.resume()
    }

    override fun onPause() {
        super.onPause()
        jankStatsAggregator?.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appContainer = (application as ToContandoApplication).container
            val themeMode by appContainer.themeManager.themeState.collectAsStateWithLifecycle()
            
            val otaState by appContainer.otaManager.otaState.collectAsStateWithLifecycle()
            val shouldShowFeedbackPrompt by appContainer.smartFeedbackManager.shouldShowPrompt.collectAsStateWithLifecycle(initialValue = false)
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            
            // Submitting feedback state
            var isSubmittingFeedback by remember { mutableStateOf(false) }
            var feedbackSubmitResult by remember { mutableStateOf<com.phdev.quantofalta.feature.feedback.FeedbackSubmitResult?>(null) }
            
            androidx.compose.runtime.LaunchedEffect(Unit) {
                appContainer.analyticsManager.track(com.phdev.quantofalta.core.analytics.AnalyticsEvent.AppOpened)

                val isDiagnosticsEnabled = appContainer.privacySettings.sharePerformanceData.first()
                jankStatsAggregator = com.phdev.quantofalta.core.performance.JankStatsAggregator(
                    this@MainActivity,
                    appContainer.performanceDao,
                    isDiagnosticsEnabled
                )

                val startupDuration = System.currentTimeMillis() - ToContandoApplication.appStartTime
                if (isDiagnosticsEnabled) {
                    appContainer.performanceDao.insertMetric(
                        com.phdev.quantofalta.core.database.PerformanceEntity(
                            runId = java.util.UUID.randomUUID().toString(),
                            metricType = "STARTUP",
                            screenName = "App",
                            interaction = null,
                            totalFrames = 0,
                            jankFrames = 0,
                            durationMs = startupDuration,
                            createdAtMillis = System.currentTimeMillis()
                        )
                    )
                }
                
                // Cleanup malformed data that was saved before navigation fix
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val events = appContainer.eventRepository.getAllEvents().first()
                        events.forEach { event ->
                            var changed = false
                            var newTitle = event.title
                            var newIcon = event.iconName
                            if (newTitle.contains("{prefillTitle}")) {
                                newTitle = "Meu Evento"
                                changed = true
                            }
                            if (newIcon.contains("{prefillIconName}")) {
                                newIcon = "Star"
                                changed = true
                            }
                            if (changed) {
                                appContainer.eventRepository.insertEvent(event.copy(title = newTitle, iconName = newIcon))
                            }
                        }
                    } catch (e: Exception) {}
                }

                appContainer.otaManager.checkForPendingInstallation()
                com.phdev.quantofalta.core.icon.IconManager.syncIconWithServer(this@MainActivity)
                com.phdev.quantofalta.core.config.AppConfigManager.syncWithServer(this@MainActivity)
                appContainer.smartFeedbackManager.recordAppOpen()
                
                launch(kotlinx.coroutines.Dispatchers.IO) {
                    com.phdev.quantofalta.core.diagnostics.NotificationDiagnosticsReporter.report(this@MainActivity)
                }
            }
            
            AppTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                            onResult = { _ -> }
                        )
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    AppNavigation(
                        onScreenChange = { screenName ->
                            jankStatsAggregator?.setScreenState(screenName)
                        }
                    )
                    
                    com.phdev.quantofalta.core.ota.ui.OtaUpdateModal(
                        otaState = otaState,
                        onUpdate = {
                            scope.launch {
                                appContainer.otaManager.checkForUpdates()
                            }
                        },
                        onDefer = {
                            scope.launch {
                                appContainer.otaManager.recordDeferred()
                            }
                        },
                        onDismiss = {
                            scope.launch {
                                appContainer.otaManager.recordDeferred()
                            }
                        }
                    )

                    if (shouldShowFeedbackPrompt) {
                        com.phdev.quantofalta.feature.feedback.FeedbackModal(
                            onDismiss = {
                                scope.launch {
                                    appContainer.smartFeedbackManager.recordDismissed()
                                }
                            },
                            onSubmit = { feedbackData ->
                                isSubmittingFeedback = true
                                scope.launch {
                                    val success = appContainer.feedbackManager.submit(feedbackData)
                                    appContainer.smartFeedbackManager.recordSubmitted()
                                    isSubmittingFeedback = false
                                    feedbackSubmitResult = if (success) {
                                        com.phdev.quantofalta.feature.feedback.FeedbackSubmitResult.SUCCESS
                                    } else {
                                        com.phdev.quantofalta.feature.feedback.FeedbackSubmitResult.QUEUED
                                    }
                                }
                            },
                            isSubmitting = isSubmittingFeedback,
                            submitResult = feedbackSubmitResult,
                            sourceScreen = "smart_prompt"
                        )
                    }
                }
            }
        }
    }
}

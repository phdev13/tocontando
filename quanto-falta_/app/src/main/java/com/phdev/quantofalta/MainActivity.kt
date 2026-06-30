package com.phdev.quantofalta

import android.content.Intent
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
import com.phdev.quantofalta.core.navigation.Screen
import com.phdev.quantofalta.core.config.AppConfigManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.phdev.quantofalta.core.ota.OtaNotificationHelper
import com.phdev.quantofalta.core.ota.OtaState
import com.phdev.quantofalta.core.ota.ui.OtaUpdateModal
import com.phdev.quantofalta.core.ota.ui.installApk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private var jankStatsAggregator: com.phdev.quantofalta.core.performance.JankStatsAggregator? = null

    /**
     * Whether we returned from the system package installer.
     * Set to true in onResume only if otaState was Installing when we left.
     */
    private var returningFromInstaller = false

    override fun onResume() {
        super.onResume()
        jankStatsAggregator?.resume()

        // KEY FIX: detect return from installer and let OtaManager check the result.
        if (returningFromInstaller) {
            returningFromInstaller = false
            val appContainer = (application as ToContandoApplication).container
            appContainer.otaManager.onResumeAfterInstaller()
        }
    }

    override fun onPause() {
        super.onPause()
        jankStatsAggregator?.stop()

        // Mark that we may be going to the system installer
        val appContainer = (application as ToContandoApplication).container
        if (appContainer.otaManager.otaState.value is OtaState.Installing) {
            returningFromInstaller = true
        }
    }

    /**
     * Handle taps on the "Tap to install" notification when the app is already open.
     * singleTop launch mode ensures this is called instead of recreating the activity.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The intent flag is handled inside setContent via the LaunchedEffect below
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
                withFrameNanos { }
                val startupDuration = System.currentTimeMillis() - ToContandoApplication.appStartTime

                launch(Dispatchers.IO) {
                    val isDiagnosticsEnabled = appContainer.privacySettings.sharePerformanceData.first()
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

                    withContext(Dispatchers.Main) {
                        jankStatsAggregator = com.phdev.quantofalta.core.performance.JankStatsAggregator(
                            this@MainActivity,
                            appContainer.performanceDao,
                            isDiagnosticsEnabled
                        )
                        jankStatsAggregator?.resume()
                    }
                }

                launch(Dispatchers.Default) {
                    delay(1_500)
                    appContainer.analyticsManager.track(com.phdev.quantofalta.core.analytics.AnalyticsEvent.AppOpened)
                    appContainer.smartFeedbackManager.recordAppOpen()

                    launch(Dispatchers.IO) {
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

                    launch(Dispatchers.IO) {
                        AppConfigManager.syncWithServer(this@MainActivity)
                        if (AppConfigManager.isOtaEnabled(this@MainActivity)) {
                            OtaNotificationHelper.ensureChannel(this@MainActivity)
                            appContainer.otaManager.checkForPendingInstallation()
                            appContainer.otaManager.checkForUpdates()
                        } else {
                            com.phdev.quantofalta.core.ota.OtaWorker.cancel(this@MainActivity)
                            OtaNotificationHelper.cancelAll(this@MainActivity)
                            appContainer.otaManager.cleanup()
                        }
                    }

                    launch(Dispatchers.IO) {
                        appContainer.entitlementManager.syncWithServer(this@MainActivity)
                    }

                    launch(Dispatchers.IO) {
                        com.phdev.quantofalta.core.diagnostics.NotificationDiagnosticsReporter.report(this@MainActivity)
                    }
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
                            delay(2_000)
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    val eventIdFromIntent = intent.getStringExtra("EVENT_ID")
                    val actionCreateFromIntent = intent.getBooleanExtra("ACTION_CREATE", false)

                    var initialEventToOpen by remember { mutableStateOf(eventIdFromIntent) }
                    val navController = androidx.navigation.compose.rememberNavController()

                    androidx.compose.runtime.LaunchedEffect(actionCreateFromIntent) {
                        if (actionCreateFromIntent) {
                            navController.navigate(Screen.CreateEvent.createRoute())
                            intent.removeExtra("ACTION_CREATE")
                        }
                    }

                    com.phdev.quantofalta.core.time.ProvideScreenTicker {
                        com.phdev.quantofalta.core.designsystem.components.AdaptiveContent {
                            AppNavigation(
                                eventToOpen = initialEventToOpen,
                                onEventOpened = { initialEventToOpen = null },
                                navController = navController,
                                onScreenChange = { screenName ->
                                    jankStatsAggregator?.setScreenState(screenName)
                                }
                            )
                        }
                    }

                    // ─── OTA Modal ─────────────────────────────────────────────────
                    OtaUpdateModal(
                        otaState = otaState,
                        onUpdate = {
                            // Called from UpdateAvailable screen — start the download
                            scope.launch {
                                val current = appContainer.otaManager.otaState.value
                                if (current is OtaState.UpdateAvailable) {
                                    appContainer.otaManager.startDownload(current.info)
                                } else {
                                    appContainer.otaManager.checkForUpdates()
                                }
                            }
                        },
                        onInstall = { apkPath ->
                            // Called from ReadyToInstall screen — notify OtaManager then launch installer
                            val current = appContainer.otaManager.otaState.value
                            if (current is OtaState.ReadyToInstall) {
                                appContainer.otaManager.onInstallLaunched(current.info, apkPath)
                            }
                            installApk(this@MainActivity, apkPath)
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
                    // ───────────────────────────────────────────────────────────────

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

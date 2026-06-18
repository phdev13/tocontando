package com.phdev.quantofalta.feature.more

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.core.designsystem.theme.ThemeManager
import com.phdev.quantofalta.core.preferences.IntroManager
import com.phdev.quantofalta.core.privacy.PrivacySettings
import com.phdev.quantofalta.billing.EntitlementManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val themeMode: com.phdev.quantofalta.core.designsystem.theme.AppThemeMode = com.phdev.quantofalta.core.designsystem.theme.AppThemeMode.SYSTEM,
    val activeEvents: Int = 0,
    val completedEvents: Int = 0,
    val diagnosticsEnabled: Boolean = true,
    val isPremium: Boolean = false,
    val premiumStatus: String = "Plano gratuito",
    val appVersion: String = "1.0.0"
)

class MoreViewModel(
    application: Application,
    private val repository: EventRepository,
    private val themeManager: ThemeManager,
    private val introManager: IntroManager,
    private val entitlementManager: EntitlementManager,
    private val privacySettings: PrivacySettings
) : AndroidViewModel(application) {

    val appVersion: String = try {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "1.0.0"
    } catch (e: Exception) { "1.0.0" }

    val uiState: StateFlow<SettingsUiState> = combine(
        themeManager.themeState,
        repository.countActiveEvents(),
        repository.countCompletedEvents(),
        privacySettings.sharePerformanceData,
        entitlementManager.hasActivePremium
    ) { theme, activeCount, completedCount, diagEnabled, isPremium ->
        SettingsUiState(
            themeMode = theme,
            activeEvents = activeCount,
            completedEvents = completedCount,
            diagnosticsEnabled = diagEnabled,
            isPremium = isPremium,
            premiumStatus = if (isPremium) "Premium" else "Plano gratuito",
            appVersion = appVersion
        )
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(appVersion = appVersion)
    )

    fun selectTheme(theme: com.phdev.quantofalta.core.designsystem.theme.AppThemeMode) {
        themeManager.setThemePreference(theme)
    }
    
    fun setDiagnosticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            privacySettings.setSharePerformanceData(enabled)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllEvents()
        }
    }

    fun resetIntro() {
        viewModelScope.launch {
            introManager.setIntroCompleted(false)
        }
    }
}

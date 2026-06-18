package com.phdev.quantofalta.core.designsystem.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _themeState = MutableStateFlow(getThemePreference())
    val themeState: StateFlow<AppThemeMode> = _themeState.asStateFlow()

    fun setThemePreference(theme: AppThemeMode) {
        prefs.edit().putString("app_theme_mode", theme.name).apply()
        _themeState.value = theme
    }

    private fun getThemePreference(): AppThemeMode {
        val savedName = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(savedName)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }
}

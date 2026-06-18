package com.phdev.quantofalta.feature.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.feature.widget.state.WidgetState
import com.phdev.quantofalta.feature.widget.state.WidgetTheme
import com.phdev.quantofalta.feature.widget.state.WidgetUnitMode
import com.phdev.quantofalta.feature.widget.ui.EventWidgetLayout
import com.phdev.quantofalta.core.database.WidgetEventData
import kotlinx.coroutines.flow.first

class EventWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ToContandoApplication
        val eventDao = app.container.database.eventDao()
        val entitlementManager = app.container.entitlementManager

        val isPremium = entitlementManager.hasActivePremium.first()
        
        val prefs = androidx.glance.appwidget.state.getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        
        val state = buildState(prefs, isPremium, eventDao)

        provideContent {
            // Support reactive preference updates without full rebuild if only UI state changes
            val currentPrefs = currentState<Preferences>()
            
            // Build the state dynamically (note: suspend functions inside Composable are generally handled via LaunchedEffect, 
            // but for Glance, we rely on the initial suspend provideGlance to fetch the heavy data, and pass it down).
            // To make it fully reactive to data changes, the Scheduler forces an update().
            
            val currentEventId = currentPrefs[eventIdKey] ?: prefs[eventIdKey]
            val currentThemeStr = currentPrefs[themeKey] ?: prefs[themeKey]
            val currentUnitMode = currentPrefs[unitModeKey] ?: prefs[unitModeKey]
            
            // Re-resolve state using the loaded data if IDs match, otherwise fallback to Unconfigured/Unavailable
            val activeState = if (currentEventId == prefs[eventIdKey]) {
                if (!isPremium) {
                    WidgetState.PremiumRequired
                } else if (currentEventId == null) {
                    WidgetState.Unconfigured
                } else if (state is WidgetState.Configured) {
                    state.copy(
                        theme = WidgetTheme.fromString(currentThemeStr),
                        unitMode = WidgetUnitMode.fromString(currentUnitMode)
                    )
                } else {
                    state
                }
            } else {
                // If it changed drastically in composition, we show unavailable until next full update
                WidgetState.EventUnavailable 
            }

            EventWidgetLayout(context = context, state = activeState)
        }
    }

    private suspend fun buildState(
        prefs: Preferences,
        isPremium: Boolean,
        eventDao: com.phdev.quantofalta.core.database.EventDao
    ): WidgetState {
        if (!isPremium) return WidgetState.PremiumRequired

        val eventId = prefs[eventIdKey] ?: return WidgetState.Unconfigured
        val eventData = eventDao.getWidgetEventByIdSync(eventId) ?: return WidgetState.EventUnavailable

        if (eventData.isPrivate) return WidgetState.PrivateEvent(eventId)

        val theme = WidgetTheme.fromString(prefs[themeKey])
        val unitMode = WidgetUnitMode.fromString(prefs[unitModeKey])

        return WidgetState.Configured(eventData, theme, unitMode)
    }

    companion object {
        val eventIdKey = stringPreferencesKey("widget_event_id")
        val themeKey = stringPreferencesKey("widget_theme")
        val unitModeKey = stringPreferencesKey("widget_unit_mode")
    }
}

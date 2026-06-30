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
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.data.repository.toDomainModel
import com.phdev.quantofalta.feature.widget.state.WidgetState
import com.phdev.quantofalta.feature.widget.state.WidgetTheme
import com.phdev.quantofalta.feature.widget.state.WidgetUnitMode
import com.phdev.quantofalta.feature.widget.util.WidgetBitmapLoader
import kotlinx.coroutines.flow.first

class MicroWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ToContandoApplication
        val eventDao = app.container.database.eventDao()
        val entitlementManager = app.container.entitlementManager

        val isPremium = entitlementManager.hasActivePremium.first()
        
        val prefs = androidx.glance.appwidget.state.getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        
        val state = buildState(context, prefs, isPremium, eventDao)

        provideContent {
            val currentPrefs = currentState<Preferences>()
            
            val currentEventId = currentPrefs[eventIdKey] ?: prefs[eventIdKey]
            val currentThemeStr = currentPrefs[themeKey] ?: prefs[themeKey]
            val currentUnitMode = currentPrefs[unitModeKey] ?: prefs[unitModeKey]
            
            val activeState = if (currentEventId == prefs[eventIdKey]) {
                if (currentEventId == null) {
                    WidgetState.Unconfigured
                } else if (state is WidgetState.Configured) {
                    val selectedTheme = WidgetTheme.fromString(currentThemeStr)
                    if (!isPremium && selectedTheme != WidgetTheme.COMPACT) {
                        WidgetState.PremiumRequired
                    } else state.copy(
                        theme = selectedTheme,
                        unitMode = WidgetUnitMode.fromString(currentUnitMode)
                    )
                } else {
                    state
                }
            } else {
                WidgetState.EventUnavailable 
            }

            when (activeState) {
                is WidgetState.PremiumRequired -> com.phdev.quantofalta.feature.widget.ui.PremiumRequiredWidget(context)
                is WidgetState.Unconfigured, is WidgetState.EventUnavailable -> com.phdev.quantofalta.feature.widget.ui.UnconfiguredWidget(context, activeState)
                is WidgetState.PrivateEvent -> com.phdev.quantofalta.feature.widget.ui.PrivateEventWidget(context, activeState.eventId)
                is WidgetState.Error -> com.phdev.quantofalta.feature.widget.ui.ErrorWidget()
                is WidgetState.Configured -> com.phdev.quantofalta.feature.widget.ui.MicroWidgetLayout(context, activeState)
                else -> com.phdev.quantofalta.feature.widget.ui.ErrorWidget()
            }
        }
    }

    private suspend fun buildState(
        context: Context,
        prefs: Preferences,
        isPremium: Boolean,
        eventDao: com.phdev.quantofalta.core.database.EventDao
    ): WidgetState {
        val eventId = prefs[eventIdKey] ?: return WidgetState.Unconfigured
        val eventEntity = eventDao.getEventByIdSync(eventId) ?: return WidgetState.EventUnavailable

        if (eventEntity.isPrivate) return WidgetState.PrivateEvent(eventId)

        val theme = WidgetTheme.fromString(prefs[themeKey])
        val unitMode = WidgetUnitMode.fromString(prefs[unitModeKey])
        if (!isPremium && theme != WidgetTheme.COMPACT) return WidgetState.PremiumRequired

        val event = eventEntity.toDomainModel()
        val uiModel = event.toUiModel(context = context)
        
        var coverBitmap: android.graphics.Bitmap? = null
        if (isPremium && uiModel.coverImageUri != null) {
            coverBitmap = WidgetBitmapLoader.loadBitmap(context, uiModel.coverImageUri, 200) // smaller size for micro widget
        }

        return WidgetState.Configured(uiModel, theme, unitMode, coverBitmap)
    }

    companion object {
        val eventIdKey = stringPreferencesKey("widget_event_id")
        val themeKey = stringPreferencesKey("widget_theme")
        val unitModeKey = stringPreferencesKey("widget_unit_mode")
    }
}

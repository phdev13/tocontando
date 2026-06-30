package com.phdev.quantofalta.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.data.repository.toDomainModel
import com.phdev.quantofalta.feature.widget.state.WidgetState
import com.phdev.quantofalta.feature.widget.util.WidgetBitmapLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ListWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ToContandoApplication
        val eventDao = app.container.database.eventDao()
        val entitlementManager = app.container.entitlementManager

        val isPremium = entitlementManager.hasActivePremium.first()
        
        val state = buildState(context, isPremium, eventDao)

        provideContent {
            when (state) {
                is WidgetState.PremiumRequired -> com.phdev.quantofalta.feature.widget.ui.PremiumRequiredWidget(context)
                is WidgetState.ListConfigured -> com.phdev.quantofalta.feature.widget.ui.ListWidgetLayout(context, state)
                is WidgetState.Error -> com.phdev.quantofalta.feature.widget.ui.ErrorWidget()
                else -> com.phdev.quantofalta.feature.widget.ui.UnconfiguredWidget(context, state)
            }
        }
    }

    private suspend fun buildState(
        context: Context,
        isPremium: Boolean,
        eventDao: com.phdev.quantofalta.core.database.EventDao
    ): WidgetState {
        if (!isPremium) return WidgetState.PremiumRequired

        // Get up to 5 next upcoming events that are not private
        val eventsEntities = eventDao.getAllEvents().firstOrNull()?.filter { !it.isPrivate }?.sortedBy { it.targetDate }?.take(5) ?: emptyList()
        
        val items = eventsEntities.map { entity ->
            val event = entity.toDomainModel()
            val uiModel = event.toUiModel(context = context)
            var coverBitmap: android.graphics.Bitmap? = null
            
            if (isPremium && uiModel.coverImageUri != null) {
                coverBitmap = WidgetBitmapLoader.loadBitmap(context, uiModel.coverImageUri, 200) // smaller size for list thumbnail
            }
            
            WidgetState.ListEventItem(uiModel, coverBitmap)
        }
        
        return WidgetState.ListConfigured(items)
    }
}

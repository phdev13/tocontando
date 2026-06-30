package com.phdev.quantofalta.feature.widget.state

import android.graphics.Bitmap
import com.phdev.quantofalta.domain.model.EventUiModel

sealed interface WidgetState {
    data class Configured(
        val event: EventUiModel,
        val theme: WidgetTheme,
        val unitMode: WidgetUnitMode,
        val coverBitmap: Bitmap? = null
    ) : WidgetState

    data object Unconfigured : WidgetState
    data object EventUnavailable : WidgetState
    data class PrivateEvent(val eventId: String) : WidgetState
    data object PremiumRequired : WidgetState
    data object Error : WidgetState
    
    data class ListEventItem(val event: EventUiModel, val coverBitmap: Bitmap? = null)
    data class ListConfigured(val events: List<ListEventItem>) : WidgetState
}

package com.phdev.quantofalta.feature.widget.state

import com.phdev.quantofalta.core.database.WidgetEventData

sealed interface WidgetState {
    data class Configured(
        val event: WidgetEventData,
        val theme: WidgetTheme,
        val unitMode: WidgetUnitMode
    ) : WidgetState

    data object Unconfigured : WidgetState
    data object EventUnavailable : WidgetState
    data class PrivateEvent(val eventId: String) : WidgetState
    data object PremiumRequired : WidgetState
    data object Error : WidgetState
}

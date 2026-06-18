package com.phdev.quantofalta.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.database.AppDatabase
import com.phdev.quantofalta.core.database.EventTimelineEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf

class TimelineViewModel(private val database: AppDatabase) : ViewModel() {

    fun getTimeline(eventId: String): StateFlow<List<EventTimelineEntity>> {
        return database.eventTimelineDao().getTimelineForEvent(eventId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
}

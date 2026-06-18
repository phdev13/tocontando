package com.phdev.quantofalta.feature.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.domain.model.toUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.phdev.quantofalta.domain.model.dateMillis

class CompletedViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository
) : ViewModel() {

    /**
     * No ticker needed for completed/archived events.
     * These are static snapshots — they don't countdown.
     * A snapshot of currentMillis is taken once per emission, which is sufficient
     * since the list only changes when the DB changes (e.g., user archives or restores).
     */
    val uiState: StateFlow<ImmutableList<EventUiModel>> = repository.getAllEvents()
        .map { events ->
            val now = java.time.Instant.now()
            events
                .filter { it.isArchived || it.isCompleted || now.toEpochMilli() >= it.dateMillis }
                .sortedByDescending { it.dateMillis }
                .map { it.toUiModel(now, application) }
                .toImmutableList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())
}

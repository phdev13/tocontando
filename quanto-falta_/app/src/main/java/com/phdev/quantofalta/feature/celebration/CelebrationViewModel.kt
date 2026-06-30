package com.phdev.quantofalta.feature.celebration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class CelebrationViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CelebrationUiState>(CelebrationUiState.Loading)
    val uiState: StateFlow<CelebrationUiState> = _uiState.asStateFlow()

    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _uiState.value = CelebrationUiState.Loading
            val event = eventRepository.getEventById(eventId).firstOrNull()
            if (event != null) {
                _uiState.value = CelebrationUiState.Success(event)
            } else {
                _uiState.value = CelebrationUiState.Error
            }
        }
    }

    fun markCelebrationViewed(eventId: String) {
        viewModelScope.launch {
            val event = eventRepository.getEventById(eventId).firstOrNull()
            if (event != null) {
                val epochDay = LocalDate.now(ZoneId.of(event.zoneId)).toEpochDay()
                eventRepository.updateLastCelebrationEpochDay(eventId, epochDay)
                
                if (event.type == com.phdev.quantofalta.domain.model.EventType.STANDARD) {
                    eventRepository.markEventAsCompleted(eventId, true)
                }
            }
        }
    }
}

sealed class CelebrationUiState {
    object Loading : CelebrationUiState()
    data class Success(val event: Event) : CelebrationUiState()
    object Error : CelebrationUiState()
}

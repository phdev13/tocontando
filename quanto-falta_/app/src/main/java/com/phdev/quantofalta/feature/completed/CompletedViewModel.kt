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

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class CompletedViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository
) : ViewModel() {

    val uiState: StateFlow<Map<String, List<EventUiModel>>?> = repository.getAllEvents()
        .map { events ->
            val now = Instant.now()
            val nowZoned = ZonedDateTime.ofInstant(now, ZoneId.systemDefault())

            val completedList = events
                .filter { it.isArchived || it.isCompleted || (!it.isSalaryEvent() && !it.isRelationshipEvent() && now.toEpochMilli() >= it.dateMillis) }
                .sortedByDescending { it.dateMillis }
                .map { it.toUiModel(now, application) }
                
            val grouped = mutableMapOf<String, MutableList<EventUiModel>>()
            
            completedList.forEach { uiModel ->
                val eventZoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(uiModel.dateMillis), ZoneId.systemDefault())
                val groupName = getGroupName(eventZoned, nowZoned)
                if (!grouped.containsKey(groupName)) {
                    grouped[groupName] = mutableListOf()
                }
                grouped[groupName]?.add(uiModel)
            }
            
            grouped.mapValues { it.value.toList() } // Retorna Map<String, List<EventUiModel>>
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun getGroupName(eventTime: ZonedDateTime, nowTime: ZonedDateTime): String {
        val daysBetween = ChronoUnit.DAYS.between(eventTime.toLocalDate(), nowTime.toLocalDate())
        
        return when {
            daysBetween == 0L -> "Hoje"
            daysBetween in 1..7 -> "Esta semana"
            eventTime.year == nowTime.year && eventTime.month == nowTime.month -> "Este mês"
            eventTime.year == nowTime.year -> "Este ano"
            else -> eventTime.year.toString()
        }
    }
}

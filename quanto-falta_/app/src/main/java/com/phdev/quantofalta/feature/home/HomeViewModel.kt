package com.phdev.quantofalta.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.EventType
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.domain.model.toUiModel
import kotlinx.coroutines.delay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.compose.runtime.Immutable

import com.android.billingclient.api.Purchase
import com.phdev.quantofalta.billing.BillingClientWrapper
import com.phdev.quantofalta.billing.EntitlementManager

@Immutable
data class HomeModePage(
    val mode: EventType,
    val featuredCard: EventUiModel?,
    val compactCards: ImmutableList<EventUiModel>
)

@Immutable
data class HomeUiState(
    val pages: ImmutableList<HomeModePage> = persistentListOf(),
    val celebrationEvent: com.phdev.quantofalta.domain.model.EventUiModel? = null,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository,
    private val billingClientWrapper: BillingClientWrapper,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = combine(
        billingClientWrapper.purchases,
        entitlementManager.activeEntitlements   // already filters expired entries
    ) { purchases, entitlements ->
        val hasActivePurchase = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val hasActiveEntitlement = entitlements.isNotEmpty() // expiresAt checked inside EntitlementManager
        hasActivePurchase || hasActiveEntitlement
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _minuteTick = kotlinx.coroutines.flow.MutableStateFlow(java.time.Instant.now())
    val minuteTick: StateFlow<java.time.Instant> = _minuteTick

    init {
        viewModelScope.launch {
            flow {
                while (true) {
                    emit(java.time.Instant.now())
                    delay(60_000L) // 1 minute
                }
            }.collect {
                _minuteTick.value = it
            }
        }
    }

    val uiState: StateFlow<HomeUiState?> = combine(
        repository.getAllEvents(),
        minuteTick
    ) { events, currentInstant ->
        val mappedEvents = events
            .filter { !it.isArchived }
            .map { it.toUiModel(currentInstant, application) }

        val celebrationEvent = mappedEvents.firstOrNull { it.shouldShowCelebration }
        val activeEvents = mappedEvents.filter { it.eventState != com.phdev.quantofalta.domain.model.EventState.COMPLETED }

        val standard = activeEvents.filter { it.type == EventType.STANDARD }.sortedBy { it.dateMillis }
        val relationship = activeEvents.filter { it.type == EventType.RELATIONSHIP }.sortedBy { it.dateMillis }
        val salary = activeEvents.filter { it.type == EventType.SALARY }.sortedBy { it.dateMillis }

        fun processMode(mode: EventType, eventsOfMode: List<EventUiModel>): HomeModePage {
            if (eventsOfMode.isEmpty()) return HomeModePage(mode, null, persistentListOf())
            
            val highlight = eventsOfMode.firstOrNull { it.isPinned }
            
            return if (highlight != null) {
                val sanitizedHighlight = highlight.copy(isAutoHighlighted = false)
                val compacts = eventsOfMode
                    .filter { it.stableId != highlight.stableId }
                    .map { it.copy(isPinned = false, isAutoHighlighted = false) }
                    .toImmutableList()
                
                HomeModePage(mode, sanitizedHighlight, compacts)
            } else {
                val autoHighlight = eventsOfMode.first().copy(isAutoHighlighted = true, isPinned = false)
                val compacts = eventsOfMode
                    .drop(1)
                    .map { it.copy(isPinned = false, isAutoHighlighted = false) }
                    .toImmutableList()
                    
                HomeModePage(mode, autoHighlight, compacts)
            }
        }
        
        val standardPage = processMode(EventType.STANDARD, standard)
        val relationshipPage = processMode(EventType.RELATIONSHIP, relationship)
        val salaryPage = processMode(EventType.SALARY, salary)

        HomeUiState(
            pages = persistentListOf(standardPage, relationshipPage, salaryPage),
            celebrationEvent = celebrationEvent,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun markCelebrated(eventId: String) {
        viewModelScope.launch {
            try {
                val event = repository.getEventById(eventId).firstOrNull()
                if (event != null) {
                    val epochDay = if (event.type == com.phdev.quantofalta.domain.model.EventType.STANDARD) {
                        event.targetDate.toEpochDay()
                    } else {
                        java.time.LocalDate.now(java.time.ZoneId.of(event.zoneId)).toEpochDay()
                    }
                    repository.insertEvent(event.copy(isCompleted = true, lastCelebrationEpochDay = epochDay))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

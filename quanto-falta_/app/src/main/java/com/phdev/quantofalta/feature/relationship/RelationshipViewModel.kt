package com.phdev.quantofalta.feature.relationship

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.relationship.RelationshipCalculator
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownFormat
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.RelationshipUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// UI State for the Create / Edit form
// ─────────────────────────────────────────────────────────────────────────────
data class CreateRelationshipUiState(
    val name: String = "",
    val type: String = "dating",               // dating | married | engaged | friendship | other
    val startEpochDay: Long = LocalDate.now().toEpochDay(),
    val iconName: String = "Favorite",
    val colorArgb: Int = 0xFF9C27B0.toInt(),   // default purple
    val coverImageUri: String? = null,
    val monthlyEnabled: Boolean = true,
    val annualEnabled: Boolean = true,
    val milestonesEnabled: Boolean = true,
    val showOnHome: Boolean = true,
    val isHighlighted: Boolean = true,
    val relationshipCardStyle: com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle = com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle.HEART,
    val isSaving: Boolean = false,
    val error: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// UI State for the Detail screen
// ─────────────────────────────────────────────────────────────────────────────
sealed class RelationshipDetailUiState {
    object Loading : RelationshipDetailUiState()
    object NotFound : RelationshipDetailUiState()
    data class Success(
        val event: Event,
        val stats: RelationshipCalculator.RelationshipStats,
        val primaryText: String,
        val secondaryText: String,
    ) : RelationshipDetailUiState()
}

class RelationshipViewModel(
    application: Application,
    private val repository: EventRepository,
    private val permissionsUseCase: com.phdev.quantofalta.domain.usecase.PermissionsUseCase
) : AndroidViewModel(application) {

    val isPremium = permissionsUseCase.isPremium.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        false
    )
    private var eventBeingEdited: Event? = null
    private var detailJob: Job? = null
    private var editLoadJob: Job? = null
    private var saveJob: Job? = null

    // ── Create / Edit form state ──────────────────────────────────────────────
    private val _formState = MutableStateFlow(CreateRelationshipUiState())
    val formState: StateFlow<CreateRelationshipUiState> = _formState.asStateFlow()

    // ── Detail state ──────────────────────────────────────────────────────────
    private val _detailState = MutableStateFlow<RelationshipDetailUiState>(RelationshipDetailUiState.Loading)
    val detailState: StateFlow<RelationshipDetailUiState> = _detailState.asStateFlow()

    // ── Load existing relationship for editing or detail ──────────────────────
    fun loadEvent(eventId: String) {
        detailJob?.cancel()
        _detailState.value = RelationshipDetailUiState.Loading
        detailJob = viewModelScope.launch {
            repository.getEventById(eventId).collect { event ->
                if (event == null || event.relationshipType == null) {
                    _detailState.value = RelationshipDetailUiState.NotFound
                    return@collect
                }
                val startEpochDay = event.relationshipStartEpochDay ?: event.targetDate.toEpochDay()
                val stats = RelationshipCalculator.calculate(startEpochDay)
                _detailState.value = RelationshipDetailUiState.Success(
                    event = event,
                    stats = stats,
                    primaryText = RelationshipCalculator.formatPrimaryText(stats, event.relationshipType),
                    secondaryText = RelationshipCalculator.formatSecondaryText(
                        stats,
                        event.relationshipMonthlyEnabled,
                        event.relationshipAnnualEnabled,
                        event.relationshipMilestonesEnabled,
                    ),
                )
            }
        }
    }

    // ── Populate form for editing ─────────────────────────────────────────────
    fun loadForEdit(eventId: String) {
        editLoadJob?.cancel()
        editLoadJob = viewModelScope.launch {
            val event = repository.getEventById(eventId).first() ?: return@launch
            eventBeingEdited = event
                _formState.value = CreateRelationshipUiState(
                    name = event.title,
                    type = event.relationshipType ?: "dating",
                    startEpochDay = event.relationshipStartEpochDay ?: event.targetDate.toEpochDay(),
                    iconName = event.iconName,
                    colorArgb = event.colorArgb,
                    coverImageUri = event.coverImageUri,
                    monthlyEnabled = event.relationshipMonthlyEnabled,
                    annualEnabled = event.relationshipAnnualEnabled,
                    milestonesEnabled = event.relationshipMilestonesEnabled,
                    showOnHome = !event.isArchived,
                    isHighlighted = event.isPinned,
                    relationshipCardStyle = event.relationshipModeStyle,
                )
        }
    }

    // ── Form field setters ────────────────────────────────────────────────────
    fun onNameChange(v: String) { _formState.value = _formState.value.copy(name = v, error = null) }
    fun onTypeChange(v: String) {
        _formState.value = _formState.value.copy(
            type = v,
            monthlyEnabled = v != "married" && v != "other",
            error = null,
        )
    }
    fun onStartDateChange(epochDay: Long) {
        _formState.value = _formState.value.copy(startEpochDay = epochDay, error = null)
    }
    fun onIconChange(v: String) { _formState.value = _formState.value.copy(iconName = v) }
    fun onColorChange(argb: Int) { _formState.value = _formState.value.copy(colorArgb = argb) }
    fun onCoverImageChange(uri: String?) { _formState.value = _formState.value.copy(coverImageUri = uri, error = null) }
    fun onRelationshipCardStyleChange(style: com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle) {
        _formState.value = _formState.value.copy(relationshipCardStyle = style)
    }
    fun onMonthlyToggle(v: Boolean) { _formState.value = _formState.value.copy(monthlyEnabled = v) }
    fun onAnnualToggle(v: Boolean) { _formState.value = _formState.value.copy(annualEnabled = v) }
    fun onMilestonesToggle(v: Boolean) { _formState.value = _formState.value.copy(milestonesEnabled = v) }
    fun onShowOnHomeToggle(v: Boolean) { _formState.value = _formState.value.copy(showOnHome = v) }
    fun onHighlightedToggle(v: Boolean) { _formState.value = _formState.value.copy(isHighlighted = v) }
    fun togglePin(id: String) {
        viewModelScope.launch { repository.togglePin(id) }
    }

    fun toggleArchived(id: String, isArchived: Boolean) {
        viewModelScope.launch { repository.markEventAsArchived(id, isArchived) }
    }

    fun toggleCompleted(id: String, isCompleted: Boolean) {
        viewModelScope.launch { repository.markEventAsCompleted(id, isCompleted) }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun save(existingEventId: String? = null, onSuccess: () -> Unit, onLimitReached: () -> Unit = {}) {
        val state = _formState.value
        if (state.isSaving || saveJob?.isActive == true) return
        if (state.name.isBlank()) {
            _formState.value = state.copy(error = "Informe um nome para o relacionamento.")
            return
        }
        if (state.startEpochDay > LocalDate.now().toEpochDay()) {
            _formState.value = state.copy(error = "A data de início não pode estar no futuro.")
            return
        }

        _formState.value = state.copy(isSaving = true)
        saveJob = viewModelScope.launch {
            try {
                // Guard: Free users cannot exceed FREE_EVENT_LIMIT active relationships
                val currentCount = repository.countActiveEventsByType(com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP).first()
                if (
                    existingEventId == null &&
                    !permissionsUseCase.canCreateEvent(currentCount, com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP, isPremium.value)
                ) {
                    _formState.value = _formState.value.copy(isSaving = false)
                    onLimitReached()
                    return@launch
                }

                val baseEvent = eventBeingEdited?.takeIf { it.id == existingEventId }
                val startDate = LocalDate.ofEpochDay(state.startEpochDay)
                val event = Event(
                    id = baseEvent?.id ?: existingEventId ?: UUID.randomUUID().toString(),
                    title = state.name.trim(),
                    iconName = state.iconName,
                    colorArgb = state.colorArgb,
                    targetDate = startDate,     // use start date as the "target" for compatibility
                    targetTime = null,
                    zoneId = ZoneId.systemDefault().id,
                    referenceDate = null,
                    format = CountdownFormat.DAYS,
                    direction = CountdownDirection.ELAPSED,
                    type = com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP,
                    createdAtMillis = baseEvent?.createdAtMillis ?: System.currentTimeMillis(),
                    isCompleted = baseEvent?.isCompleted ?: false,
                    isArchived = !state.showOnHome,
                    isPrivate = baseEvent?.isPrivate ?: false,
                    isPinned = state.isHighlighted,
                    coverImageUri = state.coverImageUri,
                    relationshipType = state.type,
                    relationshipStartEpochDay = state.startEpochDay,
                    relationshipMonthlyEnabled = state.monthlyEnabled,
                    relationshipAnnualEnabled = state.annualEnabled,
                    relationshipMilestonesEnabled = state.milestonesEnabled,
                    relationshipModeStyle = state.relationshipCardStyle,
                )

                if (state.isHighlighted) {
                    repository.unpinAllEventsForType(com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP)
                }

                // insertEvent handles both create and update by checking existing ID
                repository.insertEvent(event)
                if (baseEvent?.coverImageUri != null && baseEvent.coverImageUri != state.coverImageUri) {
                    com.phdev.quantofalta.core.utils.ImageStorageHelper.deleteInternalImage(
                        getApplication(),
                        baseEvent.coverImageUri,
                    )
                }
                _formState.value = _formState.value.copy(isSaving = false)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = e.message ?: "Erro ao salvar."
                )
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    fun delete(eventId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEventById(eventId)
            onSuccess()
        }
    }

    // ── Toggle monthly setting for an existing event ──────────────────────────
    fun toggleMonthly(event: Event, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateRelationshipMonthly(event.id, enabled)
        }
    }

    // ── Toggle annual setting for an existing event ───────────────────────────
    fun toggleAnnual(event: Event, enabled: Boolean) {
        viewModelScope.launch {
            repository.updateRelationshipAnnual(event.id, enabled)
        }
    }
}

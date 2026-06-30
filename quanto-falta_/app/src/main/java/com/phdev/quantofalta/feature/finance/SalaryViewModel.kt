package com.phdev.quantofalta.feature.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.core.analytics.AnalyticsManager
import com.phdev.quantofalta.core.database.EventEntity
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownFormat
import com.phdev.quantofalta.domain.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.UUID

import com.phdev.quantofalta.billing.EntitlementManager
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Job

data class CreateSalaryUiState(
    val title: String = "Meu recebimento",
    val frequency: String = "monthly", // "monthly", "biweekly", "weekly", "custom"
    val paymentDay: String = "5", // Dia do pagamento (1-31) ou dia da semana
    val customIntervalDays: String = "30",
    val paymentDateEpochDay: Long = LocalDate.now().toEpochDay(), // Data base para custom/weekly/biweekly
    val weekendRule: String = "keep", // "keep", "friday", "monday"
    val showBusinessDays: Boolean = false,
    val showOnHome: Boolean = true,
    val salaryValue: String = "", // Opcional
    
    // Novas propriedades de customização
    val iconName: String = "Money",
    val colorArgb: Int = 0xFF4CAF50.toInt(),
    val coverImageUri: String? = null,
    val isPinned: Boolean = false,
    
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    
    val salaryCardStyle: com.phdev.quantofalta.domain.model.mode.SalaryCardStyle = com.phdev.quantofalta.domain.model.mode.SalaryCardStyle.NEXT_SALARY,
    val salaryGoalTarget: String = "",
    val salaryCustomPhrase: String = ""
)

class SalaryViewModel(
    private val eventRepository: EventRepository,
    private val analyticsManager: AnalyticsManager,
    private val permissionsUseCase: com.phdev.quantofalta.domain.usecase.PermissionsUseCase
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = permissionsUseCase.isPremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _uiState = MutableStateFlow(CreateSalaryUiState())
    val uiState: StateFlow<CreateSalaryUiState> = _uiState.asStateFlow()

    private var eventBeingEdited: Event? = null
    private var loadJob: Job? = null

    fun loadForEdit(eventId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            eventRepository.getEventById(eventId).collect { event ->
                if (event != null && event.salaryFrequency != null) {
                    eventBeingEdited = event
                    _uiState.update { state ->
                        state.copy(
                            title = event.title,
                            frequency = event.salaryFrequency,
                            paymentDay = event.salaryPaymentDay?.toString() ?: "5",
                            customIntervalDays = event.salaryCustomIntervalDays?.toString() ?: "30",
                            paymentDateEpochDay = event.salaryPaymentDateEpochDay ?: LocalDate.now().toEpochDay(),
                            weekendRule = event.salaryWeekendRule ?: "keep",
                            showBusinessDays = event.salaryShowBusinessDays ?: false,
                            showOnHome = !event.isArchived,
                            salaryValue = event.salaryValue?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "",
                            iconName = event.iconName,
                            colorArgb = event.colorArgb,
                            coverImageUri = event.coverImageUri,
                            isPinned = event.isPinned,
                            salaryCardStyle = event.salaryModeStyle,
                            salaryGoalTarget = event.salaryGoalTarget?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "",
                            salaryCustomPhrase = event.salaryCustomPhrase ?: "",
                            error = null
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title, error = null) }
    }

    fun updateFrequency(frequency: String) {
        _uiState.update { it.copy(frequency = frequency) }
    }

    fun updatePaymentDay(day: String) {
        _uiState.update { it.copy(paymentDay = day) }
    }

    fun updatePaymentDate(epochDay: Long) {
        _uiState.update { it.copy(paymentDateEpochDay = epochDay) }
    }

    fun updateCustomInterval(interval: String) {
        _uiState.update { it.copy(customIntervalDays = interval) }
    }

    fun updateWeekendRule(rule: String) {
        _uiState.update { it.copy(weekendRule = rule) }
    }

    fun updateShowBusinessDays(show: Boolean) {
        _uiState.update { it.copy(showBusinessDays = show) }
    }

    fun updateShowOnHome(show: Boolean) {
        _uiState.update { it.copy(showOnHome = show) }
    }

    fun updateSalaryValue(value: String) {
        _uiState.update { it.copy(salaryValue = value) }
    }

    fun updateIcon(iconName: String) {
        _uiState.update { it.copy(iconName = iconName) }
    }

    fun updateColor(colorArgb: Int) {
        _uiState.update { it.copy(colorArgb = colorArgb) }
    }

    fun updateCoverImage(uri: String?) {
        _uiState.update { it.copy(coverImageUri = uri) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun updateIsPinned(isPinned: Boolean) {
        _uiState.update { it.copy(isPinned = isPinned) }
    }

    fun updateSalaryCardStyle(style: com.phdev.quantofalta.domain.model.mode.SalaryCardStyle) {
        _uiState.update { it.copy(salaryCardStyle = style) }
    }

    fun updateSalaryGoalTarget(goal: String) {
        _uiState.update { it.copy(salaryGoalTarget = goal) }
    }

    fun updateSalaryCustomPhrase(phrase: String) {
        _uiState.update { it.copy(salaryCustomPhrase = phrase) }
    }

    fun saveSalaryEvent(eventId: String? = null, onLimitReached: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                                val currentCount = eventRepository.countActiveEventsByType(com.phdev.quantofalta.domain.model.EventType.SALARY).first()
                                if (
                    eventId == null &&
                    !permissionsUseCase.canCreateEvent(currentCount, com.phdev.quantofalta.domain.model.EventType.SALARY, isPremium.value)
                ) {
                    onLimitReached()
                    return@launch
                }
                val currentState = _uiState.value
                if (currentState.title.isBlank()) {
                    _uiState.update { it.copy(error = "O nome não pode estar vazio.", isLoading = false) }
                    return@launch
                }
                
                val parsedPaymentDay = currentState.paymentDay.toIntOrNull() ?: 1
                val parsedCustomInterval = currentState.customIntervalDays.toIntOrNull() ?: 30
                val parsedValue = currentState.salaryValue.replace(",", ".").toDoubleOrNull()
                val parsedGoal = currentState.salaryGoalTarget.replace(",", ".").toDoubleOrNull()

                if (currentState.frequency == "monthly" && parsedPaymentDay !in 1..31) {
                    _uiState.update { it.copy(error = "Informe um dia de recebimento entre 1 e 31.", isLoading = false) }
                    return@launch
                }
                if (currentState.frequency == "custom" && parsedCustomInterval <= 0) {
                    _uiState.update { it.copy(error = "Informe um intervalo maior que zero.", isLoading = false) }
                    return@launch
                }
                if (parsedValue != null && parsedValue < 0) {
                    _uiState.update { it.copy(error = "O valor do recebimento não pode ser negativo.", isLoading = false) }
                    return@launch
                }
                if (parsedGoal != null && parsedGoal < 0) {
                    _uiState.update { it.copy(error = "A meta não pode ser negativa.", isLoading = false) }
                    return@launch
                }

                val baseEvent = eventBeingEdited ?: Event(
                    id = eventId ?: UUID.randomUUID().toString(),
                    title = "",
                    iconName = "",
                    colorArgb = 0,
                    targetDate = LocalDate.now(),
                    targetTime = null,
                    zoneId = java.time.ZoneId.systemDefault().id,
                    referenceDate = null,
                    format = CountdownFormat.DAYS,
                    direction = CountdownDirection.AUTO,
                    createdAtMillis = System.currentTimeMillis(),
                    isCompleted = false,
                    isArchived = false,
                    isPrivate = false,
                    isPinned = false,
                    coverImageUri = null,
                    relationshipType = null,
                    relationshipStartEpochDay = null
                )

                val event = baseEvent.copy(
                    title = currentState.title.trim(),
                    iconName = currentState.iconName,
                    colorArgb = currentState.colorArgb,
                    targetDate = LocalDate.now(), // Atualiza targetDate mesmo sendo ignorado por isSalaryEvent()
                    type = com.phdev.quantofalta.domain.model.EventType.SALARY,
                    isArchived = !currentState.showOnHome,
                    isPinned = currentState.isPinned,
                    coverImageUri = currentState.coverImageUri,
                    
                    salaryFrequency = currentState.frequency,
                    salaryPaymentDay = parsedPaymentDay,
                    salaryPaymentDateEpochDay = currentState.paymentDateEpochDay,
                    salaryCustomIntervalDays = parsedCustomInterval,
                    salaryWeekendRule = currentState.weekendRule,
                    salaryShowBusinessDays = currentState.showBusinessDays,
                    salaryValue = parsedValue,
                    salaryModeStyle = currentState.salaryCardStyle,
                    salaryGoalTarget = parsedGoal,
                    salaryCustomPhrase = currentState.salaryCustomPhrase.takeIf { it.isNotBlank() }
                )

                if (currentState.isPinned) {
                    eventRepository.unpinAllEventsForType(com.phdev.quantofalta.domain.model.EventType.SALARY)
                }

                eventRepository.insertEvent(event)
                
                _uiState.update { it.copy(isSuccess = true, isLoading = false) }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(error = e.message ?: "Erro ao salvar contagem", isLoading = false) }
            }
        }
    }
}

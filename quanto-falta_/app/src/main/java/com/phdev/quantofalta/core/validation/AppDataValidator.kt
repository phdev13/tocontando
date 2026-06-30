package com.phdev.quantofalta.core.validation

import com.phdev.quantofalta.core.database.EventEntity
import com.phdev.quantofalta.core.database.EventReminderEntity
import com.phdev.quantofalta.core.notifications.model.EventReminder
import com.phdev.quantofalta.core.notifications.model.TriggerType
import com.phdev.quantofalta.domain.model.CountdownDirection
import com.phdev.quantofalta.domain.model.CountdownFormat
import com.phdev.quantofalta.domain.model.Event
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

class DataValidationException(message: String) : IllegalArgumentException(message)

object AppDataValidator {
    const val MAX_TITLE_LENGTH = 120
    const val MAX_ID_LENGTH = 128
    const val MAX_ICON_NAME_LENGTH = 80
    const val MAX_REMINDERS_PER_EVENT = 32
    const val MAX_OFFSET_MINUTES = 525_600
    const val MAX_BACKUP_EVENTS = 10_000
    const val MAX_BACKUP_TIMELINES = 100_000
    const val MAX_BACKUP_REMINDERS = 100_000
    const val MAX_BACKUP_BYTES = 10L * 1024L * 1024L

    private val minimumDate: LocalDate = LocalDate.of(1900, 1, 1)
    private val maximumDate: LocalDate = LocalDate.of(2300, 12, 31)
    private val controlCharacters = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")

    fun normalizeTitle(value: String): String {
        val normalized = value.replace(controlCharacters, "").trim()
        if (normalized.isBlank()) throw DataValidationException("O nome do evento é obrigatório.")
        if (normalized.length > MAX_TITLE_LENGTH) {
            throw DataValidationException("O nome do evento deve ter no máximo $MAX_TITLE_LENGTH caracteres.")
        }
        return normalized
    }

    fun requireId(value: String, field: String = "ID"): String {
        val normalized = value.trim()
        if (normalized.isBlank() || normalized.length > MAX_ID_LENGTH || controlCharacters.containsMatchIn(normalized)) {
            throw DataValidationException("$field inválido.")
        }
        return normalized
    }

    fun requireZoneId(value: String): String {
        val normalized = value.trim()
        try {
            ZoneId.of(normalized)
        } catch (_: DateTimeException) {
            throw DataValidationException("Fuso horário inválido.")
        }
        return normalized
    }

    fun requireDate(epochDay: Long, field: String = "Data"): Long {
        val date = try {
            LocalDate.ofEpochDay(epochDay)
        } catch (_: DateTimeException) {
            throw DataValidationException("$field inválida.")
        }
        if (date.isBefore(minimumDate) || date.isAfter(maximumDate)) {
            throw DataValidationException("$field fora do intervalo permitido.")
        }
        return epochDay
    }

    fun requireTime(secondsOfDay: Int?): Int? {
        if (secondsOfDay != null && secondsOfDay !in 0..86_399) {
            throw DataValidationException("Horário inválido.")
        }
        return secondsOfDay
    }

    fun requireIconName(value: String): String {
        val normalized = value.trim()
        if (normalized.isBlank() || normalized.length > MAX_ICON_NAME_LENGTH || controlCharacters.containsMatchIn(normalized)) {
            throw DataValidationException("Ícone inválido.")
        }
        return normalized
    }

    fun validateEvent(event: Event): Event {
        requireId(event.id, "ID do evento")
        requireDate(event.targetDate.toEpochDay())
        event.referenceDate?.let { requireDate(it.toEpochDay(), "Data de referência") }
        requireZoneId(event.zoneId)
        requireTime(event.targetTime?.toSecondOfDay())
        if (event.format.requiresReferenceDate() && event.referenceDate == null) {
            throw DataValidationException("O formato escolhido exige uma data de referência.")
        }
        
        val normalizedTitle = normalizeTitle(event.title)
        val validIcon = requireIconName(event.iconName)
        val validZone = requireZoneId(event.zoneId)

        // Rule 17: "não permitir campos específicos de um modo vazarem para outro"
        val cleanedEvent = when (event.type) {
            com.phdev.quantofalta.domain.model.EventType.STANDARD -> event.copy(
                salaryFrequency = null, salaryPaymentDay = null, salaryPaymentDateEpochDay = null,
                salaryCustomIntervalDays = null, salaryWeekendRule = null, salaryShowBusinessDays = false, salaryValue = null,
                relationshipType = null, relationshipStartEpochDay = null, relationshipMonthlyEnabled = false,
                relationshipAnnualEnabled = false, relationshipMilestonesEnabled = false
            )
            com.phdev.quantofalta.domain.model.EventType.RELATIONSHIP -> {
                if (event.relationshipType == null) throw DataValidationException("Modo Relacionamento exige tipo definido.")
                event.copy(
                    salaryFrequency = null, salaryPaymentDay = null, salaryPaymentDateEpochDay = null,
                    salaryCustomIntervalDays = null, salaryWeekendRule = null, salaryShowBusinessDays = false, salaryValue = null
                )
            }
            com.phdev.quantofalta.domain.model.EventType.SALARY -> {
                if (event.salaryFrequency == null) throw DataValidationException("Modo Salário exige frequência definida.")
                event.copy(
                    relationshipType = null, relationshipStartEpochDay = null, relationshipMonthlyEnabled = false,
                    relationshipAnnualEnabled = false, relationshipMilestonesEnabled = false
                )
            }
        }

        return cleanedEvent.copy(
            title = normalizedTitle,
            iconName = validIcon,
            zoneId = validZone
        )
    }

    fun validateEventEntity(entity: EventEntity): EventEntity {
        requireId(entity.id, "ID do evento")
        requireDate(entity.targetDate)
        entity.referenceDate?.let { requireDate(it, "Data de referência") }
        requireTime(entity.targetTime)
        requireZoneId(entity.zoneId)
        if (entity.createdAtMillis <= 0L || entity.localRevision < 0 || entity.serverRevision < 0) {
            throw DataValidationException("Metadados do evento inválidos.")
        }
        val format = runCatching { CountdownFormat.valueOf(entity.format) }
            .getOrElse { throw DataValidationException("Formato de contagem inválido.") }
        if (format.requiresReferenceDate() && entity.referenceDate == null) {
            throw DataValidationException("O formato escolhido exige uma data de referência.")
        }
        if (runCatching { CountdownDirection.valueOf(entity.direction) }.isFailure) {
            throw DataValidationException("Direção da contagem inválida.")
        }
        return entity.copy(
            title = normalizeTitle(entity.title),
            iconName = requireIconName(entity.iconName),
            zoneId = requireZoneId(entity.zoneId)
        )
    }

    fun validateReminders(eventId: String, reminders: List<EventReminder>): List<EventReminder> {
        if (reminders.size > MAX_REMINDERS_PER_EVENT) {
            throw DataValidationException("Há lembretes demais para o mesmo evento.")
        }
        val seen = mutableSetOf<Pair<TriggerType, Long?>>()
        return reminders.map { reminder ->
            requireId(reminder.id, "ID do lembrete")
            if (reminder.eventId != eventId) throw DataValidationException("Lembrete associado ao evento incorreto.")
            val key = when (reminder.triggerType) {
                TriggerType.EXACT -> {
                    if (reminder.offsetMinutes != null || reminder.customDateTimeMillis != null) {
                        throw DataValidationException("Configuração do lembrete exato inválida.")
                    }
                    TriggerType.EXACT to null
                }
                TriggerType.OFFSET -> {
                    val offset = reminder.offsetMinutes
                    if (
                        offset == null || offset !in 1..MAX_OFFSET_MINUTES ||
                        reminder.customDateTimeMillis != null
                    ) {
                        throw DataValidationException("Antecedência do lembrete inválida.")
                    }
                    TriggerType.OFFSET to offset.toLong()
                }
                TriggerType.CUSTOM -> {
                    val custom = reminder.customDateTimeMillis
                    if (custom == null || custom <= 0L || reminder.offsetMinutes != null) {
                        throw DataValidationException("Data personalizada do lembrete inválida.")
                    }
                    TriggerType.CUSTOM to custom
                }
            }
            if (!seen.add(key)) throw DataValidationException("Existem lembretes duplicados.")
            reminder
        }
    }

    fun validateReminderEntity(eventId: String, entity: EventReminderEntity): EventReminderEntity {
        val type = runCatching { TriggerType.valueOf(entity.triggerType) }
            .getOrElse { throw DataValidationException("Tipo de lembrete inválido.") }
        validateReminders(
            eventId,
            listOf(
                EventReminder(
                    id = entity.id,
                    eventId = entity.eventId,
                    triggerType = type,
                    offsetMinutes = entity.offsetMinutes,
                    customDateTimeMillis = entity.customDateTimeMillis,
                    enabled = entity.enabled,
                    allowSnooze = entity.allowSnooze,
                    soundEnabled = entity.soundEnabled,
                    vibrationEnabled = entity.vibrationEnabled,
                    createdAtMillis = entity.createdAtMillis,
                    updatedAtMillis = entity.updatedAtMillis
                )
            )
        )
        if (
            entity.createdAtMillis <= 0L ||
            entity.updatedAtMillis < entity.createdAtMillis
        ) {
            throw DataValidationException("Metadados do lembrete inválidos.")
        }
        return entity
    }
}

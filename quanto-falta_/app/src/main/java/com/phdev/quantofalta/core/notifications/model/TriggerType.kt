package com.phdev.quantofalta.core.notifications.model

enum class TriggerType {
    EXACT, // No exato momento do evento
    OFFSET, // Minutos antes (ex: 1440 = 1 dia)
    CUSTOM // Data/hora especifica
}

package com.phdev.quantofalta.mock

import androidx.compose.ui.graphics.toArgb
import com.phdev.quantofalta.core.database.EventEntity
import com.phdev.quantofalta.core.designsystem.theme.Colors
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.time.LocalDate

object MockDataGenerator {
    fun generateMockEvents(): List<EventEntity> {
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val zone = java.time.ZoneId.systemDefault().id
        
        return listOf(
            EventEntity(
                id = UUID.randomUUID().toString(),
                title = "Viagem para Salvador 🏖️",
                targetDate = today.plusDays(45).toEpochDay(),
                targetTime = null,
                zoneId = zone,
                referenceDate = null,
                format = "DAYS",
                direction = "AUTO",
                createdAtMillis = now - TimeUnit.DAYS.toMillis(5),
                colorArgb = Colors[1].toArgb(), // Blue
                iconName = "BeachAccess",
                isCompleted = false,
                isArchived = false
            ),
            EventEntity(
                id = UUID.randomUUID().toString(),
                title = "Aniversário da Mãe 🎂",
                targetDate = today.plusDays(12).toEpochDay(),
                targetTime = null,
                zoneId = zone,
                referenceDate = null,
                format = "DAYS",
                direction = "AUTO",
                createdAtMillis = now - TimeUnit.DAYS.toMillis(10),
                colorArgb = Colors[5].toArgb(), // Pink
                iconName = "Cake",
                isCompleted = false,
                isArchived = false
            ),
            EventEntity(
                id = UUID.randomUUID().toString(),
                title = "Estreia do Novo Filme 🍿",
                targetDate = today.toEpochDay(),
                targetTime = java.time.LocalTime.now().plusHours(6).toSecondOfDay(),
                zoneId = zone,
                referenceDate = null,
                format = "FULL_TIME",
                direction = "AUTO",
                createdAtMillis = now - TimeUnit.DAYS.toMillis(2),
                colorArgb = Colors[3].toArgb(), // Orange
                iconName = "Movie",
                isCompleted = false,
                isArchived = false
            ),
            EventEntity(
                id = UUID.randomUUID().toString(),
                title = "Show de Rock Especial 🎸",
                targetDate = today.plusDays(90).toEpochDay(),
                targetTime = null,
                zoneId = zone,
                referenceDate = null,
                format = "WEEKS",
                direction = "AUTO",
                createdAtMillis = now - TimeUnit.DAYS.toMillis(1),
                colorArgb = Colors[0].toArgb(), // Purple
                iconName = "MusicNote",
                isCompleted = false,
                isArchived = false
            ),
            EventEntity(
                id = UUID.randomUUID().toString(),
                title = "Maratona de São Silvestre 🏃",
                targetDate = today.plusDays(200).toEpochDay(),
                targetTime = null,
                zoneId = zone,
                referenceDate = null,
                format = "DAYS",
                direction = "AUTO",
                createdAtMillis = now - TimeUnit.DAYS.toMillis(15),
                colorArgb = Colors[2].toArgb(), // Green
                iconName = "Star",
                isCompleted = false,
                isArchived = false
            )
        )
    }
}

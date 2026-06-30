package com.phdev.quantofalta.domain.model.mode

import com.phdev.quantofalta.domain.model.EventType

sealed class CardMode(
    val modeId: Int,
    val technicalName: String,
    val displayName: String,
    val allowsCoverImage: Boolean,
    val allowsCustomColor: Boolean,
    val allowsIconSelection: Boolean
) {
    object Standard : CardMode(
        modeId = 1,
        technicalName = "standard",
        displayName = "Padrão",
        allowsCoverImage = true,
        allowsCustomColor = true,
        allowsIconSelection = true
    )

    object Relationship : CardMode(
        modeId = 2,
        technicalName = "relationship",
        displayName = "Relacionamento",
        allowsCoverImage = true,
        allowsCustomColor = false,
        allowsIconSelection = false
    )

    object Salary : CardMode(
        modeId = 3,
        technicalName = "salary",
        displayName = "Finanças",
        allowsCoverImage = true,
        allowsCustomColor = true,
        allowsIconSelection = true
    )

    companion object {
        fun fromId(id: Int): CardMode {
            return when (id) {
                1 -> Standard
                2 -> Relationship
                3 -> Salary
                else -> Standard
            }
        }

        fun fromTechnicalName(name: String): CardMode {
            return when (name) {
                "standard" -> Standard
                "relationship" -> Relationship
                "salary" -> Salary
                else -> Standard
            }
        }

        fun fromEventType(type: EventType): CardMode {
            return when (type) {
                EventType.STANDARD -> Standard
                EventType.RELATIONSHIP -> Relationship
                EventType.SALARY -> Salary
            }
        }
    }
}

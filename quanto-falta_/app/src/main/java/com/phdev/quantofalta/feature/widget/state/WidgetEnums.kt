package com.phdev.quantofalta.feature.widget.state

enum class WidgetTheme {
    MINIMALIST,
    TRANSPARENT,
    COMPACT,
    FULLSCREEN;

    companion object {
        fun fromString(value: String?): WidgetTheme {
            return when (value?.lowercase()) {
                "minimalist" -> MINIMALIST
                "transparent" -> TRANSPARENT
                "compact" -> COMPACT
                "fullscreen", "image" -> FULLSCREEN
                else -> COMPACT // Fallback default
            }
        }
    }
}

enum class WidgetUnitMode {
    AUTO,
    DAYS,
    MONTHS,
    YEARS;

    companion object {
        fun fromString(value: String?): WidgetUnitMode {
            return when (value?.lowercase()) {
                "auto" -> AUTO
                "dias" -> DAYS
                "meses" -> MONTHS
                "anos" -> YEARS
                else -> AUTO
            }
        }
    }
}

package com.phdev.quantofalta.domain.model

enum class FormatTier { FREE, PREMIUM }

enum class CountdownFormat {
    // Gratuito
    DAYS,
    // Premium
    FULL_TIME,
    WEEKS,
    WEEKS_AND_DAYS,
    MONTHS,
    MONTHS_AND_DAYS,
    WORKING_DAYS,
    PERCENTAGE,
    ELAPSED_DETAILED,
    AGE;

    val tier: FormatTier
        get() = when (this) {
            DAYS -> FormatTier.FREE
            else -> FormatTier.PREMIUM
        }

    fun requiresReferenceDate(): Boolean = this == PERCENTAGE || this == ELAPSED_DETAILED || this == AGE
}

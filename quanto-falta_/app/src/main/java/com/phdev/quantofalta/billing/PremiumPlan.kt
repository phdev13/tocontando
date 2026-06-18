package com.phdev.quantofalta.billing

/**
 * Active premium entitlement types. Monthly and annual are kept only for legacy purchases.
 */
enum class PremiumPlan {
    MENSAL,
    ANUAL,
    VITALICIO,
    PERSONALIZADO,
    NONE;

    companion object {
        fun fromString(value: String?): PremiumPlan =
            entries.firstOrNull { it.name == value } ?: NONE
    }
}

/**
 * Represents a currently active premium entitlement.
 * [expiresAt] is a Unix timestamp in seconds; null means lifetime.
 */
data class ActivePremium(
    val plan: PremiumPlan,
    val expiresAt: Long?,   // null = vitalício
) {
    val isLifetime: Boolean get() = expiresAt == null

    fun isValid(): Boolean {
        if (expiresAt == null) return true
        return expiresAt > System.currentTimeMillis() / 1000
    }
}

package com.phdev.quantofalta.domain.usecase

import com.phdev.quantofalta.billing.EntitlementManager
import com.phdev.quantofalta.billing.PremiumFeature
import com.phdev.quantofalta.domain.model.EventType
import kotlinx.coroutines.flow.Flow

class PermissionsUseCase(
    val entitlementManager: EntitlementManager
) {
    val isPremium: Flow<Boolean> = entitlementManager.hasActivePremium

    fun canCreateEvent(currentCount: Int, type: EventType, isPremiumActive: Boolean): Boolean {
        if (isPremiumActive) return true
        return currentCount < PremiumFeature.FREE_EVENT_LIMIT
    }
    
    fun canUseFeature(feature: PremiumFeature, isPremiumActive: Boolean): Boolean {
        if (isPremiumActive) return true
        return false
    }
}

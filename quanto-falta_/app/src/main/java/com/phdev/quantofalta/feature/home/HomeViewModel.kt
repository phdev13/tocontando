package com.phdev.quantofalta.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.EventUiModel
import com.phdev.quantofalta.domain.model.toUiModel
import kotlinx.coroutines.delay
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.android.billingclient.api.Purchase
import com.phdev.quantofalta.billing.BillingClientWrapper
import com.phdev.quantofalta.billing.EntitlementManager

class HomeViewModel(
    private val application: android.app.Application,
    private val repository: EventRepository,
    private val billingClientWrapper: BillingClientWrapper,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = combine(
        billingClientWrapper.purchases,
        entitlementManager.activeEntitlements   // already filters expired entries
    ) { purchases, entitlements ->
        val hasActivePurchase = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val hasActiveEntitlement = entitlements.isNotEmpty() // expiresAt checked inside EntitlementManager
        hasActivePurchase || hasActiveEntitlement
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _minuteTick = kotlinx.coroutines.flow.MutableStateFlow(java.time.Instant.now())
    val minuteTick: StateFlow<java.time.Instant> = _minuteTick

    init {
        viewModelScope.launch {
            flow {
                while (true) {
                    emit(java.time.Instant.now())
                    delay(60_000L) // 1 minute
                }
            }.collect {
                _minuteTick.value = it
            }
        }
    }

    val uiState: StateFlow<ImmutableList<EventUiModel>?> = combine(
        repository.getAllEvents(),
        minuteTick
    ) { events, currentInstant ->
        events
            .filter { !it.isArchived && !it.isCompleted } // We don't filter past events automatically anymore
            .sortedBy { it.targetDate.toEpochDay() }
            .map { it.toUiModel(currentInstant, application) }
            .toImmutableList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

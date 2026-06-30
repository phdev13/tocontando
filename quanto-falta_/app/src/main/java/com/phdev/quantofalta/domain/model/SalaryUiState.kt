package com.phdev.quantofalta.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class SalaryUiState(
    val frequency: String,
    val paymentDay: Int?,
    val customIntervalDays: Int?,
    val nextPaymentEpochDay: Long,
    val daysRemaining: Int,
    val businessDaysRemaining: Int?,
    val cycleProgressPercentage: Int,
    val weekendRule: String,
    val showBusinessDays: Boolean,
    val salaryValue: Double?,
    
    // UI Formatted Strings specific for Salary Mode card
    val primaryText: String,     // e.g. "Faltam 8 dias" or "Próximo em 05/07/2026"
    val secondaryText: String,    // e.g. "6 dias úteis · ciclo 72%" or "Mensal"
    
    // Style configurations
    val salaryCardStyle: com.phdev.quantofalta.domain.model.mode.SalaryCardStyle,
    val salaryGoalTarget: Double?,
    val salaryCustomPhrase: String?
)

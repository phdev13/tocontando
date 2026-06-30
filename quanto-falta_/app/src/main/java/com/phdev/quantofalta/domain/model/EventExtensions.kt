package com.phdev.quantofalta.domain.model

import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

val Event.dateMillis: Long
    get() {
        if (this.isSalaryEvent()) {
            val uiState = com.phdev.quantofalta.core.finance.SalaryCalculator.calculate(this, java.time.LocalDate.now(java.time.ZoneId.of(this.zoneId)))
            return java.time.LocalDate.ofEpochDay(uiState.nextPaymentEpochDay)
                .atTime(java.time.LocalTime.of(23, 59, 59))
                .atZone(java.time.ZoneId.of(this.zoneId)).toInstant().toEpochMilli()
        }
        val time = this.targetTime ?: java.time.LocalTime.of(23, 59, 59)
        return this.targetDate.atTime(time).atZone(java.time.ZoneId.of(this.zoneId)).toInstant().toEpochMilli()
    }

val Event.unit: String
    get() = this.format.name

fun Event.getEventProgress(nowMillis: Long = System.currentTimeMillis()): Float {
    if (this.dateMillis <= nowMillis) return 1f // Já passou ou é agora
    if (this.createdAtMillis >= this.dateMillis) return 1f // Prevenção de erro (criado depois da data)

    val totalDuration = this.dateMillis - this.createdAtMillis
    if (totalDuration <= 0) return 1f

    val elapsed = nowMillis - this.createdAtMillis
    if (elapsed <= 0) return 0f

    val progress = elapsed.toFloat() / totalDuration.toFloat()
    return max(0f, min(1f, progress))
}

fun Event.getEmotionalContext(nowMillis: Long = System.currentTimeMillis()): String {
    val progress = getEventProgress(nowMillis)
    
    // Calcula diferença em dias truncando para o dia atual no fuso
    val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val eventDay = Calendar.getInstance().apply { timeInMillis = this@getEmotionalContext.dateMillis }
    val createdDay = Calendar.getInstance().apply { timeInMillis = this@getEmotionalContext.createdAtMillis }
    
    // Zera horas, minutos, segundos e milissegundos para cálculo puro de dias do calendário
    fun zeroTime(c: Calendar) {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }
    
    zeroTime(today)
    zeroTime(eventDay)
    zeroTime(createdDay)

    val diffMillis = eventDay.timeInMillis - today.timeInMillis
    val daysUntil = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

    val elapsedMillis = today.timeInMillis - createdDay.timeInMillis
    val daysSinceCreation = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()

    if (daysUntil < 0) return "O evento já aconteceu."
    if (daysUntil == 0) return "Chegou o grande dia!"
    if (daysUntil == 1) return "É amanhã."

    if (daysUntil in 2..5 && progress > 0.9f) return "Falta muito pouco."
    
    if (daysSinceCreation == 30) return "Há 30 dias você criou esta contagem."
    if (daysSinceCreation == 100) return "Cem dias de espera desde que você criou a contagem."
    if (daysSinceCreation == 365) return "Fez 1 ano que você criou esta contagem."
    
    if (daysSinceCreation == 0) return "A contagem regressiva começou."

    val percent = (progress * 100).toInt()
    if (percent in 50..55) return "Você já chegou na metade da espera."
    if (percent >= 80) return "Você já percorreu $percent% da espera."

    // Default fallbacks randomly or specific
    return "Falta pouco para a sua data."
}

fun Event.isTodayOrPast(nowMillis: Long = System.currentTimeMillis()): Boolean {
    val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val eventDay = Calendar.getInstance().apply { timeInMillis = this@isTodayOrPast.dateMillis }
    
    fun zeroTime(c: Calendar) {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }
    zeroTime(today)
    zeroTime(eventDay)
    
    return eventDay.timeInMillis <= today.timeInMillis
}

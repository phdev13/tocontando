package com.phdev.quantofalta.domain

import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.Event
import com.phdev.quantofalta.domain.model.FormatTier

class PremiumRequiredException : Exception("Format requires active Premium subscription")
class EventLimitExceededException : Exception("Limite de 5 eventos para esta categoria excedido no plano gratuito. Conclua, exclua ou libere espaço para continuar.")
class MissingReferenceDateException : Exception("Format requires a reference date")

class SaveEventUseCase(
    private val premiumState: PremiumState,
    private val repo: EventRepository
) {
    suspend operator fun invoke(event: Event): Result<Unit> {
        if (event.format.tier == FormatTier.PREMIUM && !premiumState.isPremium) {
            return Result.failure(PremiumRequiredException())
        }
        if (event.referenceDate == null && event.format.requiresReferenceDate()) {
            return Result.failure(MissingReferenceDateException())
        }
        
        return try {
            repo.insertEvent(event)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

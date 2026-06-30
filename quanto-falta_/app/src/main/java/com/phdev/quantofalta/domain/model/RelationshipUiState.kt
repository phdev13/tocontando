package com.phdev.quantofalta.domain.model

import androidx.compose.runtime.Immutable
import com.phdev.quantofalta.core.relationship.RelationshipCalculator

/**
 * Pre-formatted UI state for a Relationship event.
 * Computed once in the ViewModel (off main thread), never in recomposition.
 */
@Immutable
data class RelationshipUiState(
    val primaryText: String,        // "428 dias juntos"
    val secondaryText: String,      // "Próximo marco: 500 dias · faltam 72"
    val stats: RelationshipCalculator.RelationshipStats,
    val relationshipType: String,   // raw type key for display formatting
    val monthlyEnabled: Boolean,
    val annualEnabled: Boolean,
    val milestonesEnabled: Boolean,
    val startEpochDay: Long,        // to allow editing / detail display
)

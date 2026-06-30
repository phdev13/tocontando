package com.phdev.quantofalta.core.designsystem.components.edit

import androidx.compose.runtime.Composable
import com.phdev.quantofalta.domain.model.mode.RelationshipCardStyle

/**
 * Thin wrapper around [UnifiedCardStyleSelector] for the Relationship card mode.
 */
@Composable
fun RelationshipCardStyleSelector(
    selectedStyle: RelationshipCardStyle,
    isPremium: Boolean,
    onStyleSelected: (RelationshipCardStyle) -> Unit,
    onPremiumLocked: (RelationshipCardStyle) -> Unit
) {
    UnifiedCardStyleSelector(
        styles = RelationshipCardStyle.entries,
        selectedStyle = selectedStyle,
        isPremium = isPremium,
        onStyleSelected = onStyleSelected,
        onPremiumLocked = onPremiumLocked
    )
}

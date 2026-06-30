package com.phdev.quantofalta.core.designsystem.components.edit

import androidx.compose.runtime.Composable
import com.phdev.quantofalta.domain.model.mode.SalaryCardStyle

/**
 * Thin wrapper around [UnifiedCardStyleSelector] for the Salary card mode.
 */
@Composable
fun SalaryCardStyleSelector(
    selectedStyle: SalaryCardStyle,
    isPremium: Boolean,
    onStyleSelected: (SalaryCardStyle) -> Unit,
    onPremiumLocked: (SalaryCardStyle) -> Unit
) {
    UnifiedCardStyleSelector(
        styles = SalaryCardStyle.entries,
        selectedStyle = selectedStyle,
        isPremium = isPremium,
        onStyleSelected = onStyleSelected,
        onPremiumLocked = onPremiumLocked
    )
}

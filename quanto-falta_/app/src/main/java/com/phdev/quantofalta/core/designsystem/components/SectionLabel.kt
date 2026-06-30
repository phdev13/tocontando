package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.phdev.quantofalta.core.designsystem.theme.AppTypography

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

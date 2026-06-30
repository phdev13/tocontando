package com.phdev.quantofalta.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AppContentMaxWidth = 600.dp
val AppWideContentMaxWidth = 1040.dp

@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = AppContentMaxWidth,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxWidth),
            content = content
        )
    }
}

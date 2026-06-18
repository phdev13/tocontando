package com.phdev.quantofalta.feature.widget.ui

import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.compose.ui.unit.dp
import androidx.glance.unit.ColorProvider

object WidgetThemes {
    fun getContainerModifier(theme: com.phdev.quantofalta.feature.widget.state.WidgetTheme, eventColor: Color): GlanceModifier {
        return when (theme) {
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.MINIMALIST -> GlanceModifier
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.TRANSPARENT -> GlanceModifier
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT -> GlanceModifier
                .appWidgetBackground()
                .cornerRadius(16.dp)
                .background(ColorProvider(eventColor))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN -> GlanceModifier
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .background(ColorProvider(eventColor))
        }
    }
    
    fun getTextColor(theme: com.phdev.quantofalta.feature.widget.state.WidgetTheme, eventColor: Color, isPrimary: Boolean): ColorProvider {
        return when (theme) {
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.MINIMALIST -> if (isPrimary) ColorProvider(Color.DarkGray) else ColorProvider(eventColor)
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.TRANSPARENT -> if (isPrimary) ColorProvider(Color.White) else ColorProvider(Color.White.copy(alpha = 0.8f))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT -> if (isPrimary) ColorProvider(Color.White) else ColorProvider(Color.White.copy(alpha = 0.8f))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN -> if (isPrimary) ColorProvider(Color.White) else ColorProvider(Color.White.copy(alpha = 0.8f))
        }
    }
}

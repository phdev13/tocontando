package com.phdev.quantofalta.feature.widget.ui

import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceModifier
import androidx.glance.background
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.compose.ui.unit.dp
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as ColorProviderFactory

object WidgetThemes {
    fun getContainerModifier(theme: com.phdev.quantofalta.feature.widget.state.WidgetTheme, eventColor: Color): GlanceModifier {
        return when (theme) {
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.MINIMALIST -> GlanceModifier
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.TRANSPARENT -> GlanceModifier
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .background(ColorProviderFactory(day = Color(0x33000000), night = Color(0x4D000000)))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT -> GlanceModifier
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .background(ColorProviderFactory(day = eventColor, night = eventColor))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN -> GlanceModifier
                .appWidgetBackground()
                .cornerRadius(28.dp)
                .background(ColorProviderFactory(day = eventColor, night = eventColor))
        }
    }
    
    fun getTextColor(theme: com.phdev.quantofalta.feature.widget.state.WidgetTheme, eventColor: Color, isPrimary: Boolean): ColorProvider {
        return when (theme) {
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.MINIMALIST -> if (isPrimary) ColorProviderFactory(day = Color.DarkGray, night = Color.White) else ColorProviderFactory(day = eventColor, night = eventColor)
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.TRANSPARENT -> if (isPrimary) ColorProviderFactory(day = Color.White, night = Color.White) else ColorProviderFactory(day = Color.White.copy(alpha = 0.8f), night = Color.White.copy(alpha = 0.8f))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT -> if (isPrimary) ColorProviderFactory(day = Color.White, night = Color.White) else ColorProviderFactory(day = Color.White.copy(alpha = 0.85f), night = Color.White.copy(alpha = 0.85f))
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN -> if (isPrimary) ColorProviderFactory(day = Color.White, night = Color.White) else ColorProviderFactory(day = Color.White.copy(alpha = 0.85f), night = Color.White.copy(alpha = 0.85f))
        }
    }
}

package com.phdev.quantofalta.feature.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.feature.widget.EventWidgetConfigurationActivity
import com.phdev.quantofalta.feature.widget.state.WidgetState
import com.phdev.quantofalta.feature.widget.state.WidgetTheme
import com.phdev.quantofalta.feature.widget.state.WidgetUnitMode

@Composable
fun EventWidgetLayout(
    context: Context,
    state: WidgetState
) {
    val size = LocalSize.current

    when (state) {
        is WidgetState.PremiumRequired -> {
            PremiumRequiredWidget(context)
        }
        is WidgetState.Unconfigured, is WidgetState.EventUnavailable -> {
            UnconfiguredWidget(context, state)
        }
        is WidgetState.PrivateEvent -> {
            PrivateEventWidget(context, state.eventId)
        }
        is WidgetState.Error -> {
            ErrorWidget()
        }
        is WidgetState.Configured -> {
            ConfiguredEventWidget(context, state, size)
        }
    }
}

@Composable
private fun ConfiguredEventWidget(
    context: Context,
    state: WidgetState.Configured,
    size: DpSize
) {
    val currentMillis = System.currentTimeMillis()
    val isDone = state.event.isCompleted || currentMillis >= state.event.targetDate
    
    val displayUnit = if (state.unitMode == WidgetUnitMode.AUTO && !isDone) {
        TimeUtils.getAutoUnit(state.event.targetDate, currentMillis)
    } else if (state.unitMode == WidgetUnitMode.AUTO) {
        "dias"
    } else {
        state.unitMode.name.lowercase()
    }
    
    val numberStr = if (isDone) "0" else TimeUtils.calculateDifference(state.event.targetDate, currentMillis, displayUnit).toString()
    val eventColor = Color(state.event.colorArgb)

    val baseModifier = GlanceModifier
        .fillMaxSize()
        .clickable(
            actionStartActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EVENT_ID", state.event.id)
                }
            )
        )
        .then(WidgetThemes.getContainerModifier(state.theme, eventColor))

    Box(
        modifier = baseModifier.padding(if (state.theme == WidgetTheme.TRANSPARENT) 8.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            size.width < 150.dp || size.height < 100.dp -> {
                // Small
                SmallEventLayout(state, numberStr, displayUnit, isDone, eventColor)
            }
            size.width >= 250.dp && size.height >= 150.dp -> {
                // Large
                LargeEventLayout(state, numberStr, displayUnit, isDone, eventColor)
            }
            else -> {
                // Medium
                MediumEventLayout(state, numberStr, displayUnit, isDone, eventColor)
            }
        }
    }
}

@Composable
private fun SmallEventLayout(state: WidgetState.Configured, numberStr: String, displayUnit: String, isDone: Boolean, eventColor: Color) {
    val primaryColor = WidgetThemes.getTextColor(state.theme, eventColor, true)
    val secondaryColor = WidgetThemes.getTextColor(state.theme, eventColor, false)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isDone) "🎉" else numberStr,
            style = TextStyle(color = primaryColor, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
        Text(
            text = if (isDone) "Concluído" else mapUnitToPortuguese(displayUnit),
            style = TextStyle(color = secondaryColor, fontSize = 12.sp, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun MediumEventLayout(state: WidgetState.Configured, numberStr: String, displayUnit: String, isDone: Boolean, eventColor: Color) {
    val primaryColor = WidgetThemes.getTextColor(state.theme, eventColor, true)
    val secondaryColor = WidgetThemes.getTextColor(state.theme, eventColor, false)

    if (isDone) {
        Text(
            text = "A DATA CHEGOU! 🎉",
            style = TextStyle(color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
    } else {
        if (state.theme == WidgetTheme.TRANSPARENT || state.theme == WidgetTheme.COMPACT) {
             Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = numberStr,
                    style = TextStyle(color = primaryColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = mapUnitToPortuguese(displayUnit).uppercase(),
                    style = TextStyle(color = secondaryColor, fontSize = 14.sp)
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = numberStr,
                    style = TextStyle(color = primaryColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = mapUnitToPortuguese(displayUnit).uppercase(),
                    style = TextStyle(color = secondaryColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

@Composable
private fun LargeEventLayout(state: WidgetState.Configured, numberStr: String, displayUnit: String, isDone: Boolean, eventColor: Color) {
    val primaryColor = WidgetThemes.getTextColor(state.theme, eventColor, true)
    val secondaryColor = WidgetThemes.getTextColor(state.theme, eventColor, false)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isDone) {
            Text(text = "✨", style = TextStyle(fontSize = 32.sp))
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "É HOJE!",
                style = TextStyle(color = primaryColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = state.event.title,
                style = TextStyle(color = secondaryColor, fontSize = 14.sp)
            )
        } else {
            Text(
                text = state.event.title,
                style = TextStyle(color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = numberStr,
                    style = TextStyle(color = primaryColor, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = GlanceModifier.width(12.dp))
                Text(
                    text = mapUnitToPortuguese(displayUnit).uppercase(),
                    style = TextStyle(color = secondaryColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

private fun mapUnitToPortuguese(unit: String): String {
    return when(unit.lowercase()) {
        "days" -> "dias"
        "months" -> "meses"
        "years" -> "anos"
        else -> unit
    }
}

@Composable
private fun PremiumRequiredWidget(context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .background(ColorProvider(Color(0xFF2D2D2D)))
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("OPEN_PREMIUM", true)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.padding(8.dp)) {
            Text(
                text = "⭐ Premium",
                style = TextStyle(color = ColorProvider(Color(0xFFFFD700)), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Toque p/ desbloquear",
                style = TextStyle(color = ColorProvider(Color.White), textAlign = TextAlign.Center, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun UnconfiguredWidget(context: Context, state: WidgetState) {
    val message = if (state is WidgetState.EventUnavailable) "Evento indisponível\nToque p/ escolher outro" else "Toque para configurar"
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .background(ColorProvider(Color.DarkGray))
            .clickable(actionStartActivity(Intent(context, EventWidgetConfigurationActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurface, textAlign = TextAlign.Center)
        )
    }
}

@Composable
private fun PrivateEventWidget(context: Context, eventId: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .background(ColorProvider(Color.DarkGray))
            .clickable(
                actionStartActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("event_id", eventId)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.padding(8.dp)) {
            Text(text = "🔒", style = TextStyle(textAlign = TextAlign.Center, fontSize = 24.sp))
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Evento Privado",
                style = TextStyle(color = ColorProvider(Color.White), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun ErrorWidget() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(16.dp)
            .background(ColorProvider(Color.DarkGray)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Erro",
            style = TextStyle(color = GlanceTheme.colors.onSurface, textAlign = TextAlign.Center)
        )
    }
}

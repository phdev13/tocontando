package com.phdev.quantofalta.feature.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.feature.widget.EventWidgetConfigurationActivity
import com.phdev.quantofalta.feature.widget.state.WidgetState

@Composable
fun PremiumRequiredWidget(context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = Color(0xFF1E1E1E), night = Color(0xFF1E1E1E)))
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.padding(16.dp)) {
            Text(
                text = "✨",
                style = TextStyle(textAlign = TextAlign.Center, fontSize = 28.sp)
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Premium",
                style = TextStyle(color = ColorProvider(day = Color(0xFFFFD700), night = Color(0xFFFFD700)), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Toque p/ desbloquear",
                style = TextStyle(color = ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA)), textAlign = TextAlign.Center, fontSize = 12.sp)
            )
        }
    }
}

@Composable
fun UnconfiguredWidget(context: Context, state: WidgetState) {
    val message = if (state is WidgetState.EventUnavailable) "Evento indisponível\nToque p/ escolher outro" else "Toque para configurar"
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = Color(0xFF2A2A2A), night = Color(0xFF2A2A2A)))
            .clickable(actionStartActivity(Intent(context, EventWidgetConfigurationActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.padding(16.dp)) {
            Text(text = "⚙️", style = TextStyle(fontSize = 24.sp))
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = message,
                style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), textAlign = TextAlign.Center, fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun PrivateEventWidget(context: Context, eventId: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = Color(0xFF1A1A1A), night = Color(0xFF1A1A1A)))
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = GlanceModifier.padding(16.dp)) {
            Text(text = "🔒", style = TextStyle(textAlign = TextAlign.Center, fontSize = 28.sp))
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = "Evento Privado",
                style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
        }
    }
}

@Composable
fun ErrorWidget() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = Color(0xFF550000), night = Color(0xFF550000))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "⚠️", style = TextStyle(fontSize = 24.sp))
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Erro",
                style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), textAlign = TextAlign.Center)
            )
        }
    }
}

fun mapUnitToPortuguese(unit: String): String {
    return when(unit.lowercase()) {
        "days" -> "dias"
        "months" -> "meses"
        "years" -> "anos"
        else -> unit
    }
}

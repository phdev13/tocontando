package com.phdev.quantofalta.feature.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.size
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.feature.widget.state.WidgetState
import com.phdev.quantofalta.feature.widget.state.WidgetTheme

@Composable
fun StandardEventWidgetLayout(context: Context, state: WidgetState.Configured) {
    val size = LocalSize.current
    val eventColor = state.event.color

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
        .cornerRadius(20.dp)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        if (state.coverBitmap != null) {
            androidx.glance.Image(
                provider = ImageProvider(state.coverBitmap),
                contentDescription = null,
                contentScale = androidx.glance.layout.ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
            Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(day = Color.Black.copy(alpha = 0.4f), night = Color.Black.copy(alpha = 0.4f)))) {}
        } else {
            Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(day = eventColor, night = eventColor))) {}
        }
        
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(if (state.theme == WidgetTheme.TRANSPARENT || state.theme == WidgetTheme.MINIMALIST) 8.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                size.width < 150.dp || size.height < 100.dp -> {
                    SmallEventLayout(state)
                }
                size.width >= 250.dp && size.height >= 150.dp -> {
                    LargeEventLayout(state)
                }
                else -> {
                    MediumEventLayout(state)
                }
            }
        }
    }
}

@Composable
private fun SmallEventLayout(state: WidgetState.Configured) {
    val primaryColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.85f)
    val isDone = state.event.isCompleted

    Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isDone) "🎉" else state.event.primaryText,
                style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            )
            Text(
                text = state.event.secondaryText.uppercase(),
                style = TextStyle(color = ColorProvider(day = secondaryColor, night = secondaryColor), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
private fun MediumEventLayout(state: WidgetState.Configured) {
    val primaryColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.85f)
    val isDone = state.event.isCompleted

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = state.event.title,
                style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        
        if (state.event.badgeText.isNotEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(day = Color.Black.copy(alpha = 0.2f), night = Color.Black.copy(alpha = 0.2f)))
                        .cornerRadius(12.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.event.badgeText, style = TextStyle(fontSize = 12.sp, color = ColorProvider(day = Color.White, night = Color.White)))
                }
            }
        }
        
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            Column(modifier = GlanceModifier.padding(bottom = 16.dp)) {
                Text(
                    text = state.event.primaryText,
                    style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = state.event.secondaryText.uppercase(),
                    style = TextStyle(color = ColorProvider(day = secondaryColor, night = secondaryColor), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
        
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().height(4.dp).background(ColorProvider(day = primaryColor, night = primaryColor)).cornerRadius(2.dp)
            ) {}
        }
    }
}

@Composable
private fun LargeEventLayout(state: WidgetState.Configured) {
    val primaryColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.85f)

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            Text(
                text = state.event.title,
                style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        
        if (state.event.badgeText.isNotEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(day = Color.Black.copy(alpha = 0.2f), night = Color.Black.copy(alpha = 0.2f)))
                        .cornerRadius(16.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.event.badgeText, style = TextStyle(fontSize = 16.sp, color = ColorProvider(day = Color.White, night = Color.White)))
                }
            }
        }
        
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            Column(modifier = GlanceModifier.padding(bottom = 20.dp)) {
                Text(
                    text = state.event.primaryText,
                    style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 64.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = state.event.secondaryText.uppercase(),
                    style = TextStyle(color = ColorProvider(day = secondaryColor, night = secondaryColor), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }
        
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().height(6.dp).background(ColorProvider(day = primaryColor, night = primaryColor)).cornerRadius(3.dp)
            ) {}
        }
    }
}

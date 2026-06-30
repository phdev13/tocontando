package com.phdev.quantofalta.feature.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.feature.widget.state.WidgetState

@Composable
fun HeroWidgetLayout(context: Context, state: WidgetState.Configured) {
    val isDone = state.event.isCompleted
    val eventColor = state.event.color
    
    val primaryColor = Color.White
    val secondaryColor = Color.White.copy(alpha = 0.8f)

    val baseModifier = GlanceModifier
        .fillMaxSize()
        .appWidgetBackground()
        .cornerRadius(24.dp)
        .clickable(
            actionStartActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EVENT_ID", state.event.id)
                }
            )
        )

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
            modifier = GlanceModifier.fillMaxSize().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                Column {
                    Text(
                        text = state.event.title,
                        style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 24.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = state.event.date,
                        style = TextStyle(color = ColorProvider(day = secondaryColor, night = secondaryColor), fontSize = 14.sp),
                        maxLines = 1
                    )
                }
            }
            
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(day = Color.Black.copy(alpha = 0.2f), night = Color.Black.copy(alpha = 0.2f)))
                        .cornerRadius(16.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (isDone) "🎉" else "⏳", style = TextStyle(fontSize = 16.sp))
                }
            }
            
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                Column(modifier = GlanceModifier.padding(bottom = 24.dp)) {
                    Text(
                        text = state.event.primaryText,
                        style = TextStyle(color = ColorProvider(day = primaryColor, night = primaryColor), fontSize = 72.sp, fontWeight = FontWeight.Bold)
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
}

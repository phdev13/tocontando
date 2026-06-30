package com.phdev.quantofalta.feature.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
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
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.phdev.quantofalta.MainActivity
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.feature.widget.state.WidgetState

@Composable
fun ListWidgetLayout(context: Context, state: WidgetState.ListConfigured) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(24.dp)
            .background(ColorProvider(day = Color(0xFF141414), night = Color(0xFF141414)))
            .padding(16.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .width(4.dp)
                        .height(18.dp)
                        .cornerRadius(2.dp)
                        .background(ColorProvider(day = Color(0xFF6C63FF), night = Color(0xFF6C63FF)))
                ) {}
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "Próximos Eventos",
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Box(
                    modifier = GlanceModifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Novo",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFF6C63FF), night = Color(0xFF6C63FF)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = GlanceModifier.clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    putExtra("ACTION_CREATE", true)
                                }
                            )
                        ).padding(start = 8.dp)
                    )
                }
            }
            
            if (state.events.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum evento futuro.",
                        style = TextStyle(color = ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA)), fontSize = 14.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    itemsIndexed(state.events) { index, item ->
                        val isDone = item.event.isCompleted
                        val eventColor = item.event.color
                        
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(ColorProvider(day = Color(0xFF1E1E1E), night = Color(0xFF1E1E1E)))
                                .cornerRadius(16.dp)
                                .clickable(
                                    actionStartActivity(
                                        Intent(context, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            putExtra("EVENT_ID", item.event.id)
                                        }
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            
                            Box(
                                modifier = GlanceModifier
                                    .size(36.dp)
                                    .background(ColorProvider(day = eventColor.copy(alpha = 0.15f), night = eventColor.copy(alpha = 0.15f)))
                                    .cornerRadius(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.coverBitmap != null) {
                                    Image(
                                        provider = ImageProvider(item.coverBitmap),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = GlanceModifier.fillMaxSize().cornerRadius(10.dp)
                                    )
                                } else {
                                    Text(
                                        text = if (isDone) "🎉" else "⏳",
                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            
                            Spacer(modifier = GlanceModifier.width(12.dp))
                            
                            Column(modifier = GlanceModifier.fillMaxWidth()) {
                                Text(
                                    text = item.event.title,
                                    style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                
                                if (isDone) {
                                    Text(
                                        text = "CHEGOU!",
                                        style = TextStyle(color = ColorProvider(day = eventColor, night = eventColor), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = item.event.primaryText,
                                            style = TextStyle(color = ColorProvider(day = eventColor, night = eventColor), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = GlanceModifier.width(4.dp))
                                        Text(
                                            text = item.event.secondaryText.uppercase(),
                                            style = TextStyle(color = ColorProvider(day = eventColor, night = eventColor), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                            modifier = GlanceModifier.padding(bottom = 2.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = GlanceModifier.height(2.dp))
                                Text(
                                    text = item.event.date,
                                    style = TextStyle(color = ColorProvider(day = Color(0xFFAAAAAA), night = Color(0xFFAAAAAA)), fontSize = 12.sp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

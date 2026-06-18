package com.phdev.quantofalta.feature.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.phdev.quantofalta.domain.model.dateMillis
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.designsystem.components.AdaptiveIcon
import com.phdev.quantofalta.core.designsystem.theme.AppTheme
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.Event
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class EventWidgetConfigurationActivity : ComponentActivity() {

    private lateinit var eventRepository: EventRepository
    private var isPremium by mutableStateOf(false)
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = (applicationContext as ToContandoApplication)
        eventRepository = app.container.eventRepository
        val entitlementManager = app.container.entitlementManager

        GlobalScope.launch {
            isPremium = entitlementManager.hasActivePremium.first()
        }

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val themeMode by app.container.themeManager.themeState.collectAsState(initial = com.phdev.quantofalta.core.designsystem.theme.AppThemeMode.SYSTEM)
            AppTheme(themeMode = themeMode) {
                val allEvents by eventRepository.getAllEvents().collectAsState(initial = emptyList())
                val events = remember(allEvents) { allEvents.filter { !it.isPrivate } }
                var selectedEvent by remember { mutableStateOf<Event?>(null) }
                var selectedTheme by remember { mutableStateOf(com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN) }
                var selectedUnit by remember { mutableStateOf(com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.AUTO) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Configurar Widget") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Cancelar")
                                }
                            },
                            actions = {
                                if (selectedEvent != null) {
                                    Button(
                                        onClick = { onSaveConfiguration(selectedEvent!!.id, selectedTheme, selectedUnit) },
                                        enabled = isPremium || selectedTheme == com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT, // Free users can only use Compact? Let's say all widgets are premium.
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("Adicionar")
                                    }
                                }
                            }
                        )
                    }
                ) { padding ->
                    if (events.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                            Text("Nenhum evento criado ainda.")
                        }
                    } else if (selectedEvent == null) {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                            item {
                                Text(
                                    "1. Escolha o evento:",
                                    style = AppTypography.titleMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            items(events) { event ->
                                EventSelectionItem(event = event) {
                                    selectedEvent = event
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            // Preview Area
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .background(Color.Black.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                WidgetPreviewMockup(
                                    event = selectedEvent!!,
                                    themeStr = selectedTheme,
                                    unitMode = selectedUnit
                                )
                            }

                            if (!isPremium) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF3CD))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        "⭐ Assine o Premium para adicionar estes widgets magníficos à sua tela inicial!",
                                        color = Color(0xFF856404),
                                        style = AppTypography.labelMedium
                                    )
                                }
                            }

                            Text(
                                "2. Tema do Widget",
                                style = AppTypography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ThemeChip("Minimalista", com.phdev.quantofalta.feature.widget.state.WidgetTheme.MINIMALIST, selectedTheme) { selectedTheme = it }
                                ThemeChip("Transparente", com.phdev.quantofalta.feature.widget.state.WidgetTheme.TRANSPARENT, selectedTheme) { selectedTheme = it }
                                ThemeChip("Compacto", com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT, selectedTheme) { selectedTheme = it }
                                ThemeChip("Premium Full", com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN, selectedTheme) { selectedTheme = it }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "3. Unidade de Tempo",
                                style = AppTypography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ThemeChip("Automático", com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.AUTO, selectedUnit) { selectedUnit = it }
                                ThemeChip("Dias", com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.DAYS, selectedUnit) { selectedUnit = it }
                                ThemeChip("Meses", com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.MONTHS, selectedUnit) { selectedUnit = it }
                                ThemeChip("Anos", com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.YEARS, selectedUnit) { selectedUnit = it }
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun onSaveConfiguration(eventId: String, theme: com.phdev.quantofalta.feature.widget.state.WidgetTheme, unit: com.phdev.quantofalta.feature.widget.state.WidgetUnitMode) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        
        GlobalScope.launch {
            updateAppWidgetState(this@EventWidgetConfigurationActivity, glanceId) { prefs ->
                prefs[EventWidget.eventIdKey] = eventId
                prefs[EventWidget.themeKey] = theme.name
                prefs[EventWidget.unitModeKey] = unit.name
            }
            EventWidget().update(this@EventWidgetConfigurationActivity, glanceId)
            WidgetUpdateScheduler(this@EventWidgetConfigurationActivity).updateAllWidgets()
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun <T> ThemeChip(label: String, value: T, selectedValue: T, onSelect: (T) -> Unit) {
    val isSelected = value == selectedValue
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = { onSelect(value) }
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EventSelectionItem(event: Event, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(event.colorArgb)),
            contentAlignment = Alignment.Center
        ) {
            AdaptiveIcon(
                iconName = event.iconName,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = event.title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun WidgetPreviewMockup(event: Event, themeStr: com.phdev.quantofalta.feature.widget.state.WidgetTheme, unitMode: com.phdev.quantofalta.feature.widget.state.WidgetUnitMode) {
    val currentMillis = System.currentTimeMillis()
    val isDone = event.isCompleted || currentMillis >= event.dateMillis
    val displayUnit = if (unitMode == com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.AUTO && !isDone) TimeUtils.getAutoUnit(event.dateMillis, currentMillis) else if(unitMode == com.phdev.quantofalta.feature.widget.state.WidgetUnitMode.AUTO) "dias" else unitMode.name.lowercase()
    
    // Convert to portuguese for preview
    val displayUnitPt = when(displayUnit.lowercase()) {
        "days" -> "dias"
        "months" -> "meses"
        "years" -> "anos"
        else -> displayUnit
    }
    
    val numberStr = if (isDone) "0" else TimeUtils.calculateDifference(event.dateMillis, currentMillis, displayUnit).toString()
    val eventColor = Color(event.colorArgb)

    Box(
        modifier = Modifier
            .size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF333333)).clip(RoundedCornerShape(24.dp)))

        when (themeStr) {
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.MINIMALIST -> {
                if (isDone) {
                    Text("A DATA CHEGOU! 🎉", color = eventColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(numberStr, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                        Text(displayUnitPt.uppercase(), color = eventColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.TRANSPARENT -> {
                if (isDone) {
                    Text("🎉 Concluído!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(numberStr, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(displayUnitPt.uppercase(), color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                    }
                }
            }
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp).clip(RoundedCornerShape(16.dp)).background(eventColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isDone) "🎉" else numberStr, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(if (isDone) "Concluído" else displayUnitPt, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
            com.phdev.quantofalta.feature.widget.state.WidgetTheme.FULLSCREEN -> {
                Box(
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)).background(eventColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        if (isDone) {
                            Text("✨", fontSize = 32.sp)
                            Text("É HOJE!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(event.title, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                        } else {
                            Text(event.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(numberStr, color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                            Text(displayUnitPt.uppercase(), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

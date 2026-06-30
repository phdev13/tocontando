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
import com.phdev.quantofalta.domain.model.toUiModel
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.designsystem.components.AdaptiveIcon
import com.phdev.quantofalta.core.designsystem.theme.AppTheme
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.time.TimeUtils
import com.phdev.quantofalta.data.repository.EventRepository
import com.phdev.quantofalta.domain.model.Event
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class EventWidgetConfigurationActivity : ComponentActivity() {

    private lateinit var eventRepository: EventRepository
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = (applicationContext as ToContandoApplication)
        eventRepository = app.container.eventRepository

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
            // Collect isPremium reactively so the UI updates immediately if the
            // user purchases a plan while this Activity is open.
            val isPremium by app.container.entitlementManager.hasActivePremium
                .collectAsState(initial = false)

            
            val widgetManager = remember { AppWidgetManager.getInstance(this@EventWidgetConfigurationActivity) }
            val providerInfo = remember { widgetManager.getAppWidgetInfo(appWidgetId) }
            val providerClass = providerInfo?.provider?.className
            
            val isMicro = providerClass == MicroWidgetReceiver::class.java.name
            val isHero = providerClass == HeroWidgetReceiver::class.java.name
            val isList = providerClass == ListWidgetReceiver::class.java.name
            val isClassic = providerClass == EventWidgetReceiver::class.java.name
            
            AppTheme(themeMode = themeMode) {
                val allEvents by eventRepository.getAllEvents().collectAsState(initial = emptyList())
                val events = remember(allEvents) { allEvents.filter { !it.isPrivate } }
                var selectedEvent by remember { mutableStateOf<Event?>(null) }
                
                // Only unit mode selection remains
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
                                        onClick = { onSaveConfiguration(selectedEvent!!.id, com.phdev.quantofalta.feature.widget.state.WidgetTheme.COMPACT, selectedUnit) },
                                        enabled = isPremium || isMicro,
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                        .background(Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    WidgetPreviewMockup(
                                        event = selectedEvent!!,
                                        unitMode = selectedUnit,
                                        isMicro = isMicro
                                    )
                                }

                            if (!isPremium && !isMicro) {
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
                                "2. Unidade de Tempo",
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

    private fun onSaveConfiguration(eventId: String, theme: com.phdev.quantofalta.feature.widget.state.WidgetTheme, unit: com.phdev.quantofalta.feature.widget.state.WidgetUnitMode) {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        
        lifecycleScope.launch {
            updateAppWidgetState(this@EventWidgetConfigurationActivity, glanceId) { prefs ->
                prefs[EventWidget.eventIdKey] = eventId
                prefs[EventWidget.themeKey] = theme.name
                prefs[EventWidget.unitModeKey] = unit.name
            }
            
            val widgetManager = AppWidgetManager.getInstance(this@EventWidgetConfigurationActivity)
            val providerInfo = widgetManager.getAppWidgetInfo(appWidgetId)
            val providerClass = providerInfo?.provider?.className
            
            val widgetInstance = when (providerClass) {
                MicroWidgetReceiver::class.java.name -> MicroWidget()
                HeroWidgetReceiver::class.java.name -> HeroWidget()
                ListWidgetReceiver::class.java.name -> ListWidget()
                else -> EventWidget()
            }
            
            widgetInstance.update(this@EventWidgetConfigurationActivity, glanceId)
            WidgetUpdateScheduler(this@EventWidgetConfigurationActivity).updateAllWidgets()
            
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
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
fun WidgetPreviewMockup(event: Event, unitMode: com.phdev.quantofalta.feature.widget.state.WidgetUnitMode, isMicro: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isPremiumCardsEnabled = com.phdev.quantofalta.core.config.AppConfigManager.isPremiumCardsEnabled(context)
    val uiModel = remember(event) { event.toUiModel(context = context) }
    
    val isDone = uiModel.isCompleted || uiModel.eventState == com.phdev.quantofalta.domain.model.EventState.COMPLETED
    val eventColor = uiModel.color
    val textColor = Color.White

    val backgroundModifier = Modifier.background(eventColor)

    if (isMicro) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(backgroundModifier)
        ) {
            if (isPremiumCardsEnabled && uiModel.coverImageUri != null) {
                coil.compose.AsyncImage(
                    model = uiModel.coverImageUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Text(
                        text = uiModel.title,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                        Text(if (isDone) "🎉" else uiModel.primaryText, color = textColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(uiModel.secondaryText.uppercase(), color = textColor.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(textColor))
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(backgroundModifier)
        ) {
            if (isPremiumCardsEnabled && uiModel.coverImageUri != null) {
                coil.compose.AsyncImage(
                    model = uiModel.coverImageUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            }
            
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiModel.title,
                        style = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isDone) "🎉" else "⏳", fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = uiModel.primaryText,
                        style = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = uiModel.secondaryText.uppercase(),
                        style = androidx.compose.ui.text.TextStyle(color = textColor.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(textColor.copy(alpha = 0.3f))) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(textColor))
                }
            }
        }
    }
}

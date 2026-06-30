package com.phdev.quantofalta.feature.testers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.phdev.quantofalta.R
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.testers.Tester
import com.phdev.quantofalta.core.testers.TestersState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestersScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ToContandoApplication).container
    val testersManager = appContainer.testersManager
    val coroutineScope = rememberCoroutineScope()

    val state by testersManager.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            testersManager.loadAndSync()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agradecimentos aos Testers", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {}
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val currentState = state) {
                is TestersState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is TestersState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Falha ao carregar testers. Verifique sua conexão.",
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { coroutineScope.launch { testersManager.loadAndSync() } }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                is TestersState.Content -> {
                    if (currentState.testers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhum tester encontrado ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    "Essas pessoas incríveis testaram e ajudaram a construir o Tô Contando ❤️",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "Patrocinado por",
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(id = R.drawable.logolk),
                                            contentDescription = "Lonkis Logo",
                                            modifier = Modifier.height(48.dp)
                                        )
                                    }
                                }
                            }
                            items(currentState.testers, key = { "tester_${it.id}" }) { tester ->
                                TesterCard(tester)
                            }
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TesterCard(tester: Tester) {
    val containerColor = if (tester.isFeatured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (tester.isFeatured) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar
            if (tester.avatarUrl == "❤️") {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("❤️", fontSize = 24.sp)
                }
            } else if (tester.avatarUrl != null) {
                AsyncImage(
                    model = tester.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = tester.displayName.split(" ").take(2).joinToString("") { it.take(1) }.uppercase()
                    Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tester.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (tester.isFeatured) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("⭐")
                    }
                }
                if (tester.nickname != null) {
                    Text(
                        text = "@${tester.nickname}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (tester.badgeKey != null) {
                        BadgeView(getBadgeName(tester.badgeKey), getBadgeColor(tester.badgeKey))
                    }
                    if (tester.participationVersion != null) {
                        BadgeView("v${tester.participationVersion}", MaterialTheme.colorScheme.secondary)
                    }
                }

                if (tester.message != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\"${tester.message}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeView(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun getBadgeName(key: String): String {
    return when (key) {
        "initial_tester" -> "Tester Inicial"
        "beta_tester" -> "Beta Tester"
        "bug_hunter" -> "Caçador de Bugs"
        "ui_tester" -> "Tester de Interface"
        "performance_tester" -> "Tester de Performance"
        "contributor" -> "Colaborador"
        else -> key
    }
}

@Composable
fun getBadgeColor(key: String): Color {
    return when (key) {
        "initial_tester" -> Color(0xFFE91E63) // Pink
        "beta_tester" -> Color(0xFF2196F3) // Blue
        "bug_hunter" -> Color(0xFFFF9800) // Orange
        "ui_tester" -> Color(0xFF9C27B0) // Purple
        "performance_tester" -> Color(0xFF4CAF50) // Green
        "contributor" -> Color(0xFF00BCD4) // Cyan
        else -> MaterialTheme.colorScheme.primary
    }
}

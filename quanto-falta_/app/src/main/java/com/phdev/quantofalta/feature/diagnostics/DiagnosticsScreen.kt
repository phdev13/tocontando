package com.phdev.quantofalta.feature.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.database.PerformanceEntity
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.telemetry.TelemetrySyncWorker
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ToContandoApplication).container
    val scope = rememberCoroutineScope()
    
    var metrics by remember { mutableStateOf<List<PerformanceEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadMetrics() {
        scope.launch {
            metrics = appContainer.performanceDao.getAllMetrics()
        }
    }

    LaunchedEffect(Unit) {
        loadMetrics()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Diagnósticos (Debug)",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(AppSpacing.medium)
        ) {
            Button(
                onClick = {
                    isLoading = true
                    val workRequest = OneTimeWorkRequestBuilder<TelemetrySyncWorker>().build()
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        "ManualPerformanceSync",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                    
                    // Polling para simular o resultado do worker (em produção deveria observar o status do WorkManager)
                    scope.launch {
                        kotlinx.coroutines.delay(2000)
                        loadMetrics()
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Filled.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forçar Sincronização Agora")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Gerando e enviando Trace Completo (Mock)...", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gerar e Enviar Trace Completo")
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Métricas pendentes no banco local: ${metrics.size}",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(metrics.size) { index ->
                    val metric = metrics[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Screen: ${metric.screenName}", style = MaterialTheme.typography.bodyLarge)
                            Text("Type: ${metric.metricType}", style = MaterialTheme.typography.bodyMedium)
                            if (metric.metricType == "JANK") {
                                Text("Jank Frames: ${metric.jankFrames} / Total: ${metric.totalFrames}")
                            } else {
                                Text("Duration: ${metric.durationMs}ms")
                            }
                        }
                    }
                }
            }
        }
    }
}

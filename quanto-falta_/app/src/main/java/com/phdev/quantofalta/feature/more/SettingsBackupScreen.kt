package com.phdev.quantofalta.feature.more

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.database.BackupManager
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle
import kotlinx.coroutines.launch

@Composable
fun SettingsBackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ToContandoApplication
    val manager = remember { BackupManager(context, app.container.database) }
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = manager.exportToUri(uri)
                Toast.makeText(context, if (ok) "Backup exportado com sucesso." else "Não foi possível exportar o backup.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingImport = uri
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Backup e restauração",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { SettingsSectionTitle("Backup local") }
            item {
                SettingsItem(
                    title = "Exportar backup",
                    description = "Salva seus eventos, lembretes e histórico em um arquivo JSON.",
                    icon = Icons.Filled.Upload,
                    onClick = { exportLauncher.launch("to-contando-backup.json") }
                )
            }
            item {
                SettingsItem(
                    title = "Restaurar backup",
                    description = "Importa um arquivo criado anteriormente pelo aplicativo.",
                    icon = Icons.Filled.Download,
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                )
            }
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restaurar este backup?") },
            text = { Text("Eventos com o mesmo identificador poderão ser atualizados pelos dados do arquivo.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val ok = manager.importFromUri(uri)
                        Toast.makeText(context, if (ok) "Backup restaurado com sucesso." else "Arquivo de backup inválido.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) { Text("Cancelar") }
            }
        )
    }
}

package com.phdev.quantofalta.feature.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phdev.quantofalta.core.AppViewModelProvider
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle

@Composable
fun SettingsClearDataScreen(
    onBack: () -> Unit,
    viewModel: MoreViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var confirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Apagar todos os eventos",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { SettingsSectionTitle("Dados locais") }
            item {
                SettingsItem(
                    title = "Apagar eventos do aparelho",
                    description = "Remove todos os eventos, lembretes e históricos deste aparelho. Esta ação não pode ser desfeita.",
                    icon = Icons.Filled.DeleteForever,
                    iconColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    titleColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    onClick = { confirm = true }
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Apagar todos os eventos?") },
            text = { Text("Eventos, lembretes e dados relacionados serão removidos deste aparelho e marcados para exclusão na sincronização.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirm = false
                        viewModel.clearAllData()
                        onBack()
                    }
                ) { Text("Apagar", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancelar") }
            }
        )
    }
}

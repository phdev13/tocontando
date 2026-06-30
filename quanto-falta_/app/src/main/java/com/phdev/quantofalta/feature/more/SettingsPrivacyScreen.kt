package com.phdev.quantofalta.feature.more

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle
import com.phdev.quantofalta.core.designsystem.components.SettingsToggleItem
import kotlinx.coroutines.launch

@Composable
fun SettingsPrivacyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = (context.applicationContext as ToContandoApplication).container.privacySettings
    val usage by settings.shareUsageData.collectAsStateWithLifecycle(initialValue = true)
    val performance by settings.sharePerformanceData.collectAsStateWithLifecycle(initialValue = true)
    val errors by settings.shareErrorReports.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Privacidade e termos",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { SettingsSectionTitle("Dados de diagnóstico") }
            item {
                SettingsToggleItem(
                    title = "Dados de uso",
                    description = "Ajuda a entender quais recursos são utilizados. Não inclui o conteúdo dos eventos.",
                    icon = Icons.Filled.Analytics,
                    checked = usage,
                    onCheckedChange = { scope.launch { settings.setShareUsageData(it) } }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Dados de desempenho",
                    description = "Envia tempos de inicialização e informações de fluidez.",
                    icon = Icons.Filled.Speed,
                    checked = performance,
                    onCheckedChange = { scope.launch { settings.setSharePerformanceData(it) } }
                )
            }
            item {
                SettingsToggleItem(
                    title = "Relatórios de erro",
                    description = "Permite o envio de diagnósticos técnicos quando necessário.",
                    icon = Icons.Filled.BugReport,
                    checked = errors,
                    onCheckedChange = { scope.launch { settings.setShareErrorReports(it) } }
                )
            }
            item { SettingsSectionTitle("Documentos") }
            item {
                SettingsItem(
                    title = "Política de privacidade",
                    icon = Icons.Filled.Description,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.SITE_BASE_URL}/privacy"))) }
                )
            }
            item {
                SettingsItem(
                    title = "Termos de uso",
                    icon = Icons.Filled.Description,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("${BuildConfig.SITE_BASE_URL}/terms"))) }
                )
            }
        }
    }
}

package com.phdev.quantofalta.feature.more

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.config.AppConfigManager
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle
import com.phdev.quantofalta.core.ota.OtaState
import com.phdev.quantofalta.core.ota.ui.installApk
import kotlinx.coroutines.launch

@Composable
fun SettingsUpdatesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = (context.applicationContext as ToContandoApplication).container.otaManager
    val state by manager.otaState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val otaEnabled = AppConfigManager.isOtaEnabled(context)

    // Human-readable status line
    val statusDescription = if (!otaEnabled) {
        if (BuildConfig.OTA_UPDATES_SUPPORTED) {
            "Atualizacoes OTA desativadas pela configuracao do servidor"
        } else {
            "Atualizacoes gerenciadas pela Google Play"
        }
    } else when (val current = state) {
        OtaState.Idle            -> "Nenhuma atualização pendente"
        OtaState.Checking        -> "Verificando atualizações..."
        is OtaState.UpdateAvailable ->
            "Versão ${current.info.versionName} disponível${current.info.apkSizeBytes?.let { " · ${formatMb(it)}" } ?: ""}"
        is OtaState.DownloadingBackground ->
            "Baixando em background... ${current.progressPercent}%"
        is OtaState.Downloading  -> "Baixando... ${current.progressPercent}%"
        is OtaState.ReadyToInstall -> "Versão ${current.info.versionName} pronta para instalar ✅"
        is OtaState.Installing   -> "Instalando..."
        is OtaState.Error        -> "Não foi possível baixar a atualização"
    }

    // Action label for the tappable item
    val actionTitle = if (!otaEnabled) {
        if (BuildConfig.FLAVOR == "playStore") "Ver na Google Play" else "OTA desativado"
    } else when (state) {
        is OtaState.ReadyToInstall -> "Instalar agora"
        is OtaState.Downloading, is OtaState.DownloadingBackground -> "Baixando..."
        is OtaState.Checking -> "Verificando..."
        is OtaState.Installing -> "Instalando..."
        else -> "Verificar atualizações"
    }

    val actionIcon = when (state) {
        is OtaState.ReadyToInstall -> Icons.Filled.InstallMobile
        is OtaState.Downloading, is OtaState.DownloadingBackground -> Icons.Filled.CloudDownload
        else -> Icons.Filled.SystemUpdate
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Atualizações",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            item { SettingsSectionTitle("Versão instalada") }
            item {
                SettingsItem(
                    title = "Tô Contando ${BuildConfig.VERSION_NAME}",
                    description = "Código da versão ${BuildConfig.VERSION_CODE}",
                    icon = Icons.Filled.SystemUpdate,
                )
            }
            item { SettingsSectionTitle("Status") }
            item {
                SettingsItem(
                    title = actionTitle,
                    description = statusDescription,
                    icon = actionIcon,
                    onClick = {
                        if (!otaEnabled && BuildConfig.FLAVOR == "playStore") {
                            val market = Uri.parse("market://details?id=${context.packageName}")
                            val web = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, market)) }
                                .onFailure { context.startActivity(Intent(Intent.ACTION_VIEW, web)) }
                        } else if (!otaEnabled) {
                            Toast.makeText(
                                context,
                                "Atualizacoes OTA estao desativadas no momento.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            scope.launch {
                                when (val current = state) {
                                    is OtaState.UpdateAvailable -> manager.startDownload(current.info)
                                    is OtaState.ReadyToInstall -> {
                                        manager.onInstallLaunched(current.info, current.apkPath)
                                        installApk(context, current.apkPath)
                                    }
                                    // Busy states — no-op (tapping does nothing)
                                    is OtaState.Downloading,
                                    is OtaState.DownloadingBackground,
                                    is OtaState.Checking,
                                    is OtaState.Installing -> Unit
                                    // Idle / Error → re-check
                                    else -> {
                                        val found = manager.checkForUpdates()
                                        if (!found) {
                                            Toast.makeText(
                                                context,
                                                "Você já está usando a versão mais recente.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun formatMb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 10) String.format("%.1f MB", mb) else "${mb.toInt()} MB"
}

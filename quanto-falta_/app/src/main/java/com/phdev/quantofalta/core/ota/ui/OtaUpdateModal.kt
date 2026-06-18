package com.phdev.quantofalta.core.ota.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.phdev.quantofalta.core.designsystem.theme.BlueSecondary
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import com.phdev.quantofalta.core.ota.OtaError
import com.phdev.quantofalta.core.ota.OtaState
import com.phdev.quantofalta.core.ota.OtaUpdateInfo
import java.io.File

/**
 * OTA update modal following Tô Contando design language.
 * Purple/blue theme, smooth animations, mandatory vs optional flow.
 */
@Composable
fun OtaUpdateModal(
    otaState: OtaState,
    onUpdate: () -> Unit,
    onDefer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val visible = otaState is OtaState.UpdateAvailable ||
            otaState is OtaState.ReadyToInstall ||
            otaState is OtaState.Downloading ||
            otaState is OtaState.Error

    if (!visible) return

    Dialog(
        onDismissRequest = {
            val info = (otaState as? OtaState.UpdateAvailable)?.info
                ?: (otaState as? OtaState.ReadyToInstall)?.info
            if (info?.mandatory == true) return@Dialog // cannot dismiss mandatory
            onDefer()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (otaState) {
                is OtaState.UpdateAvailable -> UpdateContent(
                    info = otaState.info,
                    isDownloading = false,
                    progress = 0,
                    onUpdate = onUpdate,
                    onDefer = onDefer,
                    context = context,
                )
                is OtaState.Downloading -> UpdateContent(
                    info = null,
                    isDownloading = true,
                    progress = otaState.progressPercent,
                    onUpdate = {},
                    onDefer = {},
                    context = context,
                )
                is OtaState.ReadyToInstall -> UpdateContent(
                    info = otaState.info,
                    isDownloading = false,
                    progress = 100,
                    apkPath = otaState.apkPath,
                    onUpdate = { installApk(context, otaState.apkPath) },
                    onDefer = onDefer,
                    context = context,
                )
                is OtaState.Error -> ErrorContent(reason = otaState.reason, onRetry = onUpdate, onDismiss = onDismiss)
                else -> {}
            }
        }
    }
}

@Composable
private fun UpdateContent(
    info: OtaUpdateInfo?,
    isDownloading: Boolean,
    progress: Int,
    apkPath: String? = null,
    onUpdate: () -> Unit,
    onDefer: () -> Unit,
    context: Context,
) {
    var showChangelog by remember { mutableStateOf(false) }
    val isMandatory = info?.mandatory == true

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon + gradient header
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(PurplePrimary, BlueSecondary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SystemUpdateAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Text(
            text = info?.title ?: "Baixando atualização…",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (info != null) {
            Text(
                text = "Versão ${info.versionName}${info.apkSizeBytes?.let { " · ${it / (1024 * 1024)} MB" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = info.summary.ifBlank { "Nova versão disponível do Tô Contando" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Progress bar (always visible when downloading)
        if (isDownloading || progress > 0) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val animPct by animateFloatAsState(progress / 100f, spring(), label = "progress")
                LinearProgressIndicator(
                    progress = { animPct },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = PurplePrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                if (isDownloading) {
                    Text("Baixando… $progress%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // Changelog (collapsible)
        if (info != null && info.changelog.isNotEmpty()) {
            TextButton(onClick = { showChangelog = !showChangelog }) {
                Text(if (showChangelog) "Ocultar novidades" else "Ver novidades", color = PurplePrimary)
            }
            AnimatedVisibility(visible = showChangelog) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    info.changelog.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("•", color = PurplePrimary, fontWeight = FontWeight.Bold)
                            Text(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action buttons
        if (!isDownloading) {
            Button(
                onClick = onUpdate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (progress >= 100) "Instalar agora" else "Atualizar agora", fontWeight = FontWeight.SemiBold)
            }

            if (!isMandatory) {
                OutlinedButton(
                    onClick = onDefer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Depois")
                }
            } else {
                Text(
                    text = "⚠️ Esta atualização é obrigatória.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(reason: OtaError, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Text("Falha na atualização", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = when (reason) {
                OtaError.NETWORK_UNAVAILABLE -> "Verifique sua conexão e tente novamente."
                OtaError.DOWNLOAD_FAILED -> "Erro ao baixar a atualização. Tente novamente."
                OtaError.VALIDATION_FAILED -> "O arquivo de atualização está corrompido."
                OtaError.INSUFFICIENT_STORAGE -> "Armazenamento insuficiente. Libere espaço e tente novamente."
                OtaError.INVALID_APK -> "Arquivo de atualização inválido."
                OtaError.LINK_EXPIRED -> "O link de download expirou. Tente novamente."
                else -> "Ocorreu um erro inesperado."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Fechar") }
            Button(onClick = onRetry, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Tentar novamente") }
        }
    }
}

fun installApk(context: Context, apkPath: String) {
    val file = File(apkPath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.ota.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
}

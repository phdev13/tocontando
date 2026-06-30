package com.phdev.quantofalta.core.ota.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
 * OTA Update Modal — shown only for states that require user interaction.
 *
 * States that show a modal:
 *  - [OtaState.UpdateAvailable]  → "available, no URL / user needs to know" (rare — usually we auto-download)
 *  - [OtaState.Downloading]      → user-initiated download from Settings (shows in-modal progress)
 *  - [OtaState.ReadyToInstall]   → user tapped notification or opened app with OTA_READY flag
 *  - [OtaState.Installing]       → keeps modal open while system installer is in foreground
 *  - [OtaState.Error]            → shows error + retry
 *
 * States that do NOT show a modal:
 *  - [OtaState.Idle]                 → nothing to show
 *  - [OtaState.Checking]             → silent background check
 *  - [OtaState.DownloadingBackground]→ silent background download with system notification
 */
@Composable
fun OtaUpdateModal(
    otaState: OtaState,
    onUpdate: () -> Unit,
    onInstall: (apkPath: String) -> Unit,
    onDefer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = otaState is OtaState.UpdateAvailable ||
        otaState is OtaState.Downloading ||
        otaState is OtaState.ReadyToInstall ||
        otaState is OtaState.Installing ||
        otaState is OtaState.Error

    if (!visible) return

    // Mandatory updates cannot be dismissed
    val isMandatory = when (otaState) {
        is OtaState.UpdateAvailable -> otaState.info.mandatory
        is OtaState.ReadyToInstall  -> otaState.info.mandatory
        is OtaState.Installing      -> otaState.info.mandatory
        else -> false
    }

    Dialog(
        onDismissRequest = {
            if (isMandatory) return@Dialog
            onDefer()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory,
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = otaState,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.95f))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.95f))
                },
                label = "OtaModalContent",
            ) { state ->
                when (state) {
                    is OtaState.UpdateAvailable -> UpdateAvailableContent(
                        info = state.info,
                        onUpdate = onUpdate,
                        onDefer = onDefer,
                    )
                    is OtaState.Downloading -> DownloadProgressContent(
                        versionName = null,
                        progress = state.progressPercent,
                    )
                    is OtaState.ReadyToInstall -> ReadyToInstallContent(
                        info = state.info,
                        onInstall = { onInstall(state.apkPath) },
                        onDefer = onDefer,
                    )
                    is OtaState.Installing -> InstallingContent(
                        info = state.info,
                    )
                    is OtaState.Error -> ErrorContent(
                        reason = state.reason,
                        onRetry = onUpdate,
                        onDismiss = onDismiss,
                    )
                    else -> Unit
                }
            }
        }
    }
}

// ─────────────────────────── Content blocks ───────────────────────────────

@Composable
private fun UpdateAvailableContent(
    info: OtaUpdateInfo,
    onUpdate: () -> Unit,
    onDefer: () -> Unit,
) {
    var showChangelog by remember(info.versionCode) { mutableStateOf(true) }
    val changelogItems = remember(info) { sanitizeChangelog(info) }

    ModalCard {
        UpdateIcon()
        Text(
            text = info.title.ifBlank { "Nova versão disponível" },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Versão ${info.versionName}${info.apkSizeBytes?.let { " · ${formatSizeMb(it)}" } ?: ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = info.summary.ifBlank { "Uma nova versão do Tô Contando está disponível." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ChangelogSection(changelogItems, showChangelog, onToggle = { showChangelog = !showChangelog })

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onUpdate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(12.dp),
        ) { Text("Baixar e instalar", fontWeight = FontWeight.SemiBold) }

        if (!info.mandatory) {
            OutlinedButton(
                onClick = onDefer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Depois") }
        } else {
            MandatoryNote()
        }
    }
}

@Composable
private fun DownloadProgressContent(versionName: String?, progress: Int) {
    val animPct by animateFloatAsState(
        targetValue = progress / 100f,
        animationSpec = tween(200),
        label = "downloadProgress",
    )
    ModalCard {
        UpdateIcon()
        Text(
            text = "Baixando atualização${if (versionName != null) " $versionName" else ""}…",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Aguarde, não feche o app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LinearProgressIndicator(
                progress = { animPct },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = PurplePrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                "$progress%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ReadyToInstallContent(
    info: OtaUpdateInfo,
    onInstall: () -> Unit,
    onDefer: () -> Unit,
) {
    var showChangelog by remember(info.versionCode) { mutableStateOf(false) }
    val changelogItems = remember(info) { sanitizeChangelog(info) }

    ModalCard {
        // Checkmark icon instead of download icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(PurplePrimary, BlueSecondary))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp),
            )
        }

        Text(
            text = "Pronto para instalar!",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Versão ${info.versionName} baixada e validada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = "O app vai reiniciar para aplicar a atualização.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (changelogItems.isNotEmpty()) {
            ChangelogSection(changelogItems, showChangelog, onToggle = { showChangelog = !showChangelog })
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onInstall,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(12.dp),
        ) { Text("Instalar agora", fontWeight = FontWeight.SemiBold) }

        if (!info.mandatory) {
            OutlinedButton(
                onClick = onDefer,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Depois") }
        } else {
            MandatoryNote()
        }
    }
}

@Composable
private fun InstallingContent(info: OtaUpdateInfo) {
    ModalCard {
        UpdateIcon()
        Text(
            text = "Instalando ${info.versionName}…",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Siga as instruções do instalador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = PurplePrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun ErrorContent(reason: OtaError, onRetry: () -> Unit, onDismiss: () -> Unit) {
    ModalCard {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Text("Falha na atualização", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = when (reason) {
                OtaError.NETWORK_UNAVAILABLE  -> "Verifique sua conexão e tente novamente."
                OtaError.DOWNLOAD_FAILED      -> "Erro ao baixar a atualização. Tente novamente."
                OtaError.VALIDATION_FAILED    -> "O arquivo de atualização está corrompido."
                OtaError.INSUFFICIENT_STORAGE -> "Armazenamento insuficiente. Libere espaço e tente novamente."
                OtaError.INVALID_APK          -> "Arquivo de atualização inválido."
                OtaError.LINK_EXPIRED         -> "O link de download expirou. Tente novamente."
                else                          -> "Ocorreu um erro inesperado."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Fechar") }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            ) { Text("Tentar novamente") }
        }
    }
}

// ─────────────────────────── Shared composables ───────────────────────────

@Composable
private fun ModalCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = { content() },
    )
}

@Composable
private fun UpdateIcon() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PurplePrimary, BlueSecondary))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SystemUpdateAlt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
private fun MandatoryNote() {
    Text(
        text = "Esta atualização é obrigatória para continuar usando o app.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun ChangelogSection(items: List<String>, expanded: Boolean, onToggle: () -> Unit) {
    if (items.isEmpty()) return
    TextButton(onClick = onToggle) {
        Text(if (expanded) "Ocultar novidades" else "Ver novidades", color = PurplePrimary)
    }
    AnimatedVisibility(visible = expanded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•", color = PurplePrimary, fontWeight = FontWeight.Bold)
                    Text(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─────────────────────────── Helpers ──────────────────────────────────────

private fun sanitizeChangelog(info: OtaUpdateInfo): List<String> =
    info.changelog
        .map { it.trim().removePrefix("-").removePrefix("*").removePrefix("•").trim() }
        .filter { it.isNotBlank() }
        .take(8)
        .ifEmpty {
            listOf("Melhorias e correções para deixar o app mais confiável.")
        }

private fun formatSizeMb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 10) String.format("%.1f MB", mb) else "${mb.toInt()} MB"
}

/**
 * Launches the system package installer for [apkPath].
 * Uses the dedicated OTA FileProvider authority — must match AndroidManifest.xml.
 */
fun installApk(context: Context, apkPath: String) {
    try {
        val file = File(apkPath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.ota.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("OtaUpdate", "Failed to start install intent", e)
        android.widget.Toast.makeText(context, "Erro ao iniciar instalação: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}

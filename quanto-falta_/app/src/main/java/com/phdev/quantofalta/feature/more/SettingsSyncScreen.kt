package com.phdev.quantofalta.feature.more

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSyncScreen(
    viewModel: SyncViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    var otpCode by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Conta e Sincronização",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
                when (val state = uiState) {
                    is SyncUiState.Initial -> {
                        // ── Email input ──────────────────────────────────────
                        LoginStep(
                            email = email,
                            onEmailChanged = viewModel::onEmailChanged,
                            onRequestOtp = viewModel::requestOtp
                        )
                    }

                    is SyncUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PurplePrimary)
                        }
                    }

                    is SyncUiState.OtpRequested -> {
                        // ── OTP verification ─────────────────────────────────
                        OtpStep(
                            email = email,
                            otpCode = otpCode,
                            onOtpChanged = { otpCode = it.filter(Char::isDigit).take(6) },
                            onVerify = { viewModel.verifyOtp(otpCode) },
                            onBack = { viewModel.onEmailChanged(email); viewModel.requestOtp() }
                        )
                    }

                    is SyncUiState.Authenticated -> {
                        // ── Main authenticated dashboard ──────────────────────
                        AuthenticatedSection(
                            syncStatus = syncStatus,
                            onSyncNow = viewModel::syncNow,
                            onRestoreEvents = { confirmRestore = true },
                            onLogout = { confirmLogout = true },
                            onClearError = viewModel::clearError
                        )
                    }

                    is SyncUiState.Error -> {
                        ErrorStep(
                            message = state.message,
                            onRetry = {
                                viewModel.onEmailChanged(email)
                                viewModel.requestOtp()
                            },
                            onBack = { viewModel.logout() }
                        )
                    }
                }
        }
    }

    // ── Confirmation Dialogs ──────────────────────────────────────────────────
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            icon = { Icon(Icons.Filled.CloudDownload, null, tint = PurplePrimary) },
            title = { Text("Restaurar eventos da nuvem?") },
            text = {
                Text(
                    "Todos os seus eventos serão substituídos pela versão mais recente da nuvem. Esta ação não pode ser desfeita.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmRestore = false; viewModel.restoreEvents() },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancelar") }
            }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            icon = { Icon(Icons.Filled.Logout, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Sair da conta?") },
            text = {
                Text(
                    "Seus dados locais serão mantidos, mas a sincronização automática será desativada até que você faça login novamente.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmLogout = false; viewModel.logout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────



@Composable
private fun LoginStep(email: String, onEmailChanged: (String) -> Unit, onRequestOtp: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SyncHeaderCard(
            icon = Icons.Filled.AccountCircle,
            title = "Conectar conta",
            subtitle = "Digite seu e-mail para receber o código de acesso e ativar a sincronização."
        )
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            label = { Text("E-mail") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary)
        )
        Button(
            onClick = onRequestOtp,
            enabled = email.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Receber Código", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OtpStep(
    email: String,
    otpCode: String,
    onOtpChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SyncHeaderCard(
            icon = Icons.Filled.MarkEmailRead,
            title = "Verifique seu e-mail",
            subtitle = "Enviamos um código de 6 dígitos para\n$email"
        )
        OutlinedTextField(
            value = otpCode,
            onValueChange = onOtpChanged,
            label = { Text("Código de verificação") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurplePrimary),
            placeholder = { Text("000000") }
        )
        Button(
            onClick = onVerify,
            enabled = otpCode.length == 6,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Verified, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Verificar", fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Usar outro e-mail")
        }
    }
}

@Composable
private fun AuthenticatedSection(
    syncStatus: SyncStatus,
    onSyncNow: () -> Unit,
    onRestoreEvents: () -> Unit,
    onLogout: () -> Unit,
    onClearError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Card
        SyncStatusCard(syncStatus = syncStatus)

        // Error Banner
        AnimatedVisibility(
            visible = syncStatus.lastError != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            syncStatus.lastError?.let { error ->
                ErrorBanner(message = error, onDismiss = onClearError)
            }
        }

        // Actions
        SyncActionButton(
            isSyncing = syncStatus.isSyncing,
            onClick = onSyncNow
        )

        OutlinedButton(
            onClick = onRestoreEvents,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !syncStatus.isSyncing,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurplePrimary),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(PurplePrimary.copy(alpha = 0.4f))
            )
        ) {
            Icon(Icons.Filled.CloudDownload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Restaurar da Nuvem", fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(4.dp))

        // Info items
        InfoRow(icon = Icons.Filled.AutoMode, label = "Sincronização automática", value = "A cada 6 horas")
        InfoRow(icon = Icons.Filled.Shield, label = "Dados criptografados", value = "Fim-a-fim")

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Logout, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Sair da Conta", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SyncStatusCard(syncStatus: SyncStatus) {
    val containerColor = when {
        syncStatus.isSyncing -> PurplePrimary.copy(alpha = 0.08f)
        syncStatus.lastError != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        syncStatus.lastSyncMillis != null -> Color(0xFF34C759).copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon / spinner
            if (syncStatus.isSyncing) {
                val rotation by rememberInfiniteTransition(label = "sync_rotation").animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                    label = "rotate"
                )
                Icon(
                    Icons.Filled.Sync, null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(32.dp).rotate(rotation)
                )
            } else if (syncStatus.lastError != null) {
                Icon(Icons.Filled.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
            } else if (syncStatus.lastSyncMillis != null) {
                Icon(Icons.Filled.CloudDone, null, tint = Color(0xFF34C759), modifier = Modifier.size(32.dp))
            } else {
                Icon(Icons.Filled.Cloud, null, tint = PurplePrimary, modifier = Modifier.size(32.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        syncStatus.isSyncing -> "Sincronizando..."
                        syncStatus.lastError != null -> "Falha na sincronização"
                        syncStatus.lastSyncMillis != null -> "Sincronizado ✓"
                        else -> "Pronto para sincronizar"
                    },
                    style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        syncStatus.isSyncing -> PurplePrimary
                        syncStatus.lastError != null -> MaterialTheme.colorScheme.error
                        syncStatus.lastSyncMillis != null -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = when {
                        syncStatus.isSyncing -> "Enviando e recebendo alterações..."
                        syncStatus.lastSyncMillis != null -> {
                            val sdf = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
                            "Última sync: ${sdf.format(Date(syncStatus.lastSyncMillis))}"
                        }
                        else -> "Toque em 'Sincronizar Agora' para começar"
                    },
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyncActionButton(isSyncing: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isSyncing,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PurplePrimary,
            disabledContainerColor = PurplePrimary.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (isSyncing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Sincronizando...", fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Filled.Sync, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sincronizar Agora", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Error, null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp).padding(top = 1.dp)
            )
            Text(
                text = message,
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    Icons.Filled.Close, "Fechar",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ErrorStep(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SyncHeaderCard(
            icon = Icons.Filled.Error,
            iconTint = MaterialTheme.colorScheme.error,
            title = "Algo deu errado",
            subtitle = message
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Tentar novamente", fontWeight = FontWeight.SemiBold) }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Voltar ao início")
        }
    }
}

@Composable
private fun SyncHeaderCard(
    icon: ImageVector,
    iconTint: Color = PurplePrimary,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(32.dp))
        }
        Text(title, style = AppTypography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp))
        Text(
            subtitle,
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Text(label, style = AppTypography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

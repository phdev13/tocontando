package com.phdev.quantofalta.feature.premium

import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.billing.Entitlement
import com.phdev.quantofalta.billing.EntitlementManager
import com.phdev.quantofalta.core.network.ApiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemCodeScreen(
    entitlementManager: EntitlementManager,
    onBack: () -> Unit,
    onCodeRedeemed: () -> Unit,
) {
    // Capture context OUTSIDE the coroutine (Compose rule)
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resgatar código Premium") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Success state ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = isSuccess,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Spacer(Modifier.height(32.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = successMessage ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Você já pode usar todos os recursos Premium!",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Input state ────────────────────────────────────────────────
            if (!isSuccess) {
                Text(
                    text = "Insira o seu código promocional para desbloquear o acesso Premium.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { raw ->
                        errorMessage = null
                        code = raw.uppercase().filter { it.isLetterOrDigit() || it == '-' }
                    },
                    label = { Text("Código de ativação") },
                    placeholder = { Text("Ex: QF-MEN-ABCD23") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    enabled = !isLoading,
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        // Capture installationId here, outside IO dispatcher (safe)
                        val installationId = Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ANDROID_ID,
                        ) ?: "unknown_device"

                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null

                            try {
                                val payload = JSONObject().apply {
                                    put("code", code)
                                    put("installationId", installationId)
                                    put("platform", "ANDROID")
                                    put("appVersion", BuildConfig.VERSION_NAME)
                                }

                                val response = ApiClient.post("/api/v1/app/premium/activate-token", payload)
                                val responseJson = runCatching { JSONObject(response.body) }.getOrNull()
                                    ?: JSONObject()
                                val dataJson = ApiClient.unwrapDataObject(response.body)

                                if (response.isSuccess() && responseJson.optBoolean("success", false)) {
                                    val entObj = dataJson.optJSONObject("entitlement")
                                    if (entObj != null) {
                                        val expiresAt = if (!entObj.isNull("expiresAt")) entObj.getLong("expiresAt") else null
                                        val planType = entObj.optString("planType", "")
                                        val durationDays = if (!entObj.isNull("durationDays")) entObj.getInt("durationDays") else null

                                        // Persist the entitlement locally
                                        entitlementManager.addEntitlement(
                                            Entitlement(
                                                id = entObj.getString("id"),
                                                planType = planType.ifBlank { null },
                                                features = entObj.optString("features").ifBlank { null },
                                                expiresAt = expiresAt,
                                            )
                                        )

                                        // Build friendly message based on plan type
                                        successMessage = when (planType) {
                                            "MENSAL", "ANUAL" -> {
                                                val days = expiresAt?.let {
                                                    ((it - System.currentTimeMillis() / 1000) / 86400).coerceAtLeast(1)
                                                }
                                                if (days != null) "Parabéns! Você desbloqueou $days dias\nde acesso Premium. Aproveite! 🎉"
                                                else "Parabéns! Você desbloqueou acesso Premium temporário. 🎉"
                                            }
                                            "VITALICIO"    -> "Incrível! Você desbloqueou o acesso\nPremium Vitalício — para sempre! ✨"
                                            "PERSONALIZADO" -> {
                                                val dias = durationDays ?: expiresAt?.let {
                                                    ((it - System.currentTimeMillis() / 1000) / 86400).coerceAtLeast(1).toInt()
                                                }
                                                if (dias != null) "Parabéns! Você desbloqueou $dias dias\nde acesso Premium. Aproveite! 🎉"
                                                else "Parabéns! Você desbloqueou acesso Premium. 🎉"
                                            }
                                            else -> {
                                                if (expiresAt == null) {
                                                    "Incrível! Você desbloqueou o acesso\nPremium Vitalício — para sempre! ✨"
                                                } else {
                                                    val days = ((expiresAt - System.currentTimeMillis() / 1000) / 86400).coerceAtLeast(1)
                                                    "Parabéns! Você desbloqueou $days dias\nde acesso Premium. Aproveite! 🎉"
                                                }
                                            }
                                        }
                                        isSuccess = true
                                    } else {
                                        errorMessage = ApiClient.errorMessage(response.body, "Erro desconhecido. Tente novamente.")
                                    }
                                } else {
                                    errorMessage = ApiClient.errorMessage(response.body, "Codigo invalido ou expirado.")
                                }

                            } catch (e: IOException) {
                                errorMessage = "Sem conexão. Verifique sua internet e tente novamente."
                            } catch (e: Exception) {
                                errorMessage = "Erro inesperado: ${e.localizedMessage}"
                            } finally {
                                isLoading = false
                            }

                            // Auto-navigate after showing success for 2.5s
                            if (isSuccess) {
                                delay(2500)
                                onCodeRedeemed()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = code.length >= 4 && !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Verificando...")
                    } else {
                        Text("Resgatar código", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

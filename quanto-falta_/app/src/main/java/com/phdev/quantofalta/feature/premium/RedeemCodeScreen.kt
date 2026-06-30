package com.phdev.quantofalta.feature.premium

import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.billing.Entitlement
import com.phdev.quantofalta.billing.EntitlementManager
import com.phdev.quantofalta.billing.PremiumApi
import com.phdev.quantofalta.core.designsystem.components.AdaptiveContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemCodeScreen(
    entitlementManager: EntitlementManager,
    onBack: () -> Unit,
    onCodeRedeemed: () -> Unit,
    onNavigateToRecover: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    // Bloqueio de print removido conforme solicitado

    val coroutineScope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(entitlementManager.getSavedEmail() ?: "") }
    
    // States
    var needsEmailStep by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun getInstallationId() = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID,
    ) ?: "unknown_device"

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
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxWidth = 560.dp
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        text = "Premium Ativado!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Seu código foi vinculado com sucesso. Você já pode usar todos os recursos Premium!",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Input state ────────────────────────────────────────────────
            if (!isSuccess) {
                Text(
                    text = "Insira a sua Chave de Licença para desbloquear o acesso Premium.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { raw ->
                        errorMessage = null
                        code = raw.uppercase()
                            .filter { it.isLetterOrDigit() || it == '-' }
                            .take(64)
                    },
                    label = { Text("Código de acesso") },
                    placeholder = { Text("Ex: QF-123-456") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg ->
                        { Text(msg, color = MaterialTheme.colorScheme.error) }
                    },
                    enabled = !isLoading && !needsEmailStep,
                )
                
                if (errorMessage == "Esse código já está vinculado a um e-mail.") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNavigateToRecover,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Recuperar acesso")
                    }
                }

                AnimatedVisibility(visible = needsEmailStep) {
                    Column {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                errorMessage = null
                                email = it 
                            },
                            label = { Text("E-mail para vincular acesso") },
                            placeholder = { Text("Ex: seu@email.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            enabled = !isLoading,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Obrigatório. Seu acesso Premium ficará vinculado a este e-mail para sempre. Se você perder o código, nós reenviaremos para cá.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            val installationId = getInstallationId()
                            val cleanCode = code.trim('-')

                            if (!needsEmailStep) {
                                // Step 1: Validate Code
                                val res = PremiumApi.validateCode(cleanCode, installationId, BuildConfig.VERSION_NAME)
                                if (res.success && res.codeValid && res.needsEmail) {
                                    needsEmailStep = true
                                } else {
                                    when (res.error) {
                                        "CODE_INVALID" -> errorMessage = "Código inválido ou não encontrado."
                                        "CODE_ALREADY_LINKED" -> errorMessage = "Esse código já está vinculado a um e-mail."
                                        else -> errorMessage = "Não foi possível validar o código. Tente novamente."
                                    }
                                }
                            } else {
                                // Step 2: Link Email
                                if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    errorMessage = "Por favor, informe um e-mail válido."
                                    isLoading = false
                                    return@launch
                                }
                                val res = PremiumApi.linkEmail(cleanCode, email, installationId, BuildConfig.VERSION_NAME)
                                if (res.success && res.premiumActive) {
                                    val entId = res.entitlementId ?: "ent_${System.currentTimeMillis()}"
                                    entitlementManager.addEntitlement(
                                        Entitlement(
                                            id = entId,
                                            planType = res.planType,
                                            features = null,
                                            expiresAt = res.expiresAt
                                        )
                                    )
                                    entitlementManager.setSavedEmail(email)
                                    isSuccess = true
                                    delay(2500)
                                    onCodeRedeemed()
                                } else {
                                    when (res.error) {
                                        "CODE_INVALID" -> errorMessage = "Código inválido."
                                        "CODE_ALREADY_LINKED" -> errorMessage = "Esse código já está vinculado a um e-mail."
                                        else -> errorMessage = "Ocorreu um erro ao vincular o e-mail."
                                    }
                                }
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = code.trim('-').length >= 4 && !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(if (needsEmailStep) "Vincular e ativar" else "Validar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        }
    }
}

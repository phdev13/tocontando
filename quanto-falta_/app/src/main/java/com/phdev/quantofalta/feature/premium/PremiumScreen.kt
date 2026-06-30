package com.phdev.quantofalta.feature.premium

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.billing.BillingClientWrapper
import com.phdev.quantofalta.billing.BillingStatus
import com.phdev.quantofalta.core.designsystem.components.AdaptiveContent
import com.phdev.quantofalta.core.designsystem.theme.DeepPurplePrimary
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import kotlinx.coroutines.launch

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun Context.getAppContainer(): com.phdev.quantofalta.core.AppContainer? {
    return (applicationContext as? com.phdev.quantofalta.ToContandoApplication)?.container
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    billingClientWrapper: BillingClientWrapper,
    onDismiss: () -> Unit,
    onNavigateToTicket: () -> Unit,
    onNavigateToRedeem: () -> Unit,
    onNavigateToRecover: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(Unit) {
        coroutineScope.launch {
            context.getAppContainer()?.entitlementManager?.syncWithServer(context)
        }
        onDispose { }
    }

    val billingStatus by billingClientWrapper.billingStatus.collectAsState()

    val entitlementManager = context.getAppContainer()?.entitlementManager
    val hasActivePremium by entitlementManager?.hasActivePremium?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }
    val savedEmail = entitlementManager?.getSavedEmail()

    var showLinkEmailDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (hasActivePremium) "Premium ativo" else "Premium",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        AdaptiveContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            maxWidth = 620.dp
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Estado: Premium já ativo ────────────────────────────────────
            if (hasActivePremium) {
                PremiumActiveState(
                    savedEmail = savedEmail,
                    billingStatus = billingStatus,
                    onSyncNow = {
                        coroutineScope.launch {
                            context.getAppContainer()?.entitlementManager?.syncWithServer(context)
                        }
                    },
                    onLinkEmail = { showLinkEmailDialog = true },
                    onNavigateToSupport = onNavigateToRecover
                )
            } else {
                // ── Estado: Free ────────────────────────────────────────────
                PremiumOfferCard()

                BenefitsSection()

                // Status de billing (só quando relevante)
                AnimatedVisibility(
                    visible = billingStatus !is BillingStatus.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    BillingStatusBanner(status = billingStatus)
                }

                PremiumActionsSection(
                    onNavigateToTicket = onNavigateToTicket,
                    onNavigateToRedeem = onNavigateToRedeem,
                    onNavigateToRecover = onNavigateToRecover
                )
            }
        }
        }
    }

    if (showLinkEmailDialog && entitlementManager != null) {
        LinkEmailDialog(
            entitlementManager = entitlementManager,
            onDismiss = { showLinkEmailDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Estado: Premium Ativo
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumActiveState(
    savedEmail: String?,
    billingStatus: BillingStatus,
    onSyncNow: () -> Unit,
    onLinkEmail: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Badge de Premium ativo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(listOf(PurplePrimary, DeepPurplePrimary))
                )
                .padding(24.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Você é Premium!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Acesso vitalício ativo neste dispositivo.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
                if (!savedEmail.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = savedEmail,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Aviso: sem email vinculado
        if (savedEmail.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Proteja seu acesso", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Seu Premium não está vinculado a um e-mail. Se reinstalar o app, perderá o acesso.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onLinkEmail,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Vincular e-mail agora", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Sincronizar status
        val isSyncing = billingStatus is BillingStatus.Restoring
        OutlinedButton(
            onClick = onSyncNow,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isSyncing
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Sincronizando...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Verificar status")
            }
        }

        // Suporte
        TextButton(
            onClick = onNavigateToSupport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Precisa de ajuda? Falar com suporte", fontSize = 13.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Card de Oferta
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumOfferCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(PurplePrimary, DeepPurplePrimary)))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Premium vitalício",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Apoie o Tô Contando e desbloqueie todos os recursos avançados para sempre.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    "Pagamento único · Sem renovação",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Benefícios (compacta)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BenefitsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("O que está incluído", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BenefitRow("Fotos e capas personalizadas nos cards")
            BenefitRow("Paleta completa de cores e todos os ícones")
            BenefitRow("Formatos avançados de contagem")
            BenefitRow("Personalização visual avançada dos cards")
            BenefitRow("Todos os recursos atuais e futuros")
        }
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PurplePrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ações Principais (Free)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumActionsSection(
    onNavigateToTicket: () -> Unit,
    onNavigateToRedeem: () -> Unit,
    onNavigateToRecover: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Botão primário — Apoiar projeto
        Button(
            onClick = onNavigateToTicket,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text("Apoiar projeto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        Text(
            "Após o atendimento, você recebe um código para ativar o Premium no app.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        // Card: Tenho um código
        ActionCard(
            icon = Icons.Default.Key,
            title = "Tenho um código",
            subtitle = "Use o token que você recebeu para ativar seu Premium.",
            onClick = onNavigateToRedeem
        )

        // Card: Já sou Premium
        ActionCard(
            icon = Icons.Default.Email,
            title = "Já sou Premium",
            subtitle = "Recupere o acesso usando o e-mail cadastrado na ativação.",
            onClick = onNavigateToRecover
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = PurplePrimary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner de status de billing (só quando relevante)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BillingStatusBanner(status: BillingStatus) {
    val message = when (status) {
        BillingStatus.PurchasePending -> "Compra pendente. O Premium será liberado após a confirmação do pagamento."
        BillingStatus.PurchaseCompleted -> "Compra concluída. Premium Vitalício ativado!"
        BillingStatus.PurchaseCancelled -> "Compra cancelada. Nenhuma cobrança foi feita."
        BillingStatus.RestoreCompleted -> "Compra restaurada com sucesso."
        BillingStatus.RestoreEmpty -> "Nenhuma compra encontrada nesta conta Google."
        BillingStatus.AlreadyPremium -> "Você já possui acesso Premium ativo."
        is BillingStatus.ConnectionFailed -> status.message
        is BillingStatus.PurchaseFailed -> status.message
        else -> null
    } ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val isLoading = status is BillingStatus.Restoring || status is BillingStatus.PurchaseInProgress
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(text = message, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

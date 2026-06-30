package com.phdev.quantofalta.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdev.quantofalta.ToContandoApplication
import com.phdev.quantofalta.core.analytics.InstallationManager
import com.phdev.quantofalta.core.designsystem.components.AppTopBar
import com.phdev.quantofalta.core.designsystem.components.SettingsItem
import com.phdev.quantofalta.core.designsystem.components.SettingsSectionTitle
import com.phdev.quantofalta.core.utils.SupportEmailUtils
import com.phdev.quantofalta.feature.feedback.FeedbackModal
import com.phdev.quantofalta.feature.feedback.FeedbackSubmitResult
import com.phdev.quantofalta.feature.premiumticket.PremiumTicketClient
import kotlinx.coroutines.launch

@Composable
fun SettingsSupportScreen(
    onBack: () -> Unit,
    onOpenTickets: () -> Unit,
    onRecoverPremium: () -> Unit,
    onOpenSync: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as ToContandoApplication
    val scope = rememberCoroutineScope()
    val ticketClient = remember { PremiumTicketClient(context) }
    val authManager = remember { app.container.authManager }
    val isPremium by app.container.entitlementManager.hasActivePremium.collectAsStateWithLifecycle(initialValue = false)

    var showFeedback by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var submitResult by remember { mutableStateOf<FeedbackSubmitResult?>(null) }
    var ticketStatus by remember { mutableStateOf<String?>(null) }
    var installId by remember { mutableStateOf<String?>(InstallationManager.getCachedIdOrNull()) }

    LaunchedEffect(Unit) {
        installId = InstallationManager.getOrCreateId(context)
        ticketStatus = ticketClient.getActiveTicketStatus()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Suporte e tickets",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item {
                SupportStatusPanel(
                    isPremium = isPremium,
                    email = authManager.getEmail() ?: app.container.entitlementManager.getSavedEmail(),
                    ticketStatus = ticketStatus,
                    installationId = installId,
                    onOpenTickets = onOpenTickets,
                    onRecoverPremium = onRecoverPremium
                )
            }

            item { SettingsSectionTitle("Resolver rápido") }
            item {
                SettingsItem(
                    title = "Recuperar Premium",
                    description = "Receba um código por e-mail e restaure o acesso neste aparelho.",
                    icon = Icons.Filled.LockOpen,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = onRecoverPremium,
                    action = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp)) }
                )
            }
            item {
                SettingsItem(
                    title = "Sincronizar conta",
                    description = "Ao verificar seu e-mail, o app também busca Premium vinculado a ele.",
                    icon = Icons.Filled.CloudSync,
                    onClick = onOpenSync,
                    action = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp)) }
                )
            }
            item {
                SettingsItem(
                    title = "Abrir ou acompanhar ticket",
                    description = "Use o atendimento do app para Premium, pagamentos, erros e dados.",
                    icon = Icons.Filled.Chat,
                    onClick = onOpenTickets,
                    action = { Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp)) }
                )
            }

            item { SettingsSectionTitle("Assuntos") }
            item {
                SupportTopicRow(
                    topics = listOf(
                        SupportTopic("E-mail não chegou", Icons.Filled.MarkEmailRead),
                        SupportTopic("Compra ou token", Icons.Filled.Payment),
                        SupportTopic("Erro no app", Icons.Filled.BugReport),
                        SupportTopic("Histórico do ticket", Icons.Filled.History)
                    )
                )
            }

            item { SettingsSectionTitle("Contato") }
            item {
                SettingsItem(
                    title = "Enviar feedback",
                    description = "Sugestões e comentários rápidos sobre o app.",
                    icon = Icons.Filled.Feedback,
                    onClick = {
                        submitResult = null
                        showFeedback = true
                    }
                )
            }
            item {
                SettingsItem(
                    title = "Enviar e-mail",
                    description = "contato@tocontando.com.br",
                    icon = Icons.Filled.Email,
                    onClick = { SupportEmailUtils.openSupportEmail(context) }
                )
            }
        }
    }

    if (showFeedback) {
        FeedbackModal(
            onDismiss = {
                showFeedback = false
                submitResult = null
            },
            onSubmit = { data ->
                submitting = true
                scope.launch {
                    val success = app.container.feedbackManager.submit(data)
                    app.container.smartFeedbackManager.recordSubmitted()
                    submitting = false
                    submitResult = if (success) FeedbackSubmitResult.SUCCESS else FeedbackSubmitResult.QUEUED
                }
            },
            isSubmitting = submitting,
            submitResult = submitResult,
            sourceScreen = "settings_support"
        )
    }
}

@Composable
private fun SupportStatusPanel(
    isPremium: Boolean,
    email: String?,
    ticketStatus: String?,
    installationId: String?,
    onOpenTickets: () -> Unit,
    onRecoverPremium: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Central de suporte",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tickets, recuperação de Premium e problemas de conta ficam aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (isPremium) "Premium ativo" else "Plano gratuito") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Verified,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isPremium) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(ticketStatus?.let(::ticketStatusLabel) ?: "Sem ticket aberto") }
                )
            }

            Text(
                text = "E-mail: ${email?.takeIf { it.isNotBlank() } ?: "não verificado"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Instalação: ${installationId?.take(8) ?: "carregando"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenTickets,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Chat, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tickets")
                }
                TextButton(
                    onClick = onRecoverPremium,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Recuperar Premium")
                }
            }
        }
    }
}

private data class SupportTopic(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun SupportTopicRow(topics: List<SupportTopic>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        topics.chunked(2).forEach { rowTopics ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowTopics.forEach { topic ->
                    TopicChip(topic = topic, modifier = Modifier.weight(1f))
                }
                if (rowTopics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopicChip(topic: SupportTopic, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(topic.icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(topic.label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun ticketStatusLabel(status: String): String = when (status) {
    "aguardando_admin" -> "Ticket em análise"
    "aguardando_usuario" -> "Suporte respondeu"
    "aguardando_pagamento" -> "Aguardando pagamento"
    "pagamento_confirmado" -> "Pagamento confirmado"
    "token_enviado" -> "Ação necessária"
    "concluido" -> "Ticket concluído"
    "cancelado" -> "Ticket cancelado"
    "fechado" -> "Ticket fechado"
    else -> "Ticket aberto"
}

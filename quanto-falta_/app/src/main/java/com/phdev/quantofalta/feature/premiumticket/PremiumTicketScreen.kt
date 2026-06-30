package com.phdev.quantofalta.feature.premiumticket

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import com.phdev.quantofalta.core.designsystem.theme.DeepPurplePrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportTicketScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val client = remember { PremiumTicketClient(context) }
    val auth = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // States: 0 = Loading, 1 = No Ticket Form, 2 = Ticket Active
    var screenState by remember { mutableIntStateOf(0) }
    
    var conversationId by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<TicketMessage>>(emptyList()) }
    var status by remember { mutableStateOf("NEW") }
    var protocolo by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedProof by remember { mutableStateOf<Uri?>(null) }
    
    var formNome by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf(auth.getEmail() ?: "") }
    var formMensagem by remember { mutableStateOf("") }
    
    fun showError(msg: String?) {
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    suspend fun refresh() {
        val id = conversationId ?: return
        val result = client.fetchMessages(id)
        if (result.messages.isNotEmpty()) messages = (messages + result.messages).distinctBy { it.id }.sortedBy { it.createdAt }
        status = result.status
        protocolo = result.protocol
        
        // Mark as read when opened
        if (status == "aguardando_usuario" || status == "token_enviado") {
            runCatching { client.markAsRead(id) }
        }
    }

    suspend fun loadActiveTicket() {
        runCatching { client.checkActiveTicket() }
            .onSuccess { activeId ->
                if (activeId != null) {
                    conversationId = activeId
                    runCatching { refresh() }
                    screenState = 2
                } else {
                    screenState = 1
                }
            }
            .onFailure {
                showError("Erro de conexão ao buscar solicitações.")
                screenState = 1
            }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    runCatching { client.checkActiveTicket() }
                        .onSuccess { activeId ->
                            if (activeId != null) {
                                conversationId = activeId
                                runCatching { refresh() }
                                screenState = 2
                            } else {
                                screenState = 1
                            }
                        }
                        .onFailure {
                            showError("Erro de conexão ao buscar solicitações.")
                            screenState = 1
                        }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        loadActiveTicket()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedProof = uri
        }
    }

    Scaffold(
        topBar = {
            if (screenState != 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(64.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) { 
                            Icon(Icons.Default.ArrowBack, "Voltar", tint = MaterialTheme.colorScheme.onSurface) 
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(
                                    colors = listOf(PurplePrimary, DeepPurplePrimary)
                                )),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (screenState == 2 && protocolo.isNotBlank()) "Solicitação #$protocolo" else "Suporte Premium", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (screenState == 2) statusLabel(status) else "Acompanhar suporte ou compra", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (screenState) {
                0 -> {
                    // Loading State
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PurplePrimary)
                    }
                }
                1 -> {
                    SupportTriageScreen(
                        client = client,
                        onClose = onBack,
                        onTicketCreated = { newTicketId ->
                            conversationId = newTicketId
                            scope.launch {
                                runCatching { refresh() }
                                screenState = 2
                            }
                        }
                    )
                }
                2 -> {
                    // Chat/Ticket View
                    Column(Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                scope.launch {
                                    isRefreshing = true
                                    runCatching { refresh() }
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                reverseLayout = false
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                "Ticket Aberto! Nossa equipe responderá o mais rápido possível.\nArraste para baixo para atualizar.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                            )
                                        }
                                    }
                                }
                                
                                items(messages, key = { "premium_msg_${it.id}" }) { message ->
                                    val isUser = message.sender == "USER"
                                    val time = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(message.createdAt))
                                    Box(Modifier.fillMaxWidth(), contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                                        Surface(
                                            color = if (isUser) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            shape = RoundedCornerShape(
                                                topStart = 20.dp,
                                                topEnd = 20.dp,
                                                bottomStart = if (isUser) 20.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 20.dp
                                            ),
                                            modifier = Modifier.widthIn(min = 80.dp, max = 290.dp).shadow(1.dp, RoundedCornerShape(20.dp))
                                        ) {
                                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                                if (!isUser) {
                                                    Text("Suporte", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = PurplePrimary, modifier = Modifier.padding(bottom = 4.dp))
                                                }
                                                message.body?.let { Text(it, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp) }
                                                message.attachmentUrl?.let {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Comprovante enviado", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                                Text(
                                                    time, 
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), 
                                                    color = (if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                                                    modifier = Modifier.align(Alignment.End).padding(top = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        selectedProof?.let {
                            Surface(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shadowElevation = 4.dp
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AsyncImage(model = it, contentDescription = "Prévia", modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("Comprovante selecionado", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                        TextButton(onClick = { selectedProof = null }) { Text("Cancelar") }
                                        Button(
                                            onClick = {
                                                sending = true
                                                scope.launch {
                                                    runCatching { client.uploadProof(conversationId!!, it) }
                                                        .onSuccess { selectedProof = null; refresh() }
                                                        .onFailure { failure -> showError(failure.message) }
                                                    sending = false
                                                }
                                            },
                                            enabled = !sending && conversationId != null,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(if (sending) "Enviando..." else "Enviar")
                                        }
                                    }
                                }
                            }
                        }

                        val isClosed = status == "concluido" || status == "fechado" || status == "cancelado"
                        if (isClosed) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    "Esta solicitação foi concluída.",
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        } else {
                            // Premium Chat Input
                            Surface(
                                Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 16.dp
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { picker.launch("image/*") },
                                        modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    ) {
                                        Icon(Icons.Default.AttachFile, "Comprovante", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    OutlinedTextField(
                                        value = text,
                                        onValueChange = { if (it.length <= 1500) text = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Mensagem...", style = MaterialTheme.typography.bodyLarge) },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        maxLines = 4
                                    )
                                    IconButton(
                                        onClick = {
                                            val body = text.trim()
                                            if (body.isEmpty() || conversationId == null) return@IconButton
                                            sending = true
                                            scope.launch {
                                                runCatching { client.sendMessage(conversationId!!, body) }
                                                    .onSuccess { text = ""; refresh() }
                                                    .onFailure { err -> showError(err.message) }
                                                sending = false
                                            }
                                        },
                                        enabled = !sending && text.isNotBlank(),
                                        modifier = Modifier.size(48.dp).background(if (!sending && text.isNotBlank()) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    ) {
                                        if (sending) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Send, "Enviar", tint = if (!sending && text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String) = when (status) {
    "aguardando_admin" -> "Aguardando análise"
    "aguardando_usuario" -> "Resposta recebida"
    "aguardando_pagamento" -> "Aguardando pagamento"
    "pagamento_confirmado" -> "Pagamento confirmado"
    "token_enviado" -> "Ação necessária"
    "concluido" -> "Solicitação concluída"
    "cancelado" -> "Cancelado"
    "fechado" -> "Fechado"
    else -> "Nova solicitação"
}

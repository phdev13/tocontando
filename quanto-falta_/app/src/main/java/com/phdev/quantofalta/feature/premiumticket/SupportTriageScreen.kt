package com.phdev.quantofalta.feature.premiumticket

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phdev.quantofalta.billing.EntitlementManager
import com.phdev.quantofalta.core.auth.AuthManager
import com.phdev.quantofalta.core.designsystem.theme.DeepPurplePrimary
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class TriageCategory(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

data class TriageQuestion(
    val id: String,
    val text: String,
    val type: String, // "options" or "text"
    val options: List<String> = emptyList()
)

data class TriageMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

val CATEGORIES = listOf(
    TriageCategory("Recuperar acesso", "Recuperar acesso", Icons.Default.LockOpen),
    TriageCategory("Premium e ativação", "Premium e ativação", Icons.Default.ElectricBolt),
    TriageCategory("Compra/token antecipado", "Compra/token antecipado", Icons.Default.ShoppingBag),
    TriageCategory("Compra pela Play Store", "Compra pela Play Store", Icons.Default.PlayCircle),
    TriageCategory("Instalação e atualização", "Instalação e atualização", Icons.Default.Download),
    TriageCategory("Problema no app", "Problema no app", Icons.Default.ErrorOutline),
    TriageCategory("Sincronização e dados", "Sincronização e dados", Icons.Default.Sync),
    TriageCategory("Sugestão de melhoria", "Sugestão de melhoria", Icons.Default.Lightbulb),
    TriageCategory("Outro assunto", "Outro assunto", Icons.Default.ChatBubbleOutline)
)

val QUESTIONS = mapOf(
    "Recuperar acesso" to listOf(
        TriageQuestion("q1", "Você ainda tem acesso ao email usado no app?", "options", listOf("Sim", "Não", "Não sei")),
        TriageQuestion("q2", "Você trocou de celular, formatou ou apagou os dados do app?", "options", listOf("Troquei de celular", "Formatei", "Apaguei os dados", "Nenhum")),
        TriageQuestion("q3", "Você já tinha ativado Premium antes?", "options", listOf("Sim", "Não", "Não lembro")),
        TriageQuestion("q4", "Você lembra o token/código usado?", "options", listOf("Sim, tenho ele", "Não", "Nunca usei token"))
    ),
    "Premium e ativação" to listOf(
        TriageQuestion("q1", "O Premium já funcionou antes?", "options", listOf("Sim", "Não")),
        TriageQuestion("q2", "O Premium sumiu após reinstalar ou trocar de aparelho?", "options", listOf("Sim", "Não")),
        TriageQuestion("q3", "Você ativou por token ou compra?", "options", listOf("Token", "Compra Play Store", "Compra Antecipada")),
        TriageQuestion("q4", "O app mostra Free mesmo após ativar?", "options", listOf("Sim", "Não")),
        TriageQuestion("q5", "O problema acontece no app, site ou ambos?", "options", listOf("App", "Site", "Ambos"))
    ),
    "Compra/token antecipado" to listOf(
        TriageQuestion("q1", "Você comprou o acesso antecipado?", "options", listOf("Sim", "Não")),
        TriageQuestion("q2", "Recebeu algum token/código?", "options", listOf("Sim", "Ainda não")),
        TriageQuestion("q3", "O código aparece como inválido, usado ou expirado?", "options", listOf("Inválido", "Usado", "Expirado", "Não testei"))
    ),
    "Compra pela Play Store" to listOf(
        TriageQuestion("q1", "A compra foi aprovada na Play Store?", "options", listOf("Sim", "Não", "Não sei")),
        TriageQuestion("q2", "O Premium foi liberado em algum momento?", "options", listOf("Sim", "Não")),
        TriageQuestion("q3", "Você reinstalou o app depois da compra?", "options", listOf("Sim", "Não")),
        TriageQuestion("q4", "Tentou restaurar a compra?", "options", listOf("Sim e funcionou", "Sim mas deu erro", "Não tentei")),
        TriageQuestion("q5", "O problema é compra aprovada sem ativação, restauração ou cobrança?", "options", listOf("Aprovada sem ativação", "Erro na restauração", "Cobrança indevida", "Outro"))
    ),
    "Instalação e atualização" to listOf(
        TriageQuestion("q1", "Você instalou pelo site/APK ou pela Play Store?", "options", listOf("Site/APK", "Play Store", "Outro")),
        TriageQuestion("q2", "O problema acontece ao instalar, atualizar ou abrir o app?", "options", listOf("Instalar", "Atualizar", "Abrir")),
        TriageQuestion("q4", "O aparelho mostra aviso de incompatibilidade?", "options", listOf("Sim", "Não"))
    ),
    "Problema no app" to listOf(
        TriageQuestion("q2", "O app fecha sozinho?", "options", listOf("Sim", "Não")),
        TriageQuestion("q3", "O contador está parado ou mostrando informação errada?", "options", listOf("Parado", "Informação errada", "Não é sobre o contador")),
        TriageQuestion("q4", "O erro começou depois de uma atualização?", "options", listOf("Sim", "Não", "Não sei"))
    ),
    "Sincronização e dados" to listOf(
        TriageQuestion("q1", "Você está usando o mesmo email/conta?", "options", listOf("Sim", "Não", "Não sei")),
        TriageQuestion("q2", "Os dados aparecem em outro aparelho?", "options", listOf("Sim", "Não", "Não testei")),
        TriageQuestion("q3", "O problema começou depois de reinstalar?", "options", listOf("Sim", "Não")),
        TriageQuestion("q4", "Algum dado sumiu?", "options", listOf("Sim", "Não"))
    )
)

fun calculatePriority(categoria: String): String {
    if (categoria in listOf("Recuperar acesso", "Problema no app", "Sincronização e dados")) return "alta"
    if (categoria == "Sugestão de melhoria") return "baixa"
    return "normal"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupportTriageScreen(
    client: PremiumTicketClient,
    onClose: () -> Unit,
    onTicketCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val auth = remember { AuthManager(context) }
    val entitlementManager = remember { EntitlementManager(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { 
        mutableStateOf(listOf(
            TriageMessage(text = "Olá! Vou fazer uma triagem rápida para entender o que aconteceu e enviar seu chamado com as informações certas.\nQual é o assunto principal?", isUser = false)
        )) 
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var currentQuestions by remember { mutableStateOf<List<TriageQuestion>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    
    val answers = remember { mutableStateMapOf<String, String>() }
    var textAnswer by remember { mutableStateOf("") }
    
    var step by remember { mutableIntStateOf(0) } // 0: Categoria, 1: Perguntas, 2: Final/Identificação, 3: Resumo, 4: Enviando
    
    var formNome by remember { mutableStateOf("") }
    var formEmail by remember { mutableStateOf(auth.getEmail() ?: "") }
    var additionalDetails by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun addSystemMessage(text: String) {
        messages = messages + TriageMessage(text = text, isUser = false)
    }

    fun addUserMessage(text: String) {
        messages = messages + TriageMessage(text = text, isUser = true)
    }

    fun proceedToNextQuestionOrFinish() {
        if (currentQuestionIndex < currentQuestions.size) {
            val q = currentQuestions[currentQuestionIndex]
            addSystemMessage(q.text)
        } else {
            step = 2
            addSystemMessage("Entendi. Vou organizar essas informações para o suporte. Para abrir o chamado, por favor, me informe seu nome, email e, se quiser, detalhe mais o problema:")
        }
    }

    BackHandler {
        if (step > 0) {
            // Em um chat real, voltar pode ser complexo. Aqui vamos permitir fechar direto ou reiniciar.
            onClose()
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(Modifier.fillMaxSize()) {
                // HEADER
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PurplePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Suporte Tô Contando", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "Assistente de Triagem", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                }

                // CHAT AREA
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { "triage_msg_${it.id}" }) { msg ->
                        ChatBubble(msg)
                    }
                    if (step == 4) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PurplePrimary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // OPTIONS AREA
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        when (step) {
                            0 -> {
                                // Select Category
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CATEGORIES.forEach { cat ->
                                        OutlinedButton(
                                            onClick = {
                                                selectedCategory = cat.id
                                                addUserMessage(cat.title)
                                                currentQuestions = QUESTIONS[cat.id] ?: emptyList()
                                                currentQuestionIndex = 0
                                                step = 1
                                                proceedToNextQuestionOrFinish()
                                            },
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                        ) {
                                            Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PurplePrimary)
                                            Spacer(Modifier.width(8.dp))
                                            Text(cat.title)
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Questions
                                val q = currentQuestions.getOrNull(currentQuestionIndex)
                                if (q != null && q.type == "options") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        q.options.forEach { opt ->
                                            Button(
                                                onClick = {
                                                    answers[q.text] = opt
                                                    addUserMessage(opt)
                                                    currentQuestionIndex++
                                                    proceedToNextQuestionOrFinish()
                                                },
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                                            ) {
                                                Text(opt)
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                answers[q.text] = "Não sei / Pular"
                                                addUserMessage("Não sei / Pular")
                                                currentQuestionIndex++
                                                proceedToNextQuestionOrFinish()
                                            },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text("Pular esta pergunta", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else if (q != null && q.type == "text") {
                                    Column {
                                        OutlinedTextField(
                                            value = textAnswer,
                                            onValueChange = { textAnswer = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = { Text("Sua resposta...") },
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            TextButton(onClick = {
                                                answers[q.text] = "Não informado"
                                                addUserMessage("Pular")
                                                textAnswer = ""
                                                currentQuestionIndex++
                                                proceedToNextQuestionOrFinish()
                                            }) {
                                                Text("Pular", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Button(
                                                onClick = {
                                                    val ans = textAnswer.ifBlank { "Não informado" }
                                                    answers[q.text] = ans
                                                    addUserMessage(ans)
                                                    textAnswer = ""
                                                    currentQuestionIndex++
                                                    proceedToNextQuestionOrFinish()
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                                            ) {
                                                Text("Enviar")
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Final Form
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = formNome,
                                        onValueChange = { formNome = it },
                                        label = { Text("Seu Nome") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = formEmail,
                                        onValueChange = { formEmail = it },
                                        label = { Text("Seu Email") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = additionalDetails,
                                        onValueChange = { additionalDetails = it },
                                        label = { Text("Descreva o problema com mais detalhes (opcional)") },
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        maxLines = 3
                                    )
                                    
                                    Button(
                                        onClick = {
                                            if (formNome.isBlank() || formEmail.isBlank() || !formEmail.contains("@")) {
                                                Toast.makeText(context, "Preencha nome e um email válido.", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            step = 4
                                            
                                            // Trigger API Call
                                            scope.launch {
                                                try {
                                                    val cat = selectedCategory ?: "Outro assunto"
                                                    val priority = calculatePriority(cat)
                                                    
                                                    var resumoStr = "Categoria: $cat\n\n"
                                                    answers.forEach { (q, a) -> resumoStr += "- $q: $a\n" }
                                                    
                                                    val resJson = JSONObject()
                                                    answers.forEach { (q, a) -> resJson.put(q, a) }
                                                    
                                                    // Gather device metadata
                                                    val installSource = try {
                                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                                            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
                                                        } else {
                                                            @Suppress("DEPRECATION")
                                                            context.packageManager.getInstallerPackageName(context.packageName)
                                                        } ?: "desconhecida"
                                                    } catch(e: Exception) { "desconhecida" }
                                                    val isPremiumLocal = runCatching { entitlementManager.hasActivePremium.first() }.getOrDefault(false)

                                                    val ticketId = client.createTicket(
                                                        nome = formNome,
                                                        email = formEmail,
                                                        mensagemInicial = additionalDetails,
                                                        categoria = cat,
                                                        prioridadeSugerida = priority,
                                                        resumoTriagem = resumoStr.take(2000),
                                                        respostasTriagem = resJson.toString(),
                                                        origemInstalacao = installSource,
                                                        versaoAndroid = android.os.Build.VERSION.RELEASE,
                                                        modeloAparelho = android.os.Build.MODEL,
                                                        isPremiumLocal = isPremiumLocal
                                                    )
                                                    
                                                    Toast.makeText(context, "Chamado enviado com sucesso!", Toast.LENGTH_SHORT).show()
                                                    onTicketCreated(ticketId)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Erro ao enviar: ${e.message}", Toast.LENGTH_LONG).show()
                                                    step = 2 // Voltar ao form
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).padding(top = 8.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                                    ) {
                                        Text("Abrir Chamado", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                            4 -> {
                                Text(
                                    "Enviando suas informações para o suporte...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        if (step < 2) {
                            Text(
                                "Essa triagem serve apenas para organizar as informações antes do atendimento real.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: TriageMessage) {
    val time = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(message.timestamp))
    Box(Modifier.fillMaxWidth(), contentAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (message.isUser) PurplePrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            contentColor = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(min = 60.dp, max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                if (!message.isUser) {
                    Text("Tô Contando", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PurplePrimary, modifier = Modifier.padding(bottom = 2.dp))
                }
                Text(message.text, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                Text(
                    time, 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                    color = (if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

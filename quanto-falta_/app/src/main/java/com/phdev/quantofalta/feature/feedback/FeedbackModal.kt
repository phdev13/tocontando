package com.phdev.quantofalta.feature.feedback

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.pm.PackageInfoCompat
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.designsystem.theme.PurplePrimary
import com.phdev.quantofalta.core.feedback.FeedbackData
import com.phdev.quantofalta.core.utils.SupportEmailUtils
import kotlinx.coroutines.delay

private val CATEGORIES = listOf(
    "suggestion" to "Sugestão",
    "bug" to "Problema",
    "compliment" to "Elogio",
    "question" to "Dúvida",
    "other" to "Outro",
)

enum class FeedbackSubmitResult {
    SUCCESS,
    QUEUED,
    ERROR
}

/**
 * Modal Dialog for Feedback following Tô Contando design.
 * Uses a real centralized Dialog instead of a BottomSheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackModal(
    onDismiss: () -> Unit,
    onSubmit: (FeedbackData) -> Unit,
    isSubmitting: Boolean = false,
    submitResult: FeedbackSubmitResult? = null,
    sourceScreen: String = "unknown",
) {
    val context = LocalContext.current
    var rating by remember { mutableStateOf(0) }
    var category by remember { mutableStateOf("suggestion") }
    var message by remember { mutableStateOf("") }
    var includeTechData by remember { mutableStateOf(false) }
    
    // Auto-dismiss on success after a short delay
    LaunchedEffect(submitResult) {
        if (submitResult == FeedbackSubmitResult.SUCCESS || submitResult == FeedbackSubmitResult.QUEUED) {
            delay(2000)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            // Se já digitou algo importante, ignorar toque fora (obriga a usar o X para cancelar ou terminar envio)
            if (message.isBlank() && !isSubmitting) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Cabeçalho (Fixo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 24.dp, end = 20.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Deixe seu Feedback",
                                style = AppTypography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                ),
                                color = PurplePrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Sua opinião ajuda a melhorar o aplicativo.",
                                style = AppTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp),
                            enabled = !isSubmitting
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                "Fechar",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Conteúdo (Rolável se o teclado empurrar ou a tela for pequena)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // 2. Avaliação
                        RatingBar(rating = rating, onRatingChanged = { rating = it })

                        // 3. Categorias (Chips)
                        CategorySelector(category = category, onCategoryChanged = { category = it })

                        // 4. Mensagem
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Mensagem",
                                style = AppTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = message,
                                onValueChange = { if (it.length <= 2000) message = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp),
                                placeholder = { 
                                    Text(
                                        "Conte como foi sua experiência, sugira melhorias ou relate problemas encontrados...", 
                                        color = MaterialTheme.colorScheme.outline
                                    ) 
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PurplePrimary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                maxLines = 8,
                            )
                            Text(
                                "${message.length}/2000",
                                style = AppTypography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { includeTechData = !includeTechData },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = includeTechData,
                                onCheckedChange = { includeTechData = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Incluir dados técnicos",
                                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Versão do app, Android e modelo do aparelho.",
                                    style = AppTypography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // 5. Rodapé (Fixo na parte inferior)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        AnimatedVisibility(
                            visible = submitResult != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val (text, color, icon) = when (submitResult) {
                                FeedbackSubmitResult.SUCCESS -> Triple("Obrigado pelo seu feedback!", Color(0xFF34C759), "✓")
                                FeedbackSubmitResult.QUEUED -> Triple("Salvo! Será enviado assim que houver internet.", Color(0xFFFF9F0A), "📶")
                                FeedbackSubmitResult.ERROR -> Triple("Erro ao enviar. Tente novamente.", Color(0xFFFF453A), "❌")
                                null -> Triple("", Color.Transparent, "")
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$icon  $text",
                                    style = AppTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = color
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (message.isBlank() || isSubmitting) return@Button
                                val versionCode = try {
                                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                                    PackageInfoCompat.getLongVersionCode(pInfo).toInt()
                                } catch (_: Exception) { null }
                                onSubmit(FeedbackData(
                                    rating = if (rating > 0) rating else null,
                                    category = category,
                                    message = message.trim(),
                                    includeTechData = includeTechData,
                                    sourceScreen = sourceScreen,
                                    versionCode = versionCode,
                                    androidVersion = Build.VERSION.RELEASE,
                                    model = "${Build.MANUFACTURER} ${Build.MODEL}",
                                    language = java.util.Locale.getDefault().language,
                                ))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = message.isNotBlank() && !isSubmitting && submitResult != FeedbackSubmitResult.SUCCESS,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.outline
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Text(
                                    "Enviar feedback",
                                    style = AppTypography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Prefere falar por e-mail? Entre em contato.",
                            style = AppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = PurplePrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SupportEmailUtils.openSupportEmail(context)
                                }
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingBar(rating: Int, onRatingChanged: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Avaliação",
            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..5) {
                val filled = i <= rating
                val starColor by animateColorAsState(
                    targetValue = if (filled) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                    animationSpec = tween(300),
                    label = "starColor$i"
                )
                val tileColor by animateColorAsState(
                    targetValue = if (filled) Color(0xFFFFB300).copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    animationSpec = tween(300),
                    label = "starTile$i"
                )
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "starScale$i"
                )
                
                val interactionSource = remember { MutableInteractionSource() }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tileColor)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onRatingChanged(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$i estrela(s)",
                        tint = starColor,
                        modifier = Modifier
                            .size(34.dp)
                            .scale(scale)
                            .graphicsLayer {
                                alpha = if (filled) 1f else 0.9f
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(category: String, onCategoryChanged: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "O que você deseja relatar?",
            style = AppTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CATEGORIES.forEach { (key, label) ->
                val selected = category == key
                val bgColor by animateColorAsState(
                    if (selected) PurplePrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    label = "chipBg"
                )
                val borderColor by animateColorAsState(
                    if (selected) PurplePrimary else Color.Transparent,
                    label = "chipBorder"
                )
                val textColor by animateColorAsState(
                    if (selected) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "chipText"
                )
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onCategoryChanged(key) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = AppTypography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                        color = textColor
                    )
                }
            }
        }
    }
}

package com.phdev.quantofalta.feature.eventdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phdev.quantofalta.BuildConfig
import com.phdev.quantofalta.core.designsystem.theme.AppShapes
import com.phdev.quantofalta.core.designsystem.theme.AppSpacing
import com.phdev.quantofalta.core.designsystem.theme.AppTypography
import com.phdev.quantofalta.core.utils.ImageCaptureUtils
import com.phdev.quantofalta.domain.model.EventUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    event: EventUiModel,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.extraLarge)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Link Web", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.Link, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Imagem", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.Image, contentDescription = null) }
                )
            }
            
            Spacer(modifier = Modifier.height(AppSpacing.medium))
            
            if (selectedTabIndex == 0) {
                ShareLinkContent(event = event, onDismiss = onDismiss)
            } else {
                ShareImageContent(
                    event = event,
                    isPremium = isPremium,
                    onDismiss = onDismiss,
                    onNavigateToPremium = onNavigateToPremium
                )
            }
        }
    }
}

@Composable
fun ShareLinkContent(
    event: EventUiModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var generatedUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(horizontal = AppSpacing.large)) {
        Spacer(modifier = Modifier.height(AppSpacing.medium))
        Text(
            text = "Compartilhe um link dinâmico",
            style = AppTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(AppSpacing.medium))
        Text(
            text = "Gere um link do seu evento. Seus amigos veem a contagem ao vivo no navegador, sem precisar baixar o app.",
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

        if (generatedUrl == null) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.labelMedium,
                    modifier = Modifier.padding(bottom = AppSpacing.medium)
                )
            }

            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = generateShareLink(event)
                            if (result != null) {
                                generatedUrl = result
                            } else {
                                errorMessage = "Erro ao gerar link. Verifique sua internet."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Erro: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading,
                shape = AppShapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Gerar Link", style = AppTypography.labelLarge)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppShapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(AppSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = generatedUrl!!,
                    style = AppTypography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Tô Contando Link", generatedUrl)
                    clipboard.setPrimaryClip(clip)
                }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copiar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))

            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Olha quanto falta para '${event.title}': $generatedUrl")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar via"))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = AppShapes.medium
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartilhar Link", style = AppTypography.labelLarge)
            }
        }
    }
}

@Composable
fun ShareImageContent(
    event: EventUiModel,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var hideExactDate by remember { mutableStateOf(false) }
    var hideWatermark by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf(ShareAspectRatio.STORY) }

    Column(modifier = Modifier.padding(horizontal = AppSpacing.large)) {
        Spacer(modifier = Modifier.height(AppSpacing.small))
        
        // Preview Container (Scaled Down)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(AppShapes.medium)
                .background(Color.Black.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(selectedFormat.width.toFloat() / selectedFormat.height.toFloat())
                    .fillMaxHeight(0.9f)
                    .clip(AppShapes.medium)
            ) {
                ShareImageCard(
                    event = event,
                    hideExactDate = hideExactDate,
                    hideWatermark = hideWatermark,
                    aspectRatio = selectedFormat
                )
            }
        }

        Spacer(modifier = Modifier.height(AppSpacing.large))

        Text("Formato", style = AppTypography.labelLarge)
        Spacer(modifier = Modifier.height(AppSpacing.small))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatChip(
                label = "Story (9:16)", 
                selected = selectedFormat == ShareAspectRatio.STORY,
                onClick = { selectedFormat = ShareAspectRatio.STORY }
            )
            FormatChip(
                label = "Post (1:1)", 
                selected = selectedFormat == ShareAspectRatio.SQUARE,
                onClick = { selectedFormat = ShareAspectRatio.SQUARE }
            )
            FormatChip(
                label = "Wallpaper", 
                selected = selectedFormat == ShareAspectRatio.WALLPAPER,
                onClick = { selectedFormat = ShareAspectRatio.WALLPAPER }
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.mediumLarge))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ocultar data exata", style = AppTypography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = hideExactDate,
                onCheckedChange = { hideExactDate = it }
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.medium))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Remover marca d'água", style = AppTypography.bodyMedium)
                if (!isPremium) {
                    Text("⭐ Premium", color = Color(0xFFD4AF37), style = AppTypography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Switch(
                checked = hideWatermark,
                onCheckedChange = { 
                    if (isPremium) {
                        hideWatermark = it 
                    } else {
                        onDismiss()
                        onNavigateToPremium()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(AppSpacing.extraLarge))

        Button(
            onClick = {
                isGenerating = true
                coroutineScope.launch {
                    try {
                        val bitmap = ImageCaptureUtils.captureComposableToBitmap(
                            context = context,
                            width = selectedFormat.width,
                            height = selectedFormat.height,
                            content = {
                                ShareImageCard(
                                    event = event,
                                    hideExactDate = hideExactDate,
                                    hideWatermark = hideWatermark,
                                    aspectRatio = selectedFormat
                                )
                            }
                        )
                        
                        val uri = ImageCaptureUtils.saveBitmapToCacheAndGetUri(context, bitmap)
                        if (uri != null) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_TEXT, "Gerado com o app Tô Contando ⏳")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Imagem"))
                            onDismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isGenerating = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isGenerating,
            shape = AppShapes.medium
        ) {
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gerar Imagem de Alta Qualidade", style = AppTypography.labelLarge)
            }
        }
    }
}

@Composable
fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = AppShapes.pill,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = AppTypography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

suspend fun generateShareLink(event: EventUiModel): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("${BuildConfig.API_BASE_URL}/api/v1/app/share/events")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 8000
        connection.readTimeout = 8000

        val colorHex = String.format("#%06X", (0xFFFFFF and event.color.toArgb()))

        val json = JSONObject().apply {
            put("title", event.title)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            put("date", sdf.format(java.util.Date(event.dateMillis)))
            put("color", colorHex)
            put("icon", event.iconName)
        }

        connection.outputStream.use { os ->
            val input = json.toString().toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val responseObj = root.optJSONObject("data") ?: root
            val slug = responseObj.getString("slug")
            // Utilizando SHARE_BASE_URL (Ex: https://share.tocontando.com.br)
            return@withContext "${BuildConfig.SHARE_BASE_URL}/s/$slug"
        }
    } catch (e: Exception) {
        Log.e("ShareBottomSheet", "Failed to load/share link", e)
    }
    return@withContext null
}

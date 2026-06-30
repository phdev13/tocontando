package com.phdev.quantofalta.feature.eventdetails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import coil.imageLoader
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    event: EventUiModel,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    
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
                    onClick = {
                        if (isPremium) {
                            selectedTabIndex = 1
                        } else {
                            onNavigateToPremium()
                        }
                    },
                    text = { Text("Imagem", fontWeight = FontWeight.Bold) },
                    icon = {
                        if (isPremium) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                        } else {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.Lock,
                                contentDescription = "Recurso Premium",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
    var generatedUrl by rememberSaveable(event.id) { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .padding(horizontal = AppSpacing.large)
            .verticalScroll(rememberScrollState())
    ) {
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
        Spacer(modifier = Modifier.height(AppSpacing.medium))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        ) {
            Row(
                modifier = Modifier.padding(AppSpacing.medium),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Column {
                    Text(
                        "O link expira em 7 dias",
                        style = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "Após esse prazo, o link e a foto enviada serão apagados permanentemente.",
                        style = AppTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
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
                            when (val result = generateShareLink(context, event)) {
                                is ShareLinkResult.Success -> generatedUrl = result.url
                                is ShareLinkResult.Error -> errorMessage = result.message
                            }
                        } catch (e: Exception) {
                            errorMessage = "Não foi possível gerar o link. Tente novamente."
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
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(generatedUrl)))
                    }
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
    var generationError by remember { mutableStateOf<String?>(null) }
    var hideExactDate by rememberSaveable(event.id) { mutableStateOf(false) }
    var hideWatermark by rememberSaveable(event.id) { mutableStateOf(false) }
    var selectedFormatName by rememberSaveable(event.id) {
        mutableStateOf(ShareAspectRatio.STORY.name)
    }
    val selectedFormat = ShareAspectRatio.valueOf(selectedFormatName)
    var activeShareTarget by remember { mutableStateOf<ShareTarget?>(null) }
    val isBusy = activeShareTarget != null

    fun shareImage(target: ShareTarget) {
        activeShareTarget = target
        generationError = null
        coroutineScope.launch {
            var generatedBitmap: android.graphics.Bitmap? = null
            try {
                event.coverImageUri?.let { cover ->
                    withContext(Dispatchers.IO) {
                        context.imageLoader.execute(
                            ImageRequest.Builder(context)
                                .data(cover)
                                .size(selectedFormat.width, selectedFormat.height)
                                .allowHardware(false)
                                .build()
                        )
                    }
                }
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
                generatedBitmap = bitmap

                val uri = ImageCaptureUtils.saveBitmapToCacheAndGetUri(context, bitmap)
                    ?: error("Nao foi possivel salvar a imagem gerada.")
                when (target) {
                    ShareTarget.INSTAGRAM -> shareToInstagram(context, uri, selectedFormat)
                    ShareTarget.GENERIC -> shareImageGeneric(context, uri)
                }
                onDismiss()
            } catch (e: Exception) {
                Log.e("ShareBottomSheet", "Falha ao gerar imagem", e)
                generationError = e.message ?: "Nao foi possivel gerar a imagem. Tente novamente."
            } finally {
                generatedBitmap?.takeIf { !it.isRecycled }?.recycle()
                activeShareTarget = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .navigationBarsPadding()
            .padding(horizontal = AppSpacing.large)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp, max = 330.dp)
                    .clip(AppShapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .aspectRatio(selectedFormat.width.toFloat() / selectedFormat.height.toFloat())
                        .clip(AppShapes.medium)
                ) {
                    ShareImageCard(event, hideExactDate, hideWatermark, selectedFormat)
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.large))
            Text("Formato", style = AppTypography.labelLarge)
            Spacer(modifier = Modifier.height(AppSpacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatChip("Story (9:16)", selectedFormat == ShareAspectRatio.STORY) { selectedFormatName = ShareAspectRatio.STORY.name }
                FormatChip("Post (1:1)", selectedFormat == ShareAspectRatio.SQUARE) { selectedFormatName = ShareAspectRatio.SQUARE.name }
                FormatChip("Wallpaper", selectedFormat == ShareAspectRatio.WALLPAPER) { selectedFormatName = ShareAspectRatio.WALLPAPER.name }
            }

            Spacer(modifier = Modifier.height(AppSpacing.medium))
            ShareOptionRow("Ocultar data exata", hideExactDate) { hideExactDate = it }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ShareOptionRow(
                title = "Remover marca d'água",
                checked = hideWatermark,
                supportingText = if (isPremium) null else "Recurso Premium",
            ) {
                if (isPremium) {
                    hideWatermark = it
                } else {
                    onDismiss()
                    onNavigateToPremium()
                }
            }
            Spacer(modifier = Modifier.height(AppSpacing.medium))
        }

        generationError?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = AppTypography.bodySmall,
                modifier = Modifier.padding(vertical = AppSpacing.small),
            )
        }

        Button(
            onClick = { shareImage(ShareTarget.INSTAGRAM) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isBusy,
            shape = AppShapes.medium
        ) {
            if (activeShareTarget == ShareTarget.INSTAGRAM) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enviar para Instagram", style = AppTypography.labelLarge)
            }
        }
        Spacer(Modifier.height(AppSpacing.small))
        OutlinedButton(
            onClick = { shareImage(ShareTarget.GENERIC) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isBusy,
            shape = AppShapes.medium
        ) {
            if (activeShareTarget == ShareTarget.GENERIC) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartilhar imagem", style = AppTypography.labelLarge)
            }
        }
        Spacer(Modifier.height(AppSpacing.small))
    }
}

private enum class ShareTarget {
    INSTAGRAM,
    GENERIC
}

private fun shareImageGeneric(context: Context, uri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Gerado com o app To Contando")
        clipData = ClipData.newUri(context.contentResolver, "Card To Contando", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(shareIntent, "Compartilhar imagem").apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooser)
}

private fun shareToInstagram(context: Context, uri: Uri, format: ShareAspectRatio) {
    val instagramPackage = "com.instagram.android"
    val storyIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setPackage(instagramPackage)
        type = "image/png"
        putExtra("source_application", context.packageName)
        putExtra("interactive_asset_uri", uri)
        putExtra("background_asset_uri", uri)
        clipData = ClipData.newUri(context.contentResolver, "Story To Contando", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val feedIntent = Intent(Intent.ACTION_SEND).apply {
        setPackage(instagramPackage)
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "Post To Contando", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        val preferredIntent = if (format == ShareAspectRatio.STORY || format == ShareAspectRatio.WALLPAPER) {
            storyIntent
        } else {
            feedIntent
        }
        context.startActivity(preferredIntent)
    }.recoverCatching {
        context.startActivity(feedIntent)
    }.onFailure {
        Toast.makeText(
            context,
            "Instagram nao encontrado. Abrindo opcoes de compartilhamento.",
            Toast.LENGTH_SHORT
        ).show()
        shareImageGeneric(context, uri)
    }
}

@Composable
private fun ShareOptionRow(
    title: String,
    checked: Boolean,
    supportingText: String? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = AppTypography.bodyMedium)
            supportingText?.let {
                Text(it, style = AppTypography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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

suspend fun uploadCoverImage(context: android.content.Context, uriString: String): String? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val uri = android.net.Uri.parse(uriString)
        val bitmap = if (uri.scheme == "file") {
            android.graphics.BitmapFactory.decodeFile(uri.path)
                ?: return@withContext null
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
            android.graphics.ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        // Scale down to max 1080 pixels
        val maxDimension = 1080
        val scale = Math.min(
            maxDimension.toFloat() / bitmap.width,
            maxDimension.toFloat() / bitmap.height
        )
        
        val scaledBitmap = if (scale < 1) {
            val matrix = android.graphics.Matrix()
            matrix.postScale(scale, scale)
            android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val stream = java.io.ByteArrayOutputStream()
        scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
        val byteArray = stream.toByteArray()
        if (scaledBitmap !== bitmap && !scaledBitmap.isRecycled) scaledBitmap.recycle()
        if (!bitmap.isRecycled) bitmap.recycle()

        val url = URL("${BuildConfig.API_BASE_URL}/api/v1/app/share/events/cover")
        val conn = (url.openConnection() as HttpURLConnection).also { connection = it }
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        
        val boundary = "Boundary-" + System.currentTimeMillis()
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        val imageHash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(byteArray)
            .joinToString("") { "%02x".format(it) }
        conn.setRequestProperty("X-Content-SHA256", imageHash)
        conn.doOutput = true
        
        conn.outputStream.use { os ->
            val writer = java.io.PrintWriter(java.io.OutputStreamWriter(os, Charsets.UTF_8), true)
            writer.append("--$boundary\r\n")
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"cover.jpg\"\r\n")
            writer.append("Content-Type: image/jpeg\r\n\r\n")
            writer.flush()
            os.write(byteArray)
            os.flush()
            writer.append("\r\n--$boundary--\r\n")
            writer.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode in 200..299) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val responseObj = root.optJSONObject("data") ?: root
            return@withContext responseObj.optString("cover_url", null)
        }
    } catch (e: Exception) {
        Log.e("ShareBottomSheet", "Failed to upload cover image", e)
    } finally {
        connection?.disconnect()
    }
    return@withContext null
}

sealed interface ShareLinkResult {
    data class Success(val url: String) : ShareLinkResult
    data class Error(val message: String) : ShareLinkResult
}

suspend fun generateShareLink(context: android.content.Context, event: EventUiModel): ShareLinkResult = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        var coverUrl: String? = null
        if (!event.coverImageUri.isNullOrEmpty()) {
            coverUrl = uploadCoverImage(context, event.coverImageUri)
        }

        val url = URL("${BuildConfig.API_BASE_URL}/api/v1/app/share/events")
        val conn = (url.openConnection() as HttpURLConnection).also { connection = it }
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        val colorHex = String.format("#%06X", (0xFFFFFF and event.color.toArgb()))

        val relationship = event.relationshipUiState
        val shareDateMillis = relationship?.let {
            java.time.LocalDate.ofEpochDay(it.startEpochDay)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } ?: event.dateMillis

        val json = JSONObject().apply {
            put("title", event.title)
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            put("date", sdf.format(java.util.Date(shareDateMillis)))
            put("color", colorHex)
            put("icon", event.iconName)
            put("card_type", if (relationship != null) "relationship" else "event")
            if (relationship != null) {
                put("relationship_type", relationship.relationshipType)
                put("primary_text", relationship.primaryText)
                put("secondary_text", relationship.secondaryText)
                put(
                    "relationship_start_date",
                    java.time.LocalDate.ofEpochDay(relationship.startEpochDay).toString()
                )
            }
            if (coverUrl != null) {
                put("cover_url", coverUrl)
            }
        }

        conn.outputStream.use { os ->
            val input = json.toString().toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }

        val responseCode = conn.responseCode

        if (responseCode in 200..299) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val responseObj = root.optJSONObject("data") ?: root
            val slug = responseObj.getString("slug")
            // Utilizando SHARE_BASE_URL (Ex: https://share.tocontando.com.br)
            return@withContext ShareLinkResult.Success("${BuildConfig.SHARE_BASE_URL}/share?s=$slug")
        }
        val errorBody = (conn.errorStream ?: runCatching { conn.inputStream }.getOrNull())
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        val serverMessage = runCatching {
            val root = JSONObject(errorBody)
            root.optJSONObject("error")?.optString("message")
                ?: root.optString("message")
                ?: root.optString("error")
        }.getOrNull()?.takeIf { it.isNotBlank() }
        return@withContext ShareLinkResult.Error(
            serverMessage ?: "O servidor recusou o link (código $responseCode)."
        )
    } catch (e: Exception) {
        Log.e("ShareBottomSheet", "Failed to load/share link", e)
        return@withContext ShareLinkResult.Error(
            when (e) {
                is java.net.SocketTimeoutException -> "O servidor demorou para responder. Tente novamente."
                is java.net.UnknownHostException -> "Sem conexão com a internet."
                else -> e.message ?: "Não foi possível gerar o link."
            }
        )
    } finally {
        connection?.disconnect()
    }
}
